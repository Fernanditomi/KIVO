package com.example.kivo.ui.viewmodels

import android.app.Application
import android.os.Bundle
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kivo.ai.models.AiIntent
import com.example.kivo.ai.models.AiMessage
import com.example.kivo.ai.models.AiResponse
import com.example.kivo.ai.repository.KivoAiRepository
import com.example.kivo.data.models.Song
import com.example.kivo.data.models.kivoSongs
import kotlinx.coroutines.launch

data class KivoAiMessage(
    val text: String,
    val isUser: Boolean,
    val song: Song? = null
)

class KivoAiViewModel(
    private val repository: KivoAiRepository = KivoAiRepository()
) : ViewModel() {

    private val _messages = mutableStateListOf<KivoAiMessage>(
        KivoAiMessage(text = "¡Hola! Soy KIVO AI 🤖. Tu asistente musical inteligente.", isUser = false),
        KivoAiMessage(text = "¿En qué puedo ayudarte hoy?", isUser = false)
    )
    val messages: List<KivoAiMessage> = _messages

    private val _isProcessing = mutableStateOf(false)
    val isProcessing: State<Boolean> = _isProcessing

    fun sendMessage(text: String, musicViewModel: MusicViewModel) {
        if (text.isBlank()) return

        _messages.add(KivoAiMessage(text = text, isUser = true))
        _isProcessing.value = true

        val context = mapOf(
            "currentSong" to musicViewModel.currentSong.value,
            "isPlaying" to musicViewModel.isPlaying.value
        )

        viewModelScope.launch {
            try {
                val response = repository.getAiResponse(text, context)
                
                // Add AI reply with the associated song if any
                _messages.add(KivoAiMessage(
                    text = response.replyText, 
                    isUser = false,
                    song = response.recommendedSong
                ))
                
                // Execute automatic actions
                handleAiAction(response.recommendedSong, response.intent, musicViewModel)
                
            } catch (e: Exception) {
                _messages.add(KivoAiMessage(text = "Lo siento, tuve un problema. 😕", isUser = false))
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun handleAiAction(recommendedSong: Song?, intent: AiIntent, musicViewModel: MusicViewModel) {
        when (intent) {
            AiIntent.PLAY_SONG, AiIntent.SEARCH_ARTIST, AiIntent.MUSIC_BY_MOOD, AiIntent.MUSIC_BY_GENRE -> {
                recommendedSong?.let { musicViewModel.playSong(it) }
            }
            AiIntent.SAVE_SONG -> {
                musicViewModel.currentSong.value?.let { musicViewModel.toggleFavorite(it.id) }
            }
            else -> {}
        }
    }
    
    fun sendQuickResponse(text: String, musicViewModel: MusicViewModel) {
        sendMessage(text, musicViewModel)
    }
}
