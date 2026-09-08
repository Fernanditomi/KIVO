package com.example.kivo.ui.viewmodels

import android.app.Application
import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kivo.data.models.WhoSaidThatQuestion
import com.example.kivo.data.repositories.WhoSaidThatRepository
import kotlinx.coroutines.*

enum class WhoSaidThatStatus {
    MENU, DIFFICULTY_SELECT, PLAYING, RESULTS
}

data class WhoSaidThatState(
    val status: WhoSaidThatStatus = WhoSaidThatStatus.MENU,
    val currentQuestionIndex: Int = 0,
    val score: Int = 0,
    val streak: Int = 0,
    val maxStreak: Int = 0,
    val lives: Int = 3,
    val timeLeft: Int = 15,
    val selectedAnswer: String? = null,
    val isCorrect: Boolean? = null,
    val questions: List<WhoSaidThatQuestion> = emptyList(),
    val currentOptions: List<String> = emptyList(),
    val category: String = "Todos",
    val difficulty: String = "Cualquiera",
    val gameMode: String = "Clásico",
    val highScore: Int = 0,
    val totalCorrect: Int = 0,
    val totalPlayed: Int = 0
)

class WhoSaidThatViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("who_said_that_prefs", Context.MODE_PRIVATE)
    private val _state = mutableStateOf(WhoSaidThatState())
    val state: State<WhoSaidThatState> = _state

    private var timerJob: Job? = null

    init {
        loadStats()
    }

    private fun loadStats() {
        val highScore = prefs.getInt("high_score", 0)
        val maxStreak = prefs.getInt("max_streak", 0)
        _state.value = _state.value.copy(highScore = highScore, maxStreak = maxStreak)
    }

    fun selectMode(mode: String) {
        _state.value = _state.value.copy(
            gameMode = mode,
            status = WhoSaidThatStatus.DIFFICULTY_SELECT
        )
    }

    fun startGame(difficulty: String = "Cualquiera") {
        val mode = _state.value.gameMode
        val count = if (mode == "10 Preguntas") 10 else 50
        
        // Ajustamos la dificultad para que coincida con el repositorio
        val repoDifficulty = when(difficulty) {
            "Normal" -> "Medio"
            "Difícil" -> "Difícil"
            else -> difficulty
        }

        val questions = WhoSaidThatRepository.getQuestions(count, "Todos", repoDifficulty)
        
        if (questions.isEmpty()) {
            // Fallback if no questions for that specific difficulty yet
            val fallbackQuestions = WhoSaidThatRepository.getQuestions(count, "Todos", "Cualquiera")
            if (fallbackQuestions.isEmpty()) return
            
            _state.value = _state.value.copy(
                status = WhoSaidThatStatus.PLAYING,
                questions = fallbackQuestions,
                difficulty = "Cualquiera",
                score = 0,
                streak = 0,
                lives = 3,
                currentQuestionIndex = 0,
                totalCorrect = 0,
                totalPlayed = 0
            )
        } else {
            _state.value = _state.value.copy(
                status = WhoSaidThatStatus.PLAYING,
                questions = questions,
                difficulty = difficulty,
                score = 0,
                streak = 0,
                lives = 3,
                currentQuestionIndex = 0,
                totalCorrect = 0,
                totalPlayed = 0
            )
        }
        loadQuestion()
    }

    private fun loadQuestion() {
        val currentState = _state.value
        if (currentState.currentQuestionIndex >= currentState.questions.size) {
            endGame()
            return
        }
        
        val currentQuestion = currentState.questions[currentState.currentQuestionIndex]
        _state.value = _state.value.copy(
            selectedAnswer = null,
            isCorrect = null,
            timeLeft = 15,
            currentOptions = currentQuestion.getAllOptionsShuffled()
        )
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_state.value.timeLeft > 0 && _state.value.selectedAnswer == null) {
                delay(1000)
                _state.value = _state.value.copy(timeLeft = _state.value.timeLeft - 1)
            }
            if (_state.value.timeLeft == 0 && _state.value.selectedAnswer == null) {
                submitAnswer("") 
            }
        }
    }

    fun submitAnswer(answer: String) {
        if (_state.value.selectedAnswer != null) return
        
        timerJob?.cancel()
        val currentState = _state.value
        val currentQuestion = currentState.questions[currentState.currentQuestionIndex]
        val isCorrect = answer == currentQuestion.correctAnswer
        
        val pointsPerDifficulty = when(currentQuestion.difficulty) {
            "Fácil" -> 100
            "Medio" -> 200
            "Difícil" -> 300
            else -> 100
        }

        val newStreak = if (isCorrect) currentState.streak + 1 else 0
        val streakBonus = when {
            newStreak >= 5 -> pointsPerDifficulty
            newStreak >= 3 -> 50
            else -> 0
        }

        val newScore = if (isCorrect) currentState.score + pointsPerDifficulty + streakBonus else currentState.score
        val newLives = if (isCorrect) currentState.lives else currentState.lives - 1
        
        _state.value = currentState.copy(
            selectedAnswer = answer,
            isCorrect = isCorrect,
            score = newScore,
            streak = newStreak,
            maxStreak = maxOf(currentState.maxStreak, newStreak),
            lives = newLives,
            totalCorrect = if (isCorrect) currentState.totalCorrect + 1 else currentState.totalCorrect,
            totalPlayed = currentState.totalPlayed + 1
        )

        viewModelScope.launch {
            delay(1500)
            if (newLives <= 0 || (currentState.gameMode == "10 Preguntas" && currentState.currentQuestionIndex >= 9)) {
                endGame()
            } else {
                nextQuestion()
            }
        }
    }

    private fun nextQuestion() {
        _state.value = _state.value.copy(currentQuestionIndex = _state.value.currentQuestionIndex + 1)
        loadQuestion()
    }

    private fun endGame() {
        timerJob?.cancel()
        val isHighScore = _state.value.score > _state.value.highScore
        if (isHighScore) {
            prefs.edit().putInt("high_score", _state.value.score).apply()
        }
        prefs.edit().putInt("max_streak", _state.value.maxStreak).apply()
        
        _state.value = _state.value.copy(status = WhoSaidThatStatus.RESULTS)
    }

    fun resetToMenu() {
        timerJob?.cancel()
        _state.value = WhoSaidThatState(
            highScore = _state.value.highScore,
            maxStreak = _state.value.maxStreak
        )
    }
}
