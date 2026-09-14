package com.example.kivo.data.remote

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class YouTubeResult(
    val videoId: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val lengthSeconds: Int
)

class YouTubeService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val instances = listOf(
        "https://vid.puffyan.us",
        "https://invidious.fdn.fr",
        "https://inv.nadeko.net",
        "https://invidious.privacyredirect.com",
        "https://yt.artemislena.eu"
    )

    private var currentInstance = instances[0]

    suspend fun search(query: String): List<YouTubeResult> = withContext(Dispatchers.IO) {
        try {
            val url = "$currentInstance/api/v1/search?q=${query.replace(" ", "+")}&type=video&sort_by=relevance"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Kivo/1.0")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext emptyList()
                val jsonArray = JSONArray(body)
                val results = mutableListOf<YouTubeResult>()

                for (i in 0 until minOf(jsonArray.length(), 10)) {
                    val obj = jsonArray.getJSONObject(i)
                    if (obj.optString("type") == "video") {
                        results.add(
                            YouTubeResult(
                                videoId = obj.optString("videoId", ""),
                                title = obj.optString("title", ""),
                                channelName = obj.optString("author", ""),
                                thumbnailUrl = "$currentInstance${obj.optString("videoThumbnails", "")}".let {
                                    val thumbs = obj.optJSONArray("videoThumbnails")
                                    if (thumbs != null && thumbs.length() > 0) {
                                        val maxRes = thumbs.getJSONObject(thumbs.length() - 1)
                                        maxRes.optString("url", "")
                                    } else ""
                                },
                                lengthSeconds = obj.optInt("lengthSeconds", 0)
                            )
                        )
                    }
                }
                results
            } else {
                tryNextInstance()
                search(query)
            }
        } catch (e: Exception) {
            tryNextInstance()
            try {
                search(query)
            } catch (e2: Exception) {
                emptyList()
            }
        }
    }

    suspend fun getStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = "$currentInstance/api/v1/videos/$videoId"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Kivo/1.0")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)

                val adaptiveFormats = json.optJSONArray("adaptiveFormats")
                if (adaptiveFormats != null) {
                    for (i in 0 until adaptiveFormats.length()) {
                        val format = adaptiveFormats.getJSONObject(i)
                        val type = format.optString("type", "")
                        if (type.contains("audio") && type.contains("opus")) {
                            return@withContext format.optString("url", null)
                        }
                    }
                    for (i in 0 until adaptiveFormats.length()) {
                        val format = adaptiveFormats.getJSONObject(i)
                        val type = format.optString("type", "")
                        if (type.contains("audio")) {
                            return@withContext format.optString("url", null)
                        }
                    }
                }

                val formatStreams = json.optJSONArray("formatStreams")
                if (formatStreams != null && formatStreams.length() > 0) {
                    return@withContext formatStreams.getJSONObject(0).optString("url", null)
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun tryNextInstance() {
        val idx = instances.indexOf(currentInstance)
        currentInstance = instances[(idx + 1) % instances.size]
    }
}
