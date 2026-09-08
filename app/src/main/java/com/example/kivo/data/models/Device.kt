package com.example.kivo.data.models

data class Device(
    val deviceId: String = "",
    val userId: String = "",
    val fcmToken: String = "",
    val platform: String = "android",
    val updatedAt: Long = System.currentTimeMillis()
)
