package com.example.kivo.data.local

import android.content.Context

object ChatSettingsStore {
    private lateinit var appContext: Context
    private const val PREFS = "kivo_chat_settings"
    private const val KEY_MUTED = "muted_conversations"
    private const val KEY_BLOCKED = "blocked_users"

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun prefs() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun getSet(key: String): Set<String> = prefs().getStringSet(key, emptySet()) ?: emptySet()

    private fun updateSet(key: String, block: (MutableSet<String>) -> Unit) {
        val set = prefs().getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()
        block(set)
        prefs().edit().putStringSet(key, set).apply()
    }

    fun isConversationMuted(conversationId: String): Boolean = conversationId in getSet(KEY_MUTED)

    fun setConversationMuted(conversationId: String, muted: Boolean) = updateSet(KEY_MUTED) {
        if (muted) it.add(conversationId) else it.remove(conversationId)
    }

    fun isUserBlocked(userId: String): Boolean = userId in getSet(KEY_BLOCKED)

    fun setUserBlocked(userId: String, blocked: Boolean) = updateSet(KEY_BLOCKED) {
        if (blocked) it.add(userId) else it.remove(userId)
    }
}