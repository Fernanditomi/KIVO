package com.example.kivo.data.notifications

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationStore {

    private const val PREFS = "kivo_notifications"
    private const val KEY_LIST = "notifications_json"
    private const val MAX_STORED = 1000

    private val gson = Gson()
    private val listType = object : TypeToken<List<KivoNotification>>() {}.type

    private lateinit var appContext: Context

    private val _notifications = MutableStateFlow<List<KivoNotification>>(emptyList())
    val notifications: StateFlow<List<KivoNotification>> = _notifications.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        reload()
    }

    fun isInitialized(): Boolean = ::appContext.isInitialized

    fun context(): Context = appContext

    fun reload() {
        val json = prefs().getString(KEY_LIST, null)
        if (json.isNullOrBlank()) {
            _notifications.value = emptyList()
            return
        }
        try {
            val parsed = gson.fromJson<List<KivoNotification>>(json, listType) ?: emptyList()
            _notifications.value = parsed.sortedByDescending { it.timestamp }.take(MAX_STORED)
        } catch (e: Exception) {
            prefs().edit().remove(KEY_LIST).apply()
            _notifications.value = emptyList()
        }
    }

    private fun prefs() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun persist(list: List<KivoNotification>) {
        val trimmed = list.sortedByDescending { it.timestamp }.take(MAX_STORED)
        prefs().edit().putString(KEY_LIST, gson.toJson(trimmed)).apply()
    }

    fun unreadCount(): Int = _notifications.value.count { !it.isRead }

    fun add(notification: KivoNotification) {
        val updated = (listOf(notification) + _notifications.value).sortedByDescending { it.timestamp }
        _notifications.value = updated.take(MAX_STORED)
        persist(updated)
    }

    fun markRead(id: String) {
        val updated = _notifications.value.map {
            if (it.id == id && !it.isRead) it.copy(isRead = true) else it
        }
        _notifications.value = updated
        persist(updated)
    }

    fun markUnread(id: String) {
        val updated = _notifications.value.map {
            if (it.id == id && it.isRead) it.copy(isRead = false) else it
        }
        _notifications.value = updated
        persist(updated)
    }

    fun markAllRead() {
        val updated = _notifications.value.map {
            if (it.isRead) it else it.copy(isRead = true)
        }
        _notifications.value = updated
        persist(updated)
    }

    fun delete(id: String) {
        val updated = _notifications.value.filterNot { it.id == id }
        _notifications.value = updated
        persistWithRemoval(updated)
    }

    fun clear() {
        _notifications.value = emptyList()
        prefs().edit().remove(KEY_LIST).apply()
    }

    private fun persistWithRemoval(list: List<KivoNotification>) {
        if (list.isEmpty()) {
            prefs().edit().remove(KEY_LIST).apply()
        } else {
            persist(list)
        }
    }
}