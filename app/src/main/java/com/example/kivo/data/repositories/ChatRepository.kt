package com.example.kivo.data.repositories

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.kivo.data.models.Conversation
import com.example.kivo.data.models.Message
import com.example.kivo.data.models.User
import com.example.kivo.data.remote.ApiClient
import com.example.kivo.data.remote.CreateConversationRequest
import com.example.kivo.data.remote.PresenceRequest
import com.example.kivo.data.remote.SendMessageRequest
import com.example.kivo.data.remote.SocketManager
import com.example.kivo.data.remote.UploadImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

object ChatRepository {

    // Usuarios demo para que sean buscables incluso sin conexion
    private val demoUsers = listOf(
        User("uid_daniela", "daniela", "daniela", "Daniela", "daniela@kivo.com", bio = "Amo la música"),
        User("uid_rose", "rose", "rose", "Rose", "rose@kivo.com", bio = "Kivo es genial"),
        User("uid_reinaldo", "reinaldo", "reinaldo", "Reinaldo", "reinaldo@kivo.com", bio = "Explorador Kivo"),
        User("uid_fernando", "fernando", "fernando", "Fernando", "fernando@kivo.com", bio = "Creador de Kivo"),
        User("uid_hernesto", "hernesto", "hernesto", "Hernesto", "hernesto@kivo.com", bio = "Melómano")
    )

    suspend fun searchUsers(query: String): List<User> = withContext(Dispatchers.IO) {
        val queryLowercase = query.lowercase().removePrefix("@").trim()
        val matchedDemos = demoUsers.filter {
            it.usernameLowercase.contains(queryLowercase) || it.displayName.lowercase().contains(queryLowercase)
        }
        val remote = runCatching { ApiClient.service.searchUsers(queryLowercase) }.getOrDefault(emptyList())
        (matchedDemos + remote).distinctBy { it.userId }
    }

    suspend fun getOrCreateConversation(myId: String, otherUserId: String): String = withContext(Dispatchers.IO) {
        ApiClient.service.createConversation(CreateConversationRequest(otherUserId)).conversationId
    }

    suspend fun sendMessage(conversationId: String, message: Message) = withContext(Dispatchers.IO) {
        val sent = ApiClient.service.sendMessage(
            conversationId,
            SendMessageRequest(
                text = message.text,
                receiverId = message.receiverId,
                type = message.type
            )
        )
        // Echo local para que el remitente vea su mensaje de inmediato
        SocketManager.emitLocalMessage(sent)
    }

    suspend fun uploadImage(mimeType: String, bytes: ByteArray): String = withContext(Dispatchers.IO) {
        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        val response = ApiClient.service.uploadImage(
            UploadImageRequest(base64 = "data:$mimeType;base64,$base64")
        )
        response.url
    }

    suspend fun downloadImageBytes(url: String): ByteArray = withContext(Dispatchers.IO) {
        val resolved = ApiClient.resolveUrl(url) ?: url
        val client = okhttp3.OkHttpClient()
        val request = okhttp3.Request.Builder().url(resolved).build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            resp.body?.bytes() ?: throw IOException("Sin contenido")
        }
    }

    fun saveImageToGallery(context: Context, bytes: ByteArray, url: String): Boolean {
        val ext = url.substringAfterLast('.', "jpg").take(5).lowercase()
        val mime = when (ext) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            else -> "image/jpeg"
        }
        val name = "KIVO_${System.currentTimeMillis()}.${if (ext == "jpg" || ext == "jpeg") "jpg" else ext}"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                    put(MediaStore.Images.Media.MIME_TYPE, mime)
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/KIVO")
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                ) ?: return false
                context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return false
                true
            } else {
                @Suppress("DEPRECATION")
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "KIVO")
                dir.mkdirs()
                @Suppress("DEPRECATION")
                val file = File(dir, name)
                file.writeBytes(bytes)
                context.sendBroadcast(
                    Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file))
                )
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    fun getMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        // Al entrar al chat nos unimos a la sala del servidor
        SocketManager.joinConversation(conversationId)

        val initial = runCatching { ApiClient.service.getMessages(conversationId) }.getOrDefault(emptyList())
        var acc = initial.sortedBy { it.createdAt }
        trySend(acc.toList())

        fun merge(message: Message) {
            if (acc.none { it.messageId == message.messageId }) {
                acc = (acc + message).sortedBy { it.createdAt }
                trySend(acc.toList())
            }
        }

        val remoteJob = launch {
            SocketManager.newMessages
                .filter { it.conversationId == conversationId }
                .collect { merge(it) }
        }
        val localJob = launch {
            SocketManager.localMessages
                .filter { it.conversationId == conversationId }
                .collect { merge(it) }
        }
        val readJob = launch {
            SocketManager.messagesRead
                .filter { it.first == conversationId }
                .collect { (_, readerId) ->
                    acc = acc.map { m ->
                        if (m.receiverId == readerId) m.copy(status = "read", isRead = true) else m
                    }
                    trySend(acc.toList())
                }
        }

        awaitClose {
            remoteJob.cancel()
            localJob.cancel()
            readJob.cancel()
            SocketManager.leaveConversation(conversationId)
        }
    }

    fun getConversations(userId: String): Flow<List<Conversation>> = callbackFlow {
        suspend fun refresh() {
            val list = runCatching { ApiClient.service.getConversations() }
                .getOrDefault(emptyList())
                .sortedByDescending { it.updatedAt }
            trySend(list)
        }

        val initialJob = launch { refresh() }

        val updateJob = launch {
            SocketManager.conversationUpdated.collect { refresh() }
        }
        val messageJob = launch {
            SocketManager.newMessages.collect { refresh() }
        }

        awaitClose {
            initialJob.cancel()
            updateJob.cancel()
            messageJob.cancel()
        }
    }

    suspend fun markAsRead(conversationId: String, myUserId: String) = withContext(Dispatchers.IO) {
        runCatching { ApiClient.service.markAsRead(conversationId) }
    }

    suspend fun clearMessages(conversationId: String): Boolean = withContext(Dispatchers.IO) {
        runCatching { ApiClient.service.clearMessages(conversationId) }.isSuccess
    }

    suspend fun deleteConversation(conversationId: String): Boolean = withContext(Dispatchers.IO) {
        runCatching { ApiClient.service.deleteConversation(conversationId) }.isSuccess
    }

    suspend fun setUserPresence(userId: String, isOnline: Boolean) = withContext(Dispatchers.IO) {
        runCatching { ApiClient.service.setPresence(userId, PresenceRequest(isOnline)) }
    }
}