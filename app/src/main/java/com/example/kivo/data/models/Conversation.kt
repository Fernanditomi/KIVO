package com.example.kivo.data.models

data class Conversation(
    val conversationId: String = "",
    val participants: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastMessage: String = "",
    val lastMessageAt: Long = System.currentTimeMillis(),
    val lastMessageSenderId: String = "",
    val unreadCount: Map<String, Int> = emptyMap() // userId -> count
)
