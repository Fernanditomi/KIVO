package com.example.kivo.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.example.kivo.BuildConfig
import com.example.kivo.data.models.SpotifyTokenResponse
import kotlinx.coroutines.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SpotifyAuthManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("spotify_prefs", Context.MODE_PRIVATE)
    private var cachedToken: String? = prefs.getString("access_token", null)
    private var tokenExpiry: Long = prefs.getLong("token_expiry", 0L)

    private val apiService: SpotifyApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://accounts.spotify.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SpotifyApiService::class.java)
    }

    suspend fun getAccessToken(): String? {
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiry) {
            return cachedToken
        }

        return try {
            val response = apiService.getToken(
                clientId = BuildConfig.SPOTIFY_CLIENT_ID,
                clientSecret = BuildConfig.SPOTIFY_CLIENT_SECRET
            )

            if (response.isSuccessful) {
                val token = response.body()!!
                cachedToken = token.accessToken
                tokenExpiry = System.currentTimeMillis() + (token.expiresIn * 1000L) - 60000L

                prefs.edit()
                    .putString("access_token", cachedToken)
                    .putLong("token_expiry", tokenExpiry)
                    .apply()

                token.accessToken
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun clearToken() {
        cachedToken = null
        tokenExpiry = 0L
        prefs.edit().clear().apply()
    }
}
