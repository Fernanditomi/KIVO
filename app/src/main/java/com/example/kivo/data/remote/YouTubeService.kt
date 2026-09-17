package com.example.kivo.data.remote

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
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
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun search(query: String): List<YouTubeResult> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.youtube.com/results?search_query=$encodedQuery&sp=EgIQAQ%3D%3D"
            val request = Request.Builder()
                .url(url)
                .addHeader(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                )
                .addHeader("Accept-Language", "es;q=0.9,en;q=0.8")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e("YouTubeService", "HTTP ${response.code}")
                return@withContext emptyList()
            }

            val html = response.body?.string() ?: return@withContext emptyList()
            parseSearchResults(html)
        } catch (e: Exception) {
            Log.e("YouTubeService", "Search error: ${e.message}", e)
            emptyList()
        }
    }

    private fun parseSearchResults(html: String): List<YouTubeResult> {
        val results = mutableListOf<YouTubeResult>()
        try {
            val dataStart = html.indexOf("var ytInitialData = ")
            if (dataStart == -1) return emptyList()
            val jsonStart = dataStart + "var ytInitialData = ".length
            val jsonEnd = html.indexOf(";</script>", jsonStart)
            if (jsonEnd == -1) return emptyList()

            val json = JSONObject(html.substring(jsonStart, jsonEnd))
            val contents = json
                .optJSONObject("contents")
                ?.optJSONObject("twoColumnSearchResultsRenderer")
                ?.optJSONObject("primaryContents")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents") ?: return emptyList()

            for (i in 0 until contents.length()) {
                val section = contents.optJSONObject(i) ?: continue
                val items = section
                    .optJSONObject("itemSectionRenderer")
                    ?.optJSONArray("contents") ?: continue

                for (j in 0 until items.length()) {
                    val item = items.optJSONObject(j) ?: continue
                    val vr = item.optJSONObject("videoRenderer") ?: continue

                    val videoId = vr.optString("videoId", "")
                    if (videoId.isEmpty()) continue

                    val title = vr
                        .optJSONObject("title")
                        ?.optJSONArray("runs")
                        ?.optJSONObject(0)
                        ?.optString("text", "") ?: ""

                    val channelName = vr
                        .optJSONObject("ownerText")
                        ?.optJSONArray("runs")
                        ?.optJSONObject(0)
                        ?.optString("text", "") ?: ""

                    val thumbnailArray = vr
                        .optJSONObject("thumbnail")
                        ?.optJSONArray("thumbnails")
                    val thumbnailUrl = if (thumbnailArray != null && thumbnailArray.length() > 0) {
                        thumbnailArray.getJSONObject(thumbnailArray.length() - 1)
                            .optString("url", "")
                    } else ""

                    val lengthText = vr
                        .optJSONObject("lengthText")
                        ?.optString("simpleText", "") ?: "0:00"
                    val lengthSeconds = parseDuration(lengthText)

                    results.add(
                        YouTubeResult(
                            videoId = videoId,
                            title = title,
                            channelName = channelName,
                            thumbnailUrl = thumbnailUrl,
                            lengthSeconds = lengthSeconds
                        )
                    )

                    if (results.size >= 15) break
                }
                if (results.size >= 15) break
            }
        } catch (e: Exception) {
            Log.e("YouTubeService", "Parse error: ${e.message}", e)
        }

        Log.d("YouTubeService", "Parsed ${results.size} results")
        return results
    }

    private fun parseDuration(text: String): Int {
        val parts = text.split(":")
        return try {
            when (parts.size) {
                3 -> parts[0].toInt() * 3600 + parts[1].toInt() * 60 + parts[2].toInt()
                2 -> parts[0].toInt() * 60 + parts[1].toInt()
                1 -> parts[0].toInt()
                else -> 0
            }
        } catch (e: Exception) { 0 }
    }

    suspend fun getStreamUrl(videoId: String, context: Context): String? = withContext(Dispatchers.IO) {
        try {
            Log.d("YouTubeService", "getStreamUrl via backend proxy for: $videoId")

            val proxyUrl = "https://kivo-backend-fsvk.onrender.com/api/youtube/stream?videoId=$videoId"
            val request = Request.Builder()
                .url(proxyUrl)
                .addHeader("Accept", "audio/*")
                .get()
                .build()

            val response = client.newCall(request).execute()
            Log.d("YouTubeService", "Proxy response: ${response.code}")

            if (response.isSuccessful) {
                Log.d("YouTubeService", "Backend proxy streams audio for $videoId")
                return@withContext proxyUrl
            }

            val body = response.body?.string() ?: ""
            Log.e("YouTubeService", "Proxy failed: ${response.code} - $body")
            null
        } catch (e: Exception) {
            Log.e("YouTubeService", "Stream error: ${e.message}", e)
            null
        }
    }
}
