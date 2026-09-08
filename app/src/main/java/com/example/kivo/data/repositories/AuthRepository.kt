package com.example.kivo.data.repositories

import com.example.kivo.data.models.User
import com.example.kivo.data.remote.ApiClient
import com.example.kivo.data.remote.ClerkToken
import com.example.kivo.data.remote.PresenceRequest
import com.example.kivo.data.remote.ProfileUpdateRequest
import com.example.kivo.data.remote.PushManager
import com.example.kivo.data.remote.SessionManager
import com.example.kivo.data.remote.SocketManager
import com.clerk.api.Clerk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

object AuthRepository {

    fun getCurrentUserId(): String? = SessionManager.getCachedUser()?.userId

    fun isUserLoggedIn(): Boolean = SessionManager.getToken() != null

    fun connectRealtimeIfLoggedIn() {
        val token = SessionManager.getToken() ?: return
        SocketManager.connect(token)
    }

    fun clearLocalSession() {
        PushManager.onUserLoggedOut()
        SocketManager.disconnect()
        SessionManager.clear()
    }

    /**
     * Completa el inicio de sesion despues de que el flujo de Clerk termina:
     * trae el perfil del backend (/me, que aprovisiona el usuario en Postgres),
     * guarda la sesion local y conecta el socket.
     */
    suspend fun completeClerkSignIn(): Result<User> = withContext(Dispatchers.IO) {
        val token = ClerkToken.fresh()
            ?: return@withContext Result.failure(Exception("No hay sesión activa de Clerk"))

        var lastError: Throwable? = null
        repeat(3) { attempt ->
            try {
                val me = ApiClient.service.getMe()
                SessionManager.saveSession(token, me)
                PushManager.onUserLoggedIn(me.userId)
                connectRealtimeIfLoggedIn()
                return@withContext Result.success(me)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                lastError = e
                if (!isTransientError(e)) {
                    return@withContext Result.failure(e)
                }
                if (attempt < 2) delay(1000L * (attempt + 1))
            }
        }

        val cached = SessionManager.getCachedUser()
        if (cached != null && SessionManager.getToken() != null) {
            connectRealtimeIfLoggedIn()
            return@withContext Result.success(cached)
        }
        Result.failure(lastError ?: Exception("No se pudo iniciar sesión"))
    }

    private fun isTransientError(e: Exception): Boolean {
        if (e is HttpException) {
            val code = e.code()
            return code == 429 || code >= 500
        }
        return e is IOException
    }

    suspend fun signOut() {
        withContext(Dispatchers.IO) {
            runCatching { Clerk.auth.signOut() }
        }
        val userId = getCurrentUserId()
        if (userId != null) {
            runCatching { ApiClient.service.setPresence(userId, PresenceRequest(false)) }
        }
        clearLocalSession()
    }

    suspend fun updateProfile(user: User): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updated = ApiClient.service.updateProfile(
                user.userId,
                ProfileUpdateRequest(
                    username = user.username,
                    displayName = user.displayName,
                    bio = user.bio,
                    photoUrl = user.photoUrl
                )
            )
            SessionManager.updateCachedUser(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(userId: String): User? = withContext(Dispatchers.IO) {
        runCatching { ApiClient.service.getUser(userId) }.getOrNull()
            ?: SessionManager.getCachedUser()?.takeIf { it.userId == userId }
    }
}