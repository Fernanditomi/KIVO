package com.example.kivo.data.models

data class Message(
    val messageId: String = "",
    val conversationId: String? = null,
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val type: String = "text",
    val isRead: Boolean = false,
    val status: String = "sent", // sent, delivered, read
    val senderName: String = ""
)
