package com.example.kivo.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kivo.data.models.User
import com.example.kivo.data.repositories.ChatRepository
import com.example.kivo.ui.state.UserSearchState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class UserSearchViewModel : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchState = MutableStateFlow<UserSearchState>(UserSearchState.Idle)
    val searchState = _searchState.asStateFlow()

    @OptIn(FlowPreview::class)
    val searchFlow = _searchQuery
        .debounce(500)
        .onEach { query ->
            if (query.length < 3) {
                _searchState.value = if (query.isEmpty()) UserSearchState.Idle else UserSearchState.InvalidQuery
            } else {
                _searchState.value = UserSearchState.Searching
            }
        }
        .filter { it.length >= 3 }
        .map { query -> ChatRepository.searchUsers(query) }
        .onEach { results ->
            _searchState.value = if (results.isEmpty()) UserSearchState.NoResults else UserSearchState.Results(results)
        }
        .launchIn(viewModelScope)

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }
    
    suspend fun getOrCreateConversation(myId: String, otherUserId: String): String {
        return ChatRepository.getOrCreateConversation(myId, otherUserId)
    }
}
