package com.example.kivo.ui.viewmodels

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.example.kivo.data.models.Song
import com.example.kivo.data.models.kivoSongs
import com.example.kivo.data.notifications.NotificationCenter
import com.example.kivo.data.remote.YouTubeResult
import com.example.kivo.data.remote.YouTubeService
import com.example.kivo.media.MusicPlayerManager
import com.example.kivo.media.YouTubeBackgroundPlayer
import com.example.kivo.media.YouTubeMediaService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val playerManager = MusicPlayerManager(application)
    private val prefs = application.getSharedPreferences("kivo_music_prefs", Context.MODE_PRIVATE)

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isYouTubeSong = MutableStateFlow(false)
    val isYouTubeSong: StateFlow<Boolean> = _isYouTubeSong.asStateFlow()

    private val _youtubeIsPlaying = MutableStateFlow(false)
    val youtubeIsPlaying: StateFlow<Boolean> = _youtubeIsPlaying.asStateFlow()

    private val _youtubeCurrentTime = MutableStateFlow(0L)
    val youtubeCurrentTime: StateFlow<Long> = _youtubeCurrentTime.asStateFlow()

    private val _youtubeDuration = MutableStateFlow(0L)
    val youtubeDuration: StateFlow<Long> = _youtubeDuration.asStateFlow()

    val isPlaying: StateFlow<Boolean> = combine(
        playerManager.isPlaying, _youtubeIsPlaying, _isYouTubeSong
    ) { playerPlaying, ytPlaying, isYt -> if (isYt) ytPlaying else playerPlaying }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val playbackState: StateFlow<Int> = combine(
        playerManager.playbackState, _youtubeDuration, _isYouTubeSong
    ) { playerState, ytDuration, isYt ->
        if (isYt) {
            if (ytDuration > 0) Player.STATE_READY else Player.STATE_BUFFERING
        } else playerState
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Player.STATE_IDLE)

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

    val isShuffleEnabled: StateFlow<Boolean> = combine(
        playerManager.isShuffleModeEnabled, YouTubeBackgroundPlayer.isShuffleEnabled, _isYouTubeSong
    ) { playerShuffle, ytShuffle, isYt -> if (isYt) ytShuffle else playerShuffle }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _sessionFirstSongId = MutableStateFlow<String?>(null)
    private var lastPreviousClickTime = 0L

    private val _songs = MutableStateFlow(kivoSongs)
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private var progressJob: Job? = null

    // YouTube integration
    private val youtubeService = YouTubeService()
    private val _isPlayingFullSong = MutableStateFlow(false)
    val isPlayingFullSong: StateFlow<Boolean> = _isPlayingFullSong.asStateFlow()
    private val _youtubeError = MutableStateFlow<String?>(null)
    val youtubeError: StateFlow<String?> = _youtubeError.asStateFlow()

    private val _youtubeSearchResults = MutableStateFlow<List<YouTubeResult>>(emptyList())
    val youtubeSearchResults: StateFlow<List<YouTubeResult>> = _youtubeSearchResults.asStateFlow()
    private val _isSearchingYouTube = MutableStateFlow(false)
    val isSearchingYouTube: StateFlow<Boolean> = _isSearchingYouTube.asStateFlow()
    private var youtubeSearchJob: Job? = null

    private data class YouTubeMeta(
        val id: String?,
        val title: String?,
        val artist: String?,
        val thumbnail: String?,
        val durationSeconds: Int
    )

    init {
        restoreLastState()
        startProgressUpdate()
        observeMediaIdChanges()
        observeYouTubePlayer()
    }

    fun searchYouTube(query: String) {
        if (query.isBlank()) {
            _youtubeSearchResults.value = emptyList()
            return
        }

        youtubeSearchJob?.cancel()
        youtubeSearchJob = viewModelScope.launch {
            _isSearchingYouTube.value = true
            delay(500)
            try {
                val results = youtubeService.search(query)
                _youtubeSearchResults.value = results
            } catch (e: Exception) {
                Log.e("MusicViewModel", "YouTube search error: ${e.message}", e)
                _youtubeSearchResults.value = emptyList()
            } finally {
                _isSearchingYouTube.value = false
            }
        }
    }

    fun playYouTubeResult(video: YouTubeResult) {
        val results = _youtubeSearchResults.value
        val index = results.indexOfFirst { it.videoId == video.videoId }.coerceAtLeast(0)
        YouTubeBackgroundPlayer.setQueue(results, index)
        playYouTubeVideo(video)
    }

    private fun playYouTubeVideo(video: YouTubeResult) {
        _youtubeError.value = null
        _currentSong.value = Song(
            id = "yt_${video.videoId}",
            title = video.title,
            artist = video.channelName,
            duration = formatDuration(video.lengthSeconds),
            durationSeconds = video.lengthSeconds,
            imageUrl = video.thumbnailUrl
        )
        _progress.value = 0f
        _currentTime.value = "0:00"
        _totalTime.value = formatDuration(video.lengthSeconds)
        _isMiniPlayerVisible.value = true
        _isYouTubeSong.value = true
        if (playerManager.isPlaying.value) playerManager.pause()
        YouTubeBackgroundPlayer.play(
            videoId = video.videoId,
            title = video.title,
            artist = video.channelName,
            thumbnail = video.thumbnailUrl,
            durationSeconds = video.lengthSeconds
        )
        YouTubeMediaService.start(getApplication())
    }

    private fun observeYouTubePlayer() {
        viewModelScope.launch {
            combine(
                YouTubeBackgroundPlayer.currentVideoId,
                YouTubeBackgroundPlayer.title,
                YouTubeBackgroundPlayer.artist,
                YouTubeBackgroundPlayer.thumbnail,
                YouTubeBackgroundPlayer.durationSeconds
            ) { id, title, artist, thumbnail, duration ->
                YouTubeMeta(id, title, artist, thumbnail, duration)
            }.collect { meta ->
                if (meta.id != null) {
                    Log.d("MusicViewModel", "YT now playing: ${meta.id}")
                }
                if (meta.id != null && _isYouTubeSong.value) {
                    val song = Song(
                        id = "yt_${meta.id}",
                        title = meta.title ?: "",
                        artist = meta.artist ?: "",
                        duration = formatDuration(meta.durationSeconds),
                        durationSeconds = meta.durationSeconds,
                        imageUrl = meta.thumbnail
                    )
                    if (song != _currentSong.value) {
                        _currentSong.value = song
                    }
                }
            }
        }
        viewModelScope.launch {
            combine(
                YouTubeBackgroundPlayer.isPlaying,
                YouTubeBackgroundPlayer.positionMs,
                YouTubeBackgroundPlayer.durationMs
            ) { playing, pos, dur -> Triple(playing, pos, dur) }
                .collect { (playing, pos, dur) ->
                    _youtubeIsPlaying.value = playing
                    _youtubeCurrentTime.value = pos
                    _youtubeDuration.value = dur
                    if (_isYouTubeSong.value) {
                        _progress.value = if (dur > 0) pos.toFloat() / dur else 0f
                        _currentTime.value = formatTime(pos)
                        _totalTime.value = formatTime(dur)
                    }
                }
        }
    }

    private fun restoreLastState() {
        val ytId = YouTubeBackgroundPlayer.currentVideoId.value
        if (ytId != null) {
            _currentSong.value = Song(
                id = "yt_$ytId",
                title = YouTubeBackgroundPlayer.title.value ?: "",
                artist = YouTubeBackgroundPlayer.artist.value ?: "",
                duration = formatDuration(YouTubeBackgroundPlayer.durationSeconds.value),
                durationSeconds = YouTubeBackgroundPlayer.durationSeconds.value,
                imageUrl = YouTubeBackgroundPlayer.thumbnail.value
            )
            _isYouTubeSong.value = true
            _isMiniPlayerVisible.value = true
            return
        }
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
                if (_isYouTubeSong.value) {
                    delay(1000)
                    continue
                }
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

    private fun formatDuration(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%d:%02d", minutes, secs)
    }

    fun togglePlayPause() {
        _isMiniPlayerVisible.value = true
        if (_isYouTubeSong.value) {
            YouTubeBackgroundPlayer.toggle()
            return
        }
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
        if (_isYouTubeSong.value) {
            YouTubeBackgroundPlayer.toggleShuffle()
        } else {
            playerManager.toggleShuffle()
        }
    }

    fun playSong(song: Song) {
        if (_sessionFirstSongId.value == null) {
            _sessionFirstSongId.value = song.id
        }
        _isMiniPlayerVisible.value = true
        if (_isYouTubeSong.value) {
            YouTubeBackgroundPlayer.stop()
            _isYouTubeSong.value = false
        }
        _currentSong.value = song
        playerManager.play(song)
        _progress.value = 0f
        prefs.edit().putString("last_song_id", song.id).apply()
    }

    fun nextSong() {
        if (_isYouTubeSong.value) {
            YouTubeBackgroundPlayer.next()
            _progress.value = 0f
            _currentTime.value = "0:00"
            return
        }
        playerManager.next()
        _progress.value = 0f
    }

    fun previousSong() {
        if (_isYouTubeSong.value) {
            YouTubeBackgroundPlayer.previous()
            _progress.value = 0f
            _currentTime.value = "0:00"
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
        if (_isYouTubeSong.value) {
            YouTubeBackgroundPlayer.seekForward(10000L)
            return
        }
        playerManager.seekForward()
    }

    fun seekBackward() {
        if (_isYouTubeSong.value) {
            YouTubeBackgroundPlayer.seekBackward(10000L)
            return
        }
        playerManager.seekBackward()
    }

    fun dismissMiniPlayer() {
        _isMiniPlayerVisible.value = false
        if (_isYouTubeSong.value) {
            YouTubeBackgroundPlayer.stop()
        } else {
            playerManager.pause()
        }
    }

    fun updateProgress(value: Float) {
        if (_isYouTubeSong.value) {
            val duration = YouTubeBackgroundPlayer.durationMs.value
            if (duration > 0) {
                YouTubeBackgroundPlayer.seekTo((value * duration).toLong())
            }
            _progress.value = value
            return
        }
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
