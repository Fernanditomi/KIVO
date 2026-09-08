package com.example.kivo.ui.viewmodels

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.example.kivo.data.models.Song
import com.example.kivo.data.models.kivoSongs
import com.example.kivo.data.notifications.NotificationCenter
import com.example.kivo.media.MusicPlayerManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val playerManager = MusicPlayerManager(application)
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

    init {
        restoreLastState()
        startProgressUpdate()
        observeMediaIdChanges()
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
        playerManager.toggleShuffle()
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
        playerManager.next()
        _progress.value = 0f
    }

    fun previousSong() {
        val player = playerManager.getPlayer() ?: return
        val currentTime = System.currentTimeMillis()
        val timeSinceLastClick = currentTime - lastPreviousClickTime
        
        // Si han pasado más de 3 segundos, siempre reinicia la canción
        if (player.currentPosition > 3000) {
            playerManager.seekTo(0L)
            _progress.value = 0f
            lastPreviousClickTime = currentTime
            return
        }

        // Lógica de Doble Toque (dentro de un umbral de 1 segundo)
        if (timeSinceLastClick < 1000) {
            // Solo retrocede si la canción actual NO es la primera elegida en la sesión
            if (_currentSong.value?.id != _sessionFirstSongId.value) {
                playerManager.previous()
                _progress.value = 0f
            }
            lastPreviousClickTime = 0 // Reset tras retroceder
        } else {
            // Primer toque: Reinicia la canción
            playerManager.seekTo(0L)
            _progress.value = 0f
            lastPreviousClickTime = currentTime
        }
    }

    fun seekForward() = playerManager.seekForward()
    fun seekBackward() = playerManager.seekBackward()

    fun dismissMiniPlayer() {
        _isMiniPlayerVisible.value = false
        playerManager.pause()
    }

    fun updateProgress(value: Float) {
        _progress.value = value
        val duration = playerManager.getPlayer()?.duration ?: 0L
        if (duration > 0) {
            playerManager.seekTo((value * duration).toLong())
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
