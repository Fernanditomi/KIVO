package com.example.kivo.ui.state

import com.example.kivo.data.models.User

sealed class AuthState {
    object Idle : AuthState()
    object CheckingSession : AuthState()
    object Authenticating : AuthState()
    object CreatingAccount : AuthState()
    object AccountCreated : AuthState()
    data class Authenticated(val user: User) : AuthState()
    object Unauthenticated : AuthState()
    object SigningOut : AuthState()
    data class Error(val message: String) : AuthState()
}

sealed class UsernameState {
    object Idle : UsernameState()
    object Checking : UsernameState()
    object Available : UsernameState()
    object Taken : UsernameState()
    object Invalid : UsernameState()
    data class Error(val message: String) : UsernameState()
}

sealed class ChatState {
    object Idle : ChatState()
    object Loading : ChatState()
    object Loaded : ChatState()
    object Empty : ChatState()
    object Connecting : ChatState()
    object Connected : ChatState()
    object Disconnected : ChatState()
    object SendingMessage : ChatState()
    object MessageSent : ChatState()
    object MessageFailed : ChatState()
    data class Error(val message: String) : ChatState()
}

sealed class UserSearchState {
    object Idle : UserSearchState()
    object Searching : UserSearchState()
    data class Results(val users: List<User>) : UserSearchState()
    object NoResults : UserSearchState()
    object InvalidQuery : UserSearchState()
    data class Error(val message: String) : UserSearchState()
}

sealed class NetworkState {
    object Online : NetworkState()
    object Offline : NetworkState()
    object Reconnecting : NetworkState()
}
