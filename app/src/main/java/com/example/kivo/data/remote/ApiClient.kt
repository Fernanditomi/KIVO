package com.example.kivo.data.remote

import android.net.Uri
import com.example.kivo.BuildConfig
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiClient {
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private fun rewriteBase(requestUrl: okhttp3.HttpUrl, base: String): okhttp3.HttpUrl? {
        val baseUri = try {
            Uri.parse(base.trimEnd('/'))
        } catch (e: Exception) {
            return null
        }
        val builder = requestUrl.newBuilder()
            .scheme(baseUri.scheme ?: return null)
            .host(baseUri.host ?: return null)
            .port(if (baseUri.port >= 0) baseUri.port else if (baseUri.scheme == "https") 443 else 80)
        return builder.build()
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
        .addInterceptor(FailoverInterceptor())
        .addInterceptor(logging)
        .build()

    val baseUrl = BuildConfig.KIVO_BASE_URL

    val service: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BaseUrlRouter.url())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    /**
     * Redirige cada peticion al host preferido. Si un host no responde
     * (DNS caido, timeout, servidor caido) prueba con el siguiente y lo
     * recuerda para las siguientes llamadas.
     */
    class FailoverInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
            val bases = NetworkMonitor.baseUrls()
            if (bases.isEmpty()) return chain.proceed(chain.request())

            var request = chain.request()
            var lastError: Exception? = null

            for (base in bases) {
                val url = if (base == NetworkMonitor.preferredBase()) request.url else
                    rewriteBase(request.url, base) ?: request.url
                val target = request.newBuilder().url(url).build()

                try {
                    val response = chain.proceed(target)
                    if (response.isSuccessful) {
                        NetworkMonitor.markSuccess(base)
                    }
                    return response
                } catch (e: IOException) {
                    lastError = e
                    if (bases.size > 1) {
                        NetworkMonitor.markFailure(base)
                    }
                } catch (e: Exception) {
                    lastError = e
                    if (bases.size > 1) {
                        NetworkMonitor.markFailure(base)
                    }
                }
            }
            throw lastError ?: IOException("Sin conexión con el servidor")
        }
    }

    /**
     * La URL base del Retrofit: siempre el host preferido actual.
     * Retrofit la usa para rutas relativas (uploads), por eso se consulta en vivo.
     */
    object BaseUrlRouter {
        fun url(): String = NetworkMonitor.preferredBase()
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
            return "${NetworkMonitor.preferredBase().trimEnd('/')}$url"
        }
        return try {
            val uri = Uri.parse(url)
            val host = uri.host ?: return url
            if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
                "${NetworkMonitor.preferredBase().trimEnd('/')}${uri.path ?: ""}"
            } else {
                url
            }
        } catch (e: Exception) {
            url
        }
    }
}