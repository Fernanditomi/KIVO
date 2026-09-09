package com.example.kivo.data.remote

import com.example.kivo.data.models.Message
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

object SocketManager {
    private val gson = Gson()
    private var socket: Socket? = null

    private val _newMessages = MutableSharedFlow<Message>(extraBufferCapacity = 64)
    val newMessages: SharedFlow<Message> = _newMessages

    private val _localMessages = MutableSharedFlow<Message>(extraBufferCapacity = 32)
    val localMessages: SharedFlow<Message> = _localMessages

    private val _conversationUpdated = MutableSharedFlow<Unit>(extraBufferCapacity = 64)
    val conversationUpdated: SharedFlow<Unit> = _conversationUpdated

    private val _messagesRead = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 16)
    val messagesRead: SharedFlow<Pair<String, String>> = _messagesRead

    private val _presence = MutableSharedFlow<Pair<String, Boolean>>(extraBufferCapacity = 16)
    val presence: SharedFlow<Pair<String, Boolean>> = _presence

    private val _connected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _connected

    fun connect(token: String) {
        if (socket?.connected() == true) return
        socket?.disconnect()
        socket?.close()
        socket = null
        _connected.value = false

        val options = IO.Options.builder()
            .setAuth(mapOf("token" to token))
            .setReconnection(true)
            .setTransports(arrayOf("websocket", "polling"))
            .build()

        val newSocket = IO.socket(NetworkMonitor.preferredBase(), options)
        socket = newSocket

        newSocket.on(Socket.EVENT_CONNECT) {
            _connected.value = true
        }
        newSocket.on(Socket.EVENT_DISCONNECT) {
            _connected.value = false
        }
        newSocket.on("new_message") { args ->
            val obj = args.firstOrNull() as? JSONObject ?: return@on
            val msg = runCatching { gson.fromJson(obj.toString(), Message::class.java) }.getOrNull()
            if (msg != null) _newMessages.tryEmit(msg)
        }
        newSocket.on("conversation_updated") {
            _conversationUpdated.tryEmit(Unit)
        }
        newSocket.on("messages_read") { args ->
            val obj = args.firstOrNull() as? JSONObject ?: return@on
            val conversationId = obj.optString("conversationId")
            val readerId = obj.optString("readerId")
            if (conversationId.isNotEmpty() && readerId.isNotEmpty()) {
                _messagesRead.tryEmit(conversationId to readerId)
            }
        }
        newSocket.on("presence") { args ->
            val obj = args.firstOrNull() as? JSONObject ?: return@on
            val userId = obj.optString("userId")
            val isOnline = obj.optBoolean("isOnline")
            if (userId.isNotEmpty()) _presence.tryEmit(userId to isOnline)
        }
        newSocket.connect()
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.close()
        socket = null
        _connected.value = false
    }

    fun joinConversation(conversationId: String) {
        socket?.emit("conversation:join", conversationId)
    }

    fun leaveConversation(conversationId: String) {
        socket?.emit("conversation:leave", conversationId)
    }

    fun emitLocalMessage(message: Message) {
        _localMessages.tryEmit(message)
    }

    fun emitCallOffer(callId: String, callerId: String, callerName: String, receiverId: String, type: CallType) {
        val data = JSONObject().apply {
            put("callId", callId)
            put("callerId", callerId)
            put("callerName", callerName)
            put("receiverId", receiverId)
            put("type", type.name.lowercase())
        }
        socket?.emit("call:offer", data)
    }

    fun emitCallAnswer(callId: String, callerId: String, receiverId: String) {
        val data = JSONObject().apply {
            put("callId", callId)
            put("callerId", callerId)
            put("receiverId", receiverId)
        }
        socket?.emit("call:answer", data)
    }

    fun emitCallReject(callId: String, callerId: String) {
        val data = JSONObject().apply {
            put("callId", callId)
            put("callerId", callerId)
        }
        socket?.emit("call:reject", data)
    }

    fun emitCallEnd(callId: String, callerId: String) {
        val data = JSONObject().apply {
            put("callId", callId)
            put("callerId", callerId)
        }
        socket?.emit("call:end", data)
    }

    fun emitIceCandidate(callId: String, targetId: String, candidate: String) {
        val data = JSONObject().apply {
            put("callId", callId)
            put("targetId", targetId)
            put("candidate", candidate)
        }
        socket?.emit("call:ice-candidate", data)
    }

    fun emitSdpOffer(callId: String, targetId: String, sdp: String) {
        val data = JSONObject().apply {
            put("callId", callId)
            put("targetId", targetId)
            put("sdp", sdp)
        }
        socket?.emit("call:sdp-offer", data)
    }

    fun emitSdpAnswer(callId: String, targetId: String, sdp: String) {
        val data = JSONObject().apply {
            put("callId", callId)
            put("targetId", targetId)
            put("sdp", sdp)
        }
        socket?.emit("call:sdp-answer", data)
    }
}