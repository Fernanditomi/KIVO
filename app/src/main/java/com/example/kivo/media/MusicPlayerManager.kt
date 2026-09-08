package com.example.kivo.media

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.example.kivo.data.models.Song
import com.example.kivo.data.models.kivoSongs
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicPlayerManager(context: Context) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    val playbackState = _playbackState.asStateFlow()

    private val _currentMediaId = MutableStateFlow<String?>(null)
    val currentMediaId = _currentMediaId.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _isShuffleModeEnabled = MutableStateFlow(false)
    val isShuffleModeEnabled = _isShuffleModeEnabled.asStateFlow()

    init {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupController()
            } catch (e: Exception) {
                _errorMessage.value = "Error al conectar con el servicio"
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupController() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _playbackState.value = playbackState
                if (playbackState == Player.STATE_READY) {
                    _duration.value = mediaController?.duration ?: 0L
                    _errorMessage.value = null
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _currentMediaId.value = mediaItem?.mediaId
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _isShuffleModeEnabled.value = shuffleModeEnabled
            }

            override fun onPlayerError(error: PlaybackException) {
                _errorMessage.value = "Error de reproducción: ${error.message}"
                _isPlaying.value = false
            }
        })
        
        // Initialize queue if not already set
        if (mediaController?.mediaItemCount == 0) {
            setPlaylist(kivoSongs)
        }
    }

    private fun setPlaylist(songs: List<Song>) {
        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setUri(song.audioUrl)
                .setMediaId(song.id)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setArtworkUri(song.imageUrl?.let { Uri.parse(it) })
                        .build()
                )
                .build()
        }
        mediaController?.setMediaItems(mediaItems)
        mediaController?.prepare()
    }

    fun play(song: Song) {
        mediaController?.let { controller ->
            // Find index in current queue or update queue
            var index = -1
            for (i in 0 until controller.mediaItemCount) {
                if (controller.getMediaItemAt(i).mediaId == song.id) {
                    index = i
                    break
                }
            }

            if (index != -1) {
                controller.seekTo(index, 0L)
                controller.play()
            } else {
                // If not in queue, just play it as a single item for now (should not happen with full list)
                val mediaItem = MediaItem.Builder()
                    .setUri(song.audioUrl)
                    .setMediaId(song.id)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(song.title)
                            .setArtist(song.artist)
                            .setArtworkUri(song.imageUrl?.let { Uri.parse(it) })
                            .build()
                    )
                    .build()
                controller.setMediaItem(mediaItem)
                controller.prepare()
                controller.play()
            }
        }
    }

    fun pause() {
        mediaController?.pause()
    }

    fun resume() {
        mediaController?.play()
    }

    fun next() {
        mediaController?.seekToNext()
    }

    fun previous() {
        mediaController?.seekToPrevious()
    }

    fun toggleShuffle() {
        mediaController?.let {
            it.shuffleModeEnabled = !it.shuffleModeEnabled
        }
    }

    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
    }

    fun seekForward(millis: Long = 10000L) {
        mediaController?.let {
            val target = it.currentPosition + millis
            it.seekTo(target.coerceAtMost(it.duration))
        }
    }

    fun seekBackward(millis: Long = 10000L) {
        mediaController?.let {
            val target = it.currentPosition - millis
            it.seekTo(target.coerceAtLeast(0L))
        }
    }

    fun setEqualizerGain(band: Int, gain: Int) {
        val bundle = Bundle().apply {
            putInt("band", band)
            putInt("gain", gain)
        }
        mediaController?.sendCustomCommand(SessionCommand("SET_EQUALIZER_GAIN", Bundle.EMPTY), bundle)
    }

    fun setBassBoost(strength: Int) {
        val bundle = Bundle().apply { putInt("strength", strength) }
        mediaController?.sendCustomCommand(SessionCommand("SET_BASS_BOOST", Bundle.EMPTY), bundle)
    }

    fun setVirtualizer(strength: Int) {
        val bundle = Bundle().apply { putInt("strength", strength) }
        mediaController?.sendCustomCommand(SessionCommand("SET_VIRTUALIZER", Bundle.EMPTY), bundle)
    }

    suspend fun getEqualizerData(): Bundle? {
        val controller = mediaController ?: return null
        val result = controller.sendCustomCommand(SessionCommand("GET_EQUALIZER_DATA", Bundle.EMPTY), Bundle.EMPTY).get()
        return if (result.resultCode == SessionResult.RESULT_SUCCESS) result.extras else null
    }

    fun release() {
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
    
    fun getPlayer(): Player? = mediaController
}
