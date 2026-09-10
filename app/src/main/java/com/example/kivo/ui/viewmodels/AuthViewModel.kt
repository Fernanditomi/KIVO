package com.example.kivo.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.example.kivo.data.models.User
import com.example.kivo.data.repositories.AuthRepository
import com.example.kivo.ui.state.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.CheckingSession)
    val authState = _authState.asStateFlow()

    init {
        observeClerk()
    }

    private fun observeClerk() {
        viewModelScope.launch {
            Clerk.isInitialized.collect { initialized ->
                if (!initialized) {
                    if (AuthRepository.isUserLoggedIn()) {
                        val cached = com.example.kivo.data.remote.SessionManager.getCachedUser()
                        if (cached != null) {
                            _authState.value = AuthState.Authenticated(cached)
                            AuthRepository.connectRealtimeIfLoggedIn()
                            return@collect
                        }
                    }
                    AuthRepository.clearLocalSession()
                    _authState.value = AuthState.Unauthenticated
                }
            }
        }
        viewModelScope.launch {
            Clerk.isAuthFlowCompleteFlow.collect { authComplete ->
                if (!Clerk.isInitialized.value) return@collect
                if (authComplete) {
                    completeClerkSignIn()
                } else {
                    if (!AuthRepository.isUserLoggedIn()) {
                        AuthRepository.clearLocalSession()
                        _authState.value = AuthState.Unauthenticated
                    }
                }
            }
        }
    }

    fun checkSession() {
        viewModelScope.launch { completeClerkSignIn() }
    }

    private suspend fun completeClerkSignIn() {
        val current = _authState.value
        if (current is AuthState.Authenticated) return

        _authState.value = AuthState.CheckingSession
        AuthRepository.completeClerkSignIn()
            .onSuccess { user -> _authState.value = AuthState.Authenticated(user) }
            .onFailure { e ->
                _authState.value = AuthState.Error(e.message ?: "No se pudo iniciar sesión")
            }
    }

    fun logout() {
        viewModelScope.launch {
            _authState.value = AuthState.SigningOut
            AuthRepository.signOut()
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun syncProfile(name: String, username: String, photoUrl: String?, bio: String) {
        val uid = AuthRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            val user = User(
                userId = uid,
                username = username.lowercase().trim(),
                usernameLowercase = username.lowercase().trim(),
                displayName = name,
                photoUrl = photoUrl,
                bio = bio
            )
            AuthRepository.updateProfile(user)
            _authState.value = AuthState.Authenticated(user)
        }
    }
}