package com.example.kivo.data.notifications

import android.content.Context
import com.example.kivo.data.models.Message
import com.example.kivo.data.models.Song

object NotificationCenter {

    fun init(context: Context) {
        NotificationStore.init(context)
        NotificationSettingsStore.init(context)
        KivoNotificationManager.ensureChannels(context)
        val meta = context.getSharedPreferences("kivo_meta", Context.MODE_PRIVATE)
        if (!meta.getBoolean("welcome_notification_shown", false)) {
            meta.edit().putBoolean("welcome_notification_shown", true).apply()
            welcome()
        }
    }

    fun welcome() {
        emit(
            build(
                id = "welcome_${System.currentTimeMillis()}",
                category = NotificationCategory.SYSTEM,
                type = NotificationTypes.WELCOME,
                title = "¡Bienvenido a KIVO!",
                message = "Descubre tus nuevas canciones recomendadas cada día.",
                destination = NotificationDestination.MUSIC
            )
        )
    }

    fun recommendation(song: Song) {
        if (!enabled(NotificationCategory.MUSIC)) return
        emit(
            build(
                id = "rec_${song.id}",
                category = NotificationCategory.MUSIC,
                type = NotificationTypes.RECOMMENDATION,
                title = "Te recomendamos",
                message = "\"${song.title}\" de ${song.artist} está esperando tu play.",
                destination = NotificationDestination.PLAYER,
                destinationId = song.id,
                priority = NotificationPriority.DEFAULT
            )
        )
    }

    fun favoriteAdded(song: Song) {
        if (!enabled(NotificationCategory.MUSIC)) return
        emit(
            build(
                id = "fav_${song.id}",
                category = NotificationCategory.MUSIC,
                type = NotificationTypes.FAVORITE_ADDED,
                title = "Favorito guardado",
                message = "\"${song.title}\" se agregó a tus favoritos.",
                destination = NotificationDestination.PLAYER,
                destinationId = song.id,
                priority = NotificationPriority.LOW
            )
        )
    }

    fun songFinished(song: Song) {
        if (!enabled(NotificationCategory.MUSIC)) return
        emit(
            build(
                id = "ended_${song.id}",
                category = NotificationCategory.MUSIC,
                type = NotificationTypes.SONG_DOWNLOADED,
                title = "Canción terminada",
                message = "Reproducimos \"${song.title}\" de ${song.artist}.",
                destination = NotificationDestination.PLAYER,
                destinationId = song.id,
                priority = NotificationPriority.DEFAULT
            )
        )
    }

    fun newRecord(score: Int, difficultyLabel: String) {
        if (!enabled(NotificationCategory.GAMES)) return
        emit(
            build(
                id = "record_${System.currentTimeMillis()}_$score",
                category = NotificationCategory.GAMES,
                type = NotificationTypes.NEW_RECORD,
                title = "Nuevo récord",
                message = "¡Impresionante! Lograste $score puntos en $difficultyLabel.",
                destination = NotificationDestination.GAME,
                destinationId = "",
                priority = NotificationPriority.HIGH
            )
        )
    }

    fun chatMessage(message: Message) {
        if (!enabled(NotificationCategory.SOCIAL)) return
        if (message.conversationId.isNullOrBlank()) return
        val senderName = message.senderName.ifBlank { "Nuevo mensaje" }
        val body = if (message.type == "image") "📷 Foto" else message.text.ifBlank { "Mensaje" }
        emit(
            build(
                id = "msg_${message.conversationId}_${message.messageId.take(6)}_${message.createdAt}",
                category = NotificationCategory.SOCIAL,
                type = NotificationTypes.CHAT_MESSAGE,
                title = senderName,
                message = body,
                destination = NotificationDestination.CHAT,
                destinationId = "${message.conversationId}|${message.senderId}",
                priority = NotificationPriority.HIGH
            )
        )
    }

    fun importanceChange(title: String, message: String) {
        if (!enabled(NotificationCategory.SYSTEM)) return
        emit(
            build(
                id = "sys_${System.currentTimeMillis()}",
                category = NotificationCategory.SYSTEM,
                type = NotificationTypes.IMPORTANT_CHANGE,
                title = title,
                message = message,
                destination = NotificationDestination.NOTIFICATIONS,
                priority = NotificationPriority.HIGH
            )
        )
    }

    fun emit(notification: KivoNotification) {
        if (!NotificationStore.isInitialized()) return
        NotificationStore.add(notification)
        KivoNotificationManager.show(NotificationStore.context(), notification)
    }

    private fun enabled(category: NotificationCategory): Boolean =
        NotificationSettingsStore.isCategoryEnabled(category)

    private fun build(
        id: String,
        category: NotificationCategory,
        type: String,
        title: String,
        message: String,
        destination: NotificationDestination,
        destinationId: String = "",
        priority: NotificationPriority = NotificationPriority.DEFAULT
    ) = KivoNotification(
        id = id,
        category = category,
        type = type,
        title = title,
        message = message,
        timestamp = System.currentTimeMillis(),
        isRead = false,
        destination = destination,
        destinationId = destinationId,
        priority = priority
    )
}