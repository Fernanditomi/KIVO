package com.example.kivo.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kivo.data.models.Message
import com.example.kivo.data.models.User
import com.example.kivo.data.remote.AudioRecorder
import com.example.kivo.data.repositories.AuthRepository
import com.example.kivo.data.repositories.ChatRepository
import com.example.kivo.ui.state.ChatState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val myId = AuthRepository.getCurrentUserId() ?: ""
    private val audioRecorder = AudioRecorder()
    
    private val _chatState = MutableStateFlow<ChatState>(ChatState.Idle)
    val chatState = _chatState.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _otherUser = MutableStateFlow<User?>(null)
    val otherUser = _otherUser.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText = _inputText.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _recordingAmplitude = MutableStateFlow(0f)
    val recordingAmplitude = _recordingAmplitude.asStateFlow()

    private var recordingConversationId: String = ""
    private var recordingOtherUserId: String = ""

    fun initChat(conversationId: String, otherUserId: String) {
        viewModelScope.launch {
            _chatState.value = ChatState.Loading
            _otherUser.value = AuthRepository.getUserProfile(otherUserId)
            
            ChatRepository.getMessages(conversationId)
                .onEach { msgs ->
                    _messages.value = msgs
                    _chatState.value = if (msgs.isEmpty()) ChatState.Empty else ChatState.Connected
                    ChatRepository.markAsRead(conversationId, myId)
                }
                .catch { _chatState.value = ChatState.Error(it.message ?: "Error de conexión") }
                .launchIn(viewModelScope)
        }
    }

    fun onInputTextChange(text: String) {
        _inputText.value = text
    }

    fun sendMessage(conversationId: String, otherUserId: String) {
        val text = _inputText.value.trim()
        if (text.isEmpty()) return

        val message = Message(
            senderId = myId,
            receiverId = otherUserId,
            text = text,
            createdAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            _chatState.value = ChatState.SendingMessage
            try {
                ChatRepository.sendMessage(conversationId, message)
                _chatState.value = ChatState.MessageSent
                _inputText.value = ""
            } catch (e: Exception) {
                _chatState.value = ChatState.MessageFailed
            }
        }
    }

    fun sendImage(conversationId: String, otherUserId: String, mimeType: String, bytes: ByteArray) {
        viewModelScope.launch {
            _chatState.value = ChatState.SendingMessage
            try {
                val imageUrl = ChatRepository.uploadImage(mimeType, bytes)
                val message = Message(
                    senderId = myId,
                    receiverId = otherUserId,
                    text = imageUrl,
                    type = "image",
                    createdAt = System.currentTimeMillis()
                )
                ChatRepository.sendMessage(conversationId, message)
                _chatState.value = ChatState.MessageSent
            } catch (e: Exception) {
                _chatState.value = ChatState.MessageFailed
            }
        }
    }

    fun clearMessages(conversationId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = ChatRepository.clearMessages(conversationId)
            if (ok) {
                _messages.value = emptyList()
                _chatState.value = ChatState.Empty
            }
            onResult(ok)
        }
    }

    fun deleteConversation(conversationId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = ChatRepository.deleteConversation(conversationId)
            onResult(ok)
        }
    }

    fun toggleAudioRecording(conversationId: String, otherUserId: String) {
        if (_isRecording.value) {
            val file = audioRecorder.stopRecording()
            _isRecording.value = false
            _recordingAmplitude.value = 0f

            if (file != null && file.exists() && file.length() > 0) {
                sendAudio(conversationId, otherUserId, file)
            }
        } else {
            recordingConversationId = conversationId
            recordingOtherUserId = otherUserId
            audioRecorder.startRecording { amplitude ->
                _recordingAmplitude.value = amplitude
            }
            _isRecording.value = true
        }
    }

    private fun sendAudio(conversationId: String, otherUserId: String, audioFile: java.io.File) {
        viewModelScope.launch {
            _chatState.value = ChatState.SendingMessage
            try {
                android.util.Log.d("KIVO_AUDIO", "File size: ${audioFile.length()} bytes, path: ${audioFile.absolutePath}")
                val audioUrl = ChatRepository.uploadAudio(audioFile)
                android.util.Log.d("KIVO_AUDIO", "Upload OK: $audioUrl")
                val message = Message(
                    senderId = myId,
                    receiverId = otherUserId,
                    text = audioUrl,
                    type = "audio",
                    createdAt = System.currentTimeMillis()
                )
                ChatRepository.sendMessage(conversationId, message)
                android.util.Log.d("KIVO_AUDIO", "Message sent OK")
                _chatState.value = ChatState.MessageSent
            } catch (e: Exception) {
                android.util.Log.e("KIVO_AUDIO", "Error: ${e.message}", e)
                _chatState.value = ChatState.MessageFailed
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.release()
    }
}
