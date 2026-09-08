package com.example.kivo.data.remote

import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult

object ClerkToken {

    suspend fun fresh(): String? {
        if (!Clerk.isInitialized.value) return null
        val result = Clerk.auth.getToken()
        if (result is ClerkResult.Success) {
            return result.value
        }
        return null
    }
}