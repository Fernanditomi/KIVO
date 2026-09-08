package com.example.kivo.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kivo.data.notifications.KivoNotification
import com.example.kivo.data.notifications.NotificationCategory
import com.example.kivo.data.notifications.NotificationStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val _filter = MutableStateFlow<NotificationCategory?>(null)
    val filter: StateFlow<NotificationCategory?> = _filter.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow(false)
    val error: StateFlow<Boolean> = _error.asStateFlow()

    val notifications: StateFlow<List<KivoNotification>> = NotificationStore.notifications
        .onEach {
            _isLoading.value = false
            _error.value = false
        }
        .catch {
            _error.value = true
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredNotifications: StateFlow<List<KivoNotification>> =
        combine(notifications, _filter) { list, category ->
            if (category == null) list else list.filter { it.category == category }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadCount: StateFlow<Int> = notifications
        .map { list -> list.count { !it.isRead } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setFilter(category: NotificationCategory?) {
        _filter.value = category
    }

    fun retry() {
        viewModelScope.launch {
            _isLoading.value = true
            NotificationStore.reload()
        }
    }

    fun markRead(id: String) {
        NotificationStore.markRead(id)
    }

    fun markUnread(id: String) {
        NotificationStore.markUnread(id)
    }

    fun markAllRead() {
        NotificationStore.markAllRead()
    }

    fun delete(id: String) {
        NotificationStore.delete(id)
    }

    fun clearAll() {
        NotificationStore.clear()
    }

    fun onNotificationTap(notification: KivoNotification) {
        if (!notification.isRead) NotificationStore.markRead(notification.id)
    }
}