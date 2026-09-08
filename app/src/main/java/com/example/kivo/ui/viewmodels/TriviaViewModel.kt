package com.example.kivo.ui.viewmodels

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.kivo.data.models.Song
import com.example.kivo.data.models.kivoSongs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TriviaDifficulty(val duration: Long, val multiplier: Int) {
    EASY(10000L, 1),
    MEDIUM(5000L, 2),
    HARD(2500L, 3)
}

data class TriviaGameState(
    val currentQuestionIndex: Int = 0,
    val score: Int = 0,
    val isGameOver: Boolean = false,
    val difficulty: TriviaDifficulty? = null,
    val options: List<Song> = emptyList(),
    val correctSong: Song? = null,
    val selectedSong: Song? = null,
    val isCorrect: Boolean? = null
)

class TriviaViewModel(application: Application) : AndroidViewModel(application) {
    // Creamos un reproductor independiente para el juego
    private val gamePlayer = ExoPlayer.Builder(application).build()
    
    private val _gameState = mutableStateOf(TriviaGameState())
    val gameState: State<TriviaGameState> = _gameState

    private val _timerProgress = MutableStateFlow(1f)
    val timerProgress = _timerProgress.asStateFlow()

    private var timerJob: Job? = null

    fun startNewGame(difficulty: TriviaDifficulty) {
        _gameState.value = TriviaGameState(difficulty = difficulty)
        nextQuestion()
    }

    fun nextQuestion() {
        val difficulty = _gameState.value.difficulty ?: return
        val correctSong = kivoSongs.random()
        
        val otherSongs = kivoSongs
            .filter { it.title != correctSong.title || it.artist != correctSong.artist }
            .distinctBy { it.title.lowercase() + it.artist.lowercase() }
            .shuffled()
        
        val numOptions = 4 
        val options = (otherSongs.take(numOptions - 1) + correctSong).shuffled()
        
        _gameState.value = _gameState.value.copy(
            correctSong = correctSong,
            options = options,
            selectedSong = null,
            isCorrect = null
        )
        
        playSnippet()
    }

    fun replaySnippet() {
        if (_gameState.value.selectedSong == null) {
            playSnippet()
        }
    }

    private fun playSnippet() {
        timerJob?.cancel()
        
        val song = _gameState.value.correctSong ?: return
        val difficulty = _gameState.value.difficulty ?: return
        
        val snippetDuration = difficulty.duration
        
        val maxStartSeconds = (song.durationSeconds - (snippetDuration / 1000) - 2).coerceAtLeast(0)
        val startMs = if (maxStartSeconds > 0) {
            (0..maxStartSeconds.toInt()).random().toLong() * 1000
        } else 0L

        // Configuramos y reproducimos en el reproductor local del juego
        val mediaItem = MediaItem.fromUri(song.audioUrl ?: "")
        gamePlayer.setMediaItem(mediaItem)
        gamePlayer.prepare()
        gamePlayer.seekTo(startMs)
        gamePlayer.play()
        
        timerJob = viewModelScope.launch {
            val totalTime = snippetDuration
            val interval = 50L
            var elapsedTime = 0L
            
            while (elapsedTime < totalTime) {
                if (_gameState.value.selectedSong != null) break
                delay(interval)
                elapsedTime += interval
                _timerProgress.value = 1f - (elapsedTime.toFloat() / totalTime)
            }
            gamePlayer.pause()
            _timerProgress.value = 0f
        }
    }

    fun submitAnswer(song: Song) {
        if (_gameState.value.selectedSong != null) return
        
        val difficulty = _gameState.value.difficulty ?: return
        val isCorrect = song.id == _gameState.value.correctSong?.id
        val points = if (isCorrect) 100 * difficulty.multiplier else 0
        val newScore = _gameState.value.score + points
        
        _gameState.value = _gameState.value.copy(
            selectedSong = song,
            isCorrect = isCorrect,
            score = newScore
        )
        
        gamePlayer.pause()
        
        viewModelScope.launch {
            delay(2000)
            if (_gameState.value.currentQuestionIndex < 9) {
                _gameState.value = _gameState.value.copy(
                    currentQuestionIndex = _gameState.value.currentQuestionIndex + 1
                )
                nextQuestion()
            } else {
                _gameState.value = _gameState.value.copy(isGameOver = true)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        gamePlayer.release() // Liberamos el reproductor local
    }
}
