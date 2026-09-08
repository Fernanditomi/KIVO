package com.example.kivo.ui.viewmodels

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kivo.data.models.Song
import com.example.kivo.data.models.kivoSongs
import kotlinx.coroutines.*

enum class GameMode {
    SURVIVAL, TOURNAMENT, ARTIST_VS_ARTIST, CUSTOM
}

data class GameSong(
    val id: String,
    val title: String,
    val artist: String,
    val albumImage: String?,
    val previewUrl: String?,
    val wins: Int = 0,
    val losses: Int = 0
)

data class QueCancionSalvasState(
    val round: Int = 1,
    val winner: GameSong? = null,
    val challenger: GameSong? = null,
    val playingSongId: String? = null,
    val isGameOver: Boolean = false,
    val gameWinner: GameSong? = null,
    val savedCount: Int = 0,
    val progress: Float = 0f,
    val mode: GameMode? = null,
    val history: List<String> = emptyList(), // Store only IDs
    val ranking: List<GameSong> = emptyList()
)

class QueCancionSalvasViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val prefs = application.getSharedPreferences("que_cancion_salvas_prefs", Context.MODE_PRIVATE)
    
    private var mediaPlayer: MediaPlayer? = null
    private var playbackJob: Job? = null

    private val _state = mutableStateOf(QueCancionSalvasState())
    val state: State<QueCancionSalvasState> = _state

    init {
        loadRanking()
    }

    private fun getAllSongs(): List<GameSong> {
        return kivoSongs.map { s ->
            GameSong(
                id = s.id,
                title = s.title,
                artist = s.artist,
                albumImage = s.imageUrl,
                previewUrl = s.audioUrl,
                wins = prefs.getInt("wins_${s.id}", 0),
                losses = prefs.getInt("losses_${s.id}", 0)
            )
        }
    }

    private fun loadRanking() {
        _state.value = _state.value.copy(ranking = getAllSongs().sortedByDescending { it.wins }.take(10))
    }

    fun setMode(mode: GameMode) {
        stopAudio()
        val all = getAllSongs().shuffled()
        if (all.size >= 2) {
            _state.value = QueCancionSalvasState(
                winner = all[0],
                challenger = all[1],
                mode = mode,
                ranking = _state.value.ranking,
                history = listOf(all[0].id, all[1].id)
            )
        }
    }

    fun playSong(songId: String, url: String) {
        if (_state.value.playingSongId == songId) {
            stopAudio()
            return
        }

        stopAudio()
        if (url.isEmpty()) return

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.parse(url))
                setOnPreparedListener { 
                    it.start() 
                    startProgressTimer()
                }
                prepareAsync()
            }
            _state.value = _state.value.copy(playingSongId = songId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startProgressTimer() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            val total = 10000L
            val interval = 100L
            var current = 0L
            while (current < total) {
                delay(interval)
                current += interval
                _state.value = _state.value.copy(progress = current.toFloat() / total)
            }
            stopAudio()
        }
    }

    fun selectWinner(saved: GameSong) {
        val currentState = _state.value
        val winner = currentState.winner ?: return
        val challenger = currentState.challenger ?: return
        
        val loser = if (saved.id == winner.id) challenger else winner
        
        // Persist stats immediately
        val newWins = saved.wins + 1
        val newLosses = loser.losses + 1
        prefs.edit().putInt("wins_${saved.id}", newWins).apply()
        prefs.edit().putInt("losses_${loser.id}", newLosses).apply()

        stopAudio()

        // Get next rival
        val usedIds = currentState.history
        val nextChallenger = getAllSongs()
            .filter { it.id !in usedIds }
            .shuffled()
            .firstOrNull()

        if (nextChallenger == null) {
            _state.value = currentState.copy(
                isGameOver = true,
                gameWinner = saved.copy(wins = newWins)
            )
        } else {
            _state.value = currentState.copy(
                round = currentState.round + 1,
                winner = saved.copy(wins = newWins),
                challenger = nextChallenger,
                savedCount = currentState.savedCount + 1,
                history = usedIds + nextChallenger.id
            )
        }
        loadRanking()
    }

    fun resetToMenu() {
        stopAudio()
        _state.value = QueCancionSalvasState(ranking = _state.value.ranking)
    }

    private fun stopAudio() {
        playbackJob?.cancel()
        try {
            mediaPlayer?.stop()
        } catch (e: Exception) {}
        mediaPlayer?.release()
        mediaPlayer = null
        _state.value = _state.value.copy(playingSongId = null, progress = 0f)
    }

    override fun onCleared() {
        super.onCleared()
        stopAudio()
    }
}
