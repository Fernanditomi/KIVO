package com.example.kivo.data.models

data class User(
    val userId: String = "",
    val username: String = "",
    val usernameLowercase: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val bio: String = "",
    val password: String = "", // Guardado en base de datos propia
    val createdAt: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis(),
    val isOnline: Boolean = false
)
