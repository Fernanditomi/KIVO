package com.example.kivo.media

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.kivo.BuildConfig
import com.example.kivo.data.models.SpotifyTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class SpotifyPlayerManager(private val context: Context) {

    companion object {
        private const val TAG = "SpotifyPlayerManager"
        private const val CLIENT_ID = BuildConfig.SPOTIFY_CLIENT_ID
        private const val REDIRECT_URI = "kivo://spotify-callback"
        private const val REQUEST_CODE = 1337
        private const val WEB_API_BASE = "https://api.spotify.com/v1/"
    }

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken = _accessToken.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentTrack = MutableStateFlow<SpotifyTrack?>(null)
    val currentTrack = _currentTrack.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position = _position.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(0)
    val repeatMode = _repeatMode.asStateFlow()

    private val client = OkHttpClient()

    fun getLoginIntent(activity: Activity): Intent {
        val scopes = "streaming user-read-playback-state user-modify-playback-state user-read-currently-playing"
        val authUrl = "https://accounts.spotify.com/authorize" +
                "?client_id=$CLIENT_ID" +
                "&response_type=token" +
                "&redirect_uri=${Uri.encode(REDIRECT_URI)}" +
                "&scope=${Uri.encode(scopes)}" +
                "&show_dialog=true"
        return Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
    }

    fun handleLoginResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        val uri = data?.data
        if (uri != null && uri.toString().startsWith(REDIRECT_URI)) {
            val fragment = uri.fragment
            if (fragment != null) {
                val params = fragment.split("&").associate {
                    val parts = it.split("=", limit = 2)
                    parts[0] to (parts.getOrElse(1) { "" })
                }
                val token = params["access_token"]
                if (token != null) {
                    _accessToken.value = token
                    _isConnected.value = true
                    Log.d(TAG, "Spotify login successful, token received")
                    return true
                }
                val error = params["error"]
                if (error != null) {
                    Log.e(TAG, "Spotify auth error: $error")
                    _isConnected.value = false
                    return false
                }
            }
        }
        Log.d(TAG, "Spotify auth cancelled or no token in response")
        return false
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        handleLoginResult(requestCode, resultCode, data)
    }

    fun setAccessToken(token: String) {
        _accessToken.value = token
        _isConnected.value = true
        Log.d(TAG, "Spotify token set directly")
    }

    fun play() {
        val token = _accessToken.value ?: return
        apiRequest("PUT", "me/player/play", token)
        _isPlaying.value = true
    }

    fun pause() {
        val token = _accessToken.value ?: return
        apiRequest("PUT", "me/player/pause", token)
        _isPlaying.value = false
    }

    fun togglePlayPause() {
        if (_isPlaying.value) pause() else play()
    }

    fun skipToNext() {
        val token = _accessToken.value ?: return
        apiRequest("POST", "me/player/next", token)
    }

    fun skipToPrevious() {
        val token = _accessToken.value ?: return
        apiRequest("POST", "me/player/previous", token)
    }

    fun seekTo(positionMs: Int) {
        val token = _accessToken.value ?: return
        apiRequest("PUT", "me/player/seek?position_ms=$positionMs", token)
        _position.value = positionMs.toLong()
    }

    fun toggleShuffle() {
        val token = _accessToken.value ?: return
        val newState = !_shuffleEnabled.value
        apiRequest("PUT", "me/player/shuffle?state=$newState", token)
        _shuffleEnabled.value = newState
    }

    fun setRepeatMode(mode: Int) {
        val token = _accessToken.value ?: return
        val modeStr = when (mode) { 1 -> "track"; 2 -> "context"; else -> "off" }
        apiRequest("PUT", "me/player/repeat?state=$modeStr", token)
        _repeatMode.value = mode
    }

    fun playTrack(uri: String) {
        val token = _accessToken.value ?: return
        val body = JSONObject().apply {
            put("uris", org.json.JSONArray().put(uri))
        }
        apiRequest("PUT", "me/player/play", token, body.toString())
        _isPlaying.value = true
    }

    fun playTracks(uris: List<String>, startIndex: Int = 0) {
        val token = _accessToken.value ?: return
        val body = JSONObject().apply {
            put("uris", org.json.JSONArray(uris))
            put("offset", JSONObject().put("position", startIndex))
        }
        apiRequest("PUT", "me/player/play", token, body.toString())
        _isPlaying.value = true
    }

    fun refreshPlaybackState() {
        val token = _accessToken.value ?: return
        val request = Request.Builder()
            .url("${WEB_API_BASE}me/player")
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to get playback state: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let { json ->
                        try {
                            val obj = JSONObject(json)
                            _isPlaying.value = !obj.optBoolean("is_playing", true)
                            _shuffleEnabled.value = obj.optJSONObject("shuffle_state")?.let { false } ?: obj.optBoolean("shuffle_state", false)
                            _repeatMode.value = when (obj.optString("repeat_state", "off")) {
                                "track" -> 1; "context" -> 2; else -> 0
                            }

                            val progressMs = obj.optLong("progress_ms", 0)
                            _position.value = progressMs

                            obj.optJSONObject("item")?.let { item ->
                                _duration.value = item.optLong("duration_ms", 0)
                                _currentTrack.value = SpotifyTrack(
                                    id = item.optString("id", ""),
                                    name = item.optString("name", ""),
                                    uri = item.optString("uri", ""),
                                    durationMs = item.optLong("duration_ms", 0).toInt(),
                                    explicit = item.optBoolean("explicit", false),
                                    popularity = item.optInt("popularity", 0),
                                    previewUrl = item.optString("preview_url", null),
                                    artists = item.optJSONArray("artists")?.let { arr ->
                                        (0 until arr.length()).map { i ->
                                            val a = arr.getJSONObject(i)
                                            com.example.kivo.data.models.SpotifyArtist(
                                                id = a.optString("id", ""),
                                                name = a.optString("name", ""),
                                                uri = a.optString("uri", "")
                                            )
                                        }
                                    } ?: emptyList(),
                                    album = item.optJSONObject("album")?.let { alb ->
                                        com.example.kivo.data.models.SpotifyAlbum(
                                            id = alb.optString("id", ""),
                                            name = alb.optString("name", ""),
                                            releaseDate = alb.optString("release_date", ""),
                                            images = alb.optJSONArray("images")?.let { imgs ->
                                                (0 until imgs.length()).map { i ->
                                                    val img = imgs.getJSONObject(i)
                                                    com.example.kivo.data.models.SpotifyImage(
                                                        url = img.optString("url", ""),
                                                        height = img.optInt("height", 0),
                                                        width = img.optInt("width", 0)
                                                    )
                                                }
                                            } ?: emptyList(),
                                            totalTracks = alb.optInt("total_tracks", 0)
                                        )
                                    } ?: com.example.kivo.data.models.SpotifyAlbum(
                                        id = "", name = "", releaseDate = "",
                                        images = emptyList(), totalTracks = 0
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Parse error: ${e.message}")
                        }
                    }
                }
            }
        })
    }

    private fun apiRequest(method: String, endpoint: String, token: String, body: String? = null) {
        val url = "${WEB_API_BASE}$endpoint"
        val requestBody = body?.toRequestBody("application/json".toMediaType())

        val requestBuilder = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")

        when (method) {
            "GET" -> requestBuilder.get()
            "POST" -> requestBuilder.post(requestBody ?: "".toRequestBody(null))
            "PUT" -> requestBuilder.put(requestBody ?: "".toRequestBody(null))
            "DELETE" -> requestBuilder.delete()
        }

        client.newCall(requestBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "API request failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d(TAG, "API $method $endpoint: ${response.code}")
            }
        })
    }

    fun logout() {
        _accessToken.value = null
        _isConnected.value = false
        _isPlaying.value = false
        _currentTrack.value = null
        _shuffleEnabled.value = false
        _repeatMode.value = 0
    }

    fun getPosition(): Long = _position.value
    fun getDuration(): Long = _duration.value
}
