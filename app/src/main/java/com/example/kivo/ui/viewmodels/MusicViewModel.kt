package com.example.kivo.ui.viewmodels

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.example.kivo.data.models.Song
import com.example.kivo.data.models.SpotifyTrack
import com.example.kivo.data.models.kivoSongs
import com.example.kivo.data.notifications.NotificationCenter
import com.example.kivo.data.remote.SpotifyApiService
import com.example.kivo.media.MusicPlayerManager
import com.example.kivo.media.SpotifyPlayerManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val playerManager = MusicPlayerManager(application)
    val spotifyManager = SpotifyPlayerManager(application)
    private val spotifyAuthManager = com.example.kivo.data.remote.SpotifyAuthManager(application)
    private val prefs = application.getSharedPreferences("kivo_music_prefs", Context.MODE_PRIVATE)

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val playbackState: StateFlow<Int> = playerManager.playbackState
    val errorMessage: StateFlow<String?> = playerManager.errorMessage

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _currentTime = MutableStateFlow("0:00")
    val currentTime = _currentTime.asStateFlow()

    private val _totalTime = MutableStateFlow("0:00")
    val totalTime = _totalTime.asStateFlow()

    private val _favoriteSongIds = MutableStateFlow<Set<String>>(
        prefs.getStringSet("favorites", emptySet()) ?: emptySet()
    )
    val favoriteSongIds: StateFlow<Set<String>> = _favoriteSongIds.asStateFlow()

    private val _isMiniPlayerVisible = MutableStateFlow(false)
    val isMiniPlayerVisible: StateFlow<Boolean> = _isMiniPlayerVisible.asStateFlow()

    val isShuffleEnabled: StateFlow<Boolean> = playerManager.isShuffleModeEnabled

    private val _sessionFirstSongId = MutableStateFlow<String?>(null)
    private var lastPreviousClickTime = 0L

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filteredSongs = MutableStateFlow(kivoSongs)
    val filteredSongs = _filteredSongs.asStateFlow()

    private var progressJob: Job? = null

    // Spotify integration
    val spotifyConnected: StateFlow<Boolean> = spotifyManager.isConnected
    private val _spotifySearchResults = MutableStateFlow<List<SpotifyTrack>>(emptyList())
    val spotifySearchResults: StateFlow<List<SpotifyTrack>> = _spotifySearchResults.asStateFlow()
    private val _isSearchingSpotify = MutableStateFlow(false)
    val isSearchingSpotify: StateFlow<Boolean> = _isSearchingSpotify.asStateFlow()

    // YouTube integration
    private val youtubeService = com.example.kivo.data.remote.YouTubeService()
    private val _isPlayingFullSong = MutableStateFlow(false)
    val isPlayingFullSong: StateFlow<Boolean> = _isPlayingFullSong.asStateFlow()
    private val _youtubeError = MutableStateFlow<String?>(null)
    val youtubeError: StateFlow<String?> = _youtubeError.asStateFlow()

    private val spotifyApiService: SpotifyApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.spotify.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SpotifyApiService::class.java)
    }

    init {
        restoreLastState()
        startProgressUpdate()
        observeMediaIdChanges()
    }

    fun getSpotifyLoginIntent(activity: Activity): Intent = spotifyManager.getLoginIntent(activity)

    fun handleSpotifyActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        spotifyManager.onActivityResult(requestCode, resultCode, data)
    }

    fun handleSpotifyToken(token: String) {
        spotifyManager.setAccessToken(token)
    }

    fun searchSpotify(query: String) {
        if (query.isBlank()) {
            _spotifySearchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isSearchingSpotify.value = true
            try {
                val token = spotifyAuthManager.getAccessToken()
                if (token == null) {
                    _spotifySearchResults.value = emptyList()
                    Log.e("MusicViewModel", "Failed to get Spotify token")
                    return@launch
                }
                val response = spotifyApiService.search(
                    auth = "Bearer $token",
                    query = query,
                    type = "track",
                    market = "US"
                )

                if (response.isSuccessful) {
                    _spotifySearchResults.value = response.body()?.tracks?.items ?: emptyList()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "no body"
                    Log.e("MusicViewModel", "Spotify search failed: ${response.code()} $errorBody")
                    _spotifySearchResults.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Search error: ${e.message}", e)
                _spotifySearchResults.value = emptyList()
            } finally {
                _isSearchingSpotify.value = false
            }
        }
    }

    fun playSpotifyTrack(track: SpotifyTrack) {
        viewModelScope.launch {
            _isPlayingFullSong.value = true
            _youtubeError.value = null

            try {
                val query = "${track.name} ${track.artists.joinToString(" ") { it.name }} official"
                val results = youtubeService.search(query)

                if (results.isNotEmpty()) {
                    val video = results.first()
                    val streamUrl = youtubeService.getStreamUrl(video.videoId)

                    if (streamUrl != null) {
                        _currentSong.value = Song(
                            id = "yt_${video.videoId}",
                            title = track.name,
                            artist = track.artists.joinToString(", ") { it.name },
                            duration = "${track.durationMs / 60000}:${String.format("%02d", (track.durationMs % 60000) / 1000)}",
                            durationSeconds = track.durationMs / 1000,
                            imageUrl = track.album.images.firstOrNull()?.url
                        )
                        _isMiniPlayerVisible.value = true

                        playerManager.playFromUrl(
                            url = streamUrl,
                            id = "yt_${video.videoId}",
                            title = track.name,
                            artist = track.artists.joinToString(", ") { it.name }
                        )
                    } else {
                        _youtubeError.value = "No se pudo obtener el audio"
                    }
                } else {
                    _youtubeError.value = "No se encontró en YouTube"
                }
            } catch (e: Exception) {
                _youtubeError.value = "Error de conexión"
            } finally {
                _isPlayingFullSong.value = false
            }
        }
    }

    fun logoutSpotify() {
        spotifyManager.logout()
        _spotifySearchResults.value = emptyList()
    }

    private fun restoreLastState() {
        val lastSongId = prefs.getString("last_song_id", null)
        if (lastSongId != null) {
            _currentSong.value = kivoSongs.find { it.id == lastSongId }
        }
    }

    private fun observeMediaIdChanges() {
        viewModelScope.launch {
            playerManager.currentMediaId.collect { mediaId ->
                if (mediaId != null) {
                    val song = kivoSongs.find { it.id == mediaId }
                    if (song != null && song != _currentSong.value) {
                        _currentSong.value = song
                        prefs.edit().putString("last_song_id", song.id).apply()
                    }
                }
            }
        }
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            var endedNotifiedFor: String? = null
            while (true) {
                if (spotifyManager.isConnected.value && spotifyManager.currentTrack.value != null) {
                    val track = spotifyManager.currentTrack.value
                    val duration = spotifyManager.duration.value
                    val position = spotifyManager.getPosition()

                    if (track != null && duration > 0) {
                        _progress.value = position.toFloat() / duration
                        _currentTime.value = formatTime(position)
                        _totalTime.value = formatTime(duration)
                    }
                } else {
                    val player = playerManager.getPlayer()
                    if (player != null) {
                        val duration = player.duration.coerceAtLeast(0L)
                        val currentPos = player.currentPosition.coerceAtLeast(0L)
                        if (duration > 0) {
                            _progress.value = currentPos.toFloat() / duration
                            _currentTime.value = formatTime(currentPos)
                            _totalTime.value = formatTime(duration)
                        }
                        if (player.playbackState == Player.STATE_ENDED) {
                            val song = _currentSong.value
                            if (song != null && endedNotifiedFor != song.id) {
                                endedNotifiedFor = song.id
                                NotificationCenter.songFinished(song)
                            }
                        } else {
                            endedNotifiedFor = null
                        }
                    }
                }
                delay(1000)
            }
        }
    }

    private fun formatTime(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    fun togglePlayPause() {
        _isMiniPlayerVisible.value = true
        if (spotifyManager.isConnected.value) {
            spotifyManager.togglePlayPause()
        } else {
            if (playerManager.isPlaying.value) {
                playerManager.pause()
            } else {
                val player = playerManager.getPlayer()
                if (player == null || player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                    _currentSong.value?.let { playSong(it) }
                } else {
                    playerManager.resume()
                }
            }
        }
    }

    fun toggleFavorite(songId: String) {
        val currentFavorites = _favoriteSongIds.value
        val newFavorites = if (currentFavorites.contains(songId)) {
            currentFavorites - songId
        } else {
            currentFavorites + songId
        }
        _favoriteSongIds.value = newFavorites
        prefs.edit().putStringSet("favorites", newFavorites).apply()

        if (!currentFavorites.contains(songId)) {
            kivoSongs.find { it.id == songId }?.let { NotificationCenter.favoriteAdded(it) }
        }
    }

    fun toggleShuffle() {
        if (spotifyManager.isConnected.value) {
            spotifyManager.toggleShuffle()
        } else {
            playerManager.toggleShuffle()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _filteredSongs.value = kivoSongs
        } else {
            _filteredSongs.value = kivoSongs.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true)
            }
            if (spotifyManager.isConnected.value) {
                searchSpotify(query)
            }
        }
    }

    fun playSong(song: Song) {
        if (_sessionFirstSongId.value == null) {
            _sessionFirstSongId.value = song.id
        }
        _isMiniPlayerVisible.value = true
        _currentSong.value = song
        playerManager.play(song)
        _progress.value = 0f
        prefs.edit().putString("last_song_id", song.id).apply()
    }

    fun nextSong() {
        if (spotifyManager.isConnected.value) {
            spotifyManager.skipToNext()
        } else {
            playerManager.next()
            _progress.value = 0f
        }
    }

    fun previousSong() {
        if (spotifyManager.isConnected.value) {
            spotifyManager.skipToPrevious()
            _progress.value = 0f
            return
        }

        val player = playerManager.getPlayer() ?: return
        val currentTime = System.currentTimeMillis()
        val timeSinceLastClick = currentTime - lastPreviousClickTime

        if (player.currentPosition > 3000) {
            playerManager.seekTo(0L)
            _progress.value = 0f
            lastPreviousClickTime = currentTime
            return
        }

        if (timeSinceLastClick < 1000) {
            if (_currentSong.value?.id != _sessionFirstSongId.value) {
                playerManager.previous()
                _progress.value = 0f
            }
            lastPreviousClickTime = 0
        } else {
            playerManager.seekTo(0L)
            _progress.value = 0f
            lastPreviousClickTime = currentTime
        }
    }

    fun seekForward() {
        if (spotifyManager.isConnected.value) {
            val current = spotifyManager.getPosition()
            spotifyManager.seekTo((current + 10000).toInt())
        } else {
            playerManager.seekForward()
        }
    }

    fun seekBackward() {
        if (spotifyManager.isConnected.value) {
            val current = spotifyManager.getPosition()
            spotifyManager.seekTo((current - 10000).toInt().coerceAtLeast(0))
        } else {
            playerManager.seekBackward()
        }
    }

    fun dismissMiniPlayer() {
        _isMiniPlayerVisible.value = false
        if (spotifyManager.isConnected.value) {
            spotifyManager.pause()
        } else {
            playerManager.pause()
        }
    }

    fun updateProgress(value: Float) {
        _progress.value = value
        if (spotifyManager.isConnected.value) {
            val duration = spotifyManager.getDuration()
            if (duration > 0) {
                spotifyManager.seekTo((value * duration).toInt())
            }
        } else {
            val duration = playerManager.getPlayer()?.duration ?: 0L
            if (duration > 0) {
                playerManager.seekTo((value * duration).toLong())
            }
        }
    }

    fun setEqualizerGain(band: Int, gain: Int) {
        playerManager.setEqualizerGain(band, gain)
    }

    fun setBassBoost(strength: Int) {
        playerManager.setBassBoost(strength)
    }

    fun setVirtualizer(strength: Int) {
        playerManager.setVirtualizer(strength)
    }

    suspend fun getEqualizerData(): Bundle? {
        return playerManager.getEqualizerData()
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
