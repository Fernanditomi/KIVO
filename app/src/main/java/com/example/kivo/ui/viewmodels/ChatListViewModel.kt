package com.example.kivo.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kivo.data.models.Conversation
import com.example.kivo.data.models.User
import com.example.kivo.data.repositories.AuthRepository
import com.example.kivo.data.repositories.ChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatListViewModel : ViewModel() {
    private val myId = AuthRepository.getCurrentUserId() ?: ""

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations = _conversations.asStateFlow()

    private val _usersInfo = MutableStateFlow<Map<String, User>>(emptyMap())
    val usersInfo = _usersInfo.asStateFlow()

    init {
        if (myId.isNotEmpty()) {
            loadConversations()
        }
    }

    private fun loadConversations() {
        ChatRepository.getConversations(myId)
            .onEach { convs ->
                _conversations.value = convs
                fetchOtherUsersInfo(convs)
            }
            .launchIn(viewModelScope)
    }

    private fun fetchOtherUsersInfo(convs: List<Conversation>) {
        val otherUserIds = convs.flatMap { it.participants }.filter { it != myId }.distinct()
        viewModelScope.launch {
            val newUsers = otherUserIds.associateWith { userId ->
                AuthRepository.getUserProfile(userId)
            }.filterValues { it != null } as Map<String, User>
            
            _usersInfo.value = _usersInfo.value + newUsers
        }
    }
}
