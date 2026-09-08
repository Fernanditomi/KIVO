package com.example.kivo.ui.viewmodels

import android.app.Application
import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.kivo.data.models.Song
import com.example.kivo.data.models.kivoSongs
import com.example.kivo.data.notifications.NotificationCenter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class TapDifficulty(
    val msPerNote: Long,
    val travelTime: Long,
    val multiplier: Int,
    val label: String
) {
    EASY(1500L, 3000L, 1, "Fácil"),
    NORMAL(1000L, 2500L, 2, "Normal"),
    HARD(380L, 1400L, 3, "Difícil") // Mucho más rápido y frenético
}

enum class GameStatus {
    IDLE, COUNTDOWN, PLAYING, GAMEOVER, VICTORY
}

data class GameNote(
    val id: String = UUID.randomUUID().toString(),
    val column: Int,
    val targetTimeMs: Long,
    var isHit: Boolean = false,
    var isMissed: Boolean = false
)

data class TapGameState(
    val score: Int = 0,
    val combo: Int = 0,
    val maxCombo: Int = 0,
    val status: GameStatus = GameStatus.IDLE,
    val isPaused: Boolean = false,
    val currentSong: Song? = null,
    val difficulty: TapDifficulty = TapDifficulty.EASY,
    val countdown: Int? = null,
    val progress: Float = 0f,
    val highScore: Int = 0
)

class TapChallengeViewModel(application: Application) : AndroidViewModel(application) {
    private var player: ExoPlayer = ExoPlayer.Builder(application).build()
    private val prefs = application.getSharedPreferences("kivo_games_prefs", Context.MODE_PRIVATE)

    private val _gameState = mutableStateOf(TapGameState())
    val gameState: State<TapGameState> = _gameState

    private val _gameTimeMs = MutableStateFlow(0L)
    val gameTimeMs = _gameTimeMs.asStateFlow()

    private val _songBeatMap = MutableStateFlow<List<GameNote>>(emptyList())
    val songBeatMap = _songBeatMap.asStateFlow()

    private var gameLoopJob: Job? = null
    private var pauseTimerJob: Job? = null

    fun selectSong(song: Song) {
        resetState()
        _gameState.value = TapGameState(currentSong = song, difficulty = _gameState.value.difficulty)
        loadHighScore(song.id, _gameState.value.difficulty)
    }

    fun selectDifficulty(difficulty: TapDifficulty) {
        _gameState.value = _gameState.value.copy(difficulty = difficulty)
        _gameState.value.currentSong?.let { loadHighScore(it.id, difficulty) }
    }

    private fun loadHighScore(songId: String, difficulty: TapDifficulty) {
        val key = "high_score_${songId}_${difficulty.name}"
        _gameState.value = _gameState.value.copy(highScore = prefs.getInt(key, 0))
    }

    fun startCountdown() {
        if (_gameState.value.status != GameStatus.IDLE && _gameState.value.status != GameStatus.GAMEOVER && _gameState.value.status != GameStatus.VICTORY) return
        
        stopEverything()
        _gameState.value = _gameState.value.copy(status = GameStatus.COUNTDOWN, countdown = 3)
        
        viewModelScope.launch {
            for (i in 3 downTo 1) {
                _gameState.value = _gameState.value.copy(countdown = i)
                delay(1000)
            }
            _gameState.value = _gameState.value.copy(countdown = null)
            generateBeatMap()
            startGame()
        }
    }

    private fun generateBeatMap() {
        val song = _gameState.value.currentSong ?: return
        val difficulty = _gameState.value.difficulty
        val durationMs = song.durationSeconds * 1000L
        
        val msPerNote = difficulty.msPerNote
        val newMap = mutableListOf<GameNote>()
        var currentTime = 2000L 
        var lastColumn = -1
        
        while (currentTime < durationMs - 2000) {
            var column = (0..3).random()
            if (column == lastColumn) column = (0..3).random()
            lastColumn = column
            newMap.add(GameNote(column = column, targetTimeMs = currentTime))
            currentTime += msPerNote
        }
        _songBeatMap.value = newMap
    }

    private fun startGame() {
        val song = _gameState.value.currentSong ?: return
        val difficulty = _gameState.value.difficulty
        _gameState.value = _gameState.value.copy(status = GameStatus.PLAYING, score = 0, combo = 0)
        
        val mediaItem = MediaItem.fromUri(song.audioUrl ?: "")
        player.setMediaItem(mediaItem)
        player.prepare()

        gameLoopJob = viewModelScope.launch(Dispatchers.Main) {
            val startTime = System.currentTimeMillis()
            while (isActive && _gameState.value.status == GameStatus.PLAYING) {
                if (!_gameState.value.isPaused) {
                    val elapsed = System.currentTimeMillis() - startTime
                    _gameTimeMs.value = elapsed
                    
                    // Lógica de notas perdidas
                    val missedNote = _songBeatMap.value.find { !it.isHit && !it.isMissed && (elapsed - it.targetTimeMs) > 400 }
                    if (missedNote != null) {
                        gameOver()
                        break
                    }

                    if (elapsed >= (song.durationSeconds * 1000L)) {
                        victory()
                        break
                    }

                    _gameState.value = _gameState.value.copy(
                        progress = elapsed.toFloat() / (song.durationSeconds * 1000f)
                    )
                }
                delay(8)
            }
        }
    }

    fun onNoteTap(noteId: String) {
        if (_gameState.value.status != GameStatus.PLAYING || _gameState.value.isPaused) return
        
        val currentList = _songBeatMap.value
        val noteIndex = currentList.indexOfFirst { it.id == noteId }
        
        if (noteIndex != -1) {
            val note = currentList[noteIndex]
            if (!note.isHit && !note.isMissed) {
                val newList = currentList.toMutableList()
                newList[noteIndex] = note.copy(isHit = true)
                _songBeatMap.value = newList

                player.play()
                resetPauseTimer()

                val newScore = _gameState.value.score + (100 * _gameState.value.difficulty.multiplier)
                val newCombo = _gameState.value.combo + 1
                
                _gameState.value = _gameState.value.copy(
                    score = newScore,
                    combo = newCombo,
                    maxCombo = maxOf(_gameState.value.maxCombo, newCombo)
                )
            }
        }
    }

    private fun resetPauseTimer() {
        pauseTimerJob?.cancel()
        pauseTimerJob = viewModelScope.launch {
            delay(2000) 
            player.pause()
        }
    }

    fun onEmptyTap() {
        if (_gameState.value.status == GameStatus.PLAYING && !_gameState.value.isPaused) {
            gameOver()
        }
    }

    private fun gameOver() {
        player.pause()
        _gameState.value = _gameState.value.copy(status = GameStatus.GAMEOVER, combo = 0)
        saveHighScore()
    }

    private fun victory() {
        player.pause()
        _gameState.value = _gameState.value.copy(status = GameStatus.VICTORY)
        saveHighScore()
    }

    private fun saveHighScore() {
        val song = _gameState.value.currentSong ?: return
        val difficulty = _gameState.value.difficulty
        val key = "high_score_${song.id}_${difficulty.name}"
        if (_gameState.value.score > _gameState.value.highScore) {
            prefs.edit().putInt(key, _gameState.value.score).apply()
            _gameState.value = _gameState.value.copy(highScore = _gameState.value.score)
            NotificationCenter.newRecord(_gameState.value.score, difficulty.label)
        }
    }

    fun pauseGame() {
        _gameState.value = _gameState.value.copy(isPaused = true)
        player.pause()
        pauseTimerJob?.cancel()
    }

    fun resumeGame() {
        _gameState.value = _gameState.value.copy(isPaused = false)
    }

    fun restartGame() {
        stopEverything()
        resetState()
        startCountdown()
    }

    private fun resetState() {
        _gameState.value = _gameState.value.copy(
            score = 0,
            combo = 0,
            status = GameStatus.IDLE,
            progress = 0f,
            isPaused = false,
            countdown = null
        )
        _gameTimeMs.value = 0L
        _songBeatMap.value = emptyList()
    }

    fun exitGame() {
        stopEverything()
        _gameState.value = TapGameState()
    }

    private fun stopEverything() {
        gameLoopJob?.cancel()
        pauseTimerJob?.cancel()
        player.stop()
        player.clearMediaItems()
    }

    override fun onCleared() {
        super.onCleared()
        stopEverything()
        player.release()
    }
}
