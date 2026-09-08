package com.example.kivo.data.remote

import android.net.Uri
import com.example.kivo.BuildConfig
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val token = runBlocking { ClerkToken.fresh() } ?: SessionManager.getToken()
            val request = if (token != null) {
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }
        .addInterceptor(logging)
        .build()

    val baseUrl = BuildConfig.KIVO_BASE_URL

    val service: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    /**
     * Resuelve la URL de una imagen al host actual del backend.
     * - Rutas relativas del servidor (ej. /uploads/x.jpg) -> baseUrl + ruta.
     * - URLs absolutas de un host privado antiguo (ej. 192.168.x.x) -> se reescriben al host actual.
     * - URLs externas (Clerk, fallbacks) y rutas locales del dispositivo quedan igual.
     */
    fun resolveUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        if (url.startsWith("/uploads/")) {
            return "${baseUrl.trimEnd('/')}$url"
        }
        return try {
            val uri = Uri.parse(url)
            val host = uri.host ?: return url
            if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
                "${baseUrl.trimEnd('/')}${uri.path ?: ""}"
            } else {
                url
            }
        } catch (e: Exception) {
            url
        }
    }
}