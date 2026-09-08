package com.example.kivo.data.notifications

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationSettingsStore {

    private const val PREFS = "kivo_notification_settings"

    data class NotificationSettings(
        val general: Boolean = true,
        val music: Boolean = true,
        val games: Boolean = true,
        val social: Boolean = true,
        val downloads: Boolean = true,
        val sound: Boolean = true,
        val vibration: Boolean = true,
        val lockscreenContent: Boolean = true
    )

    private val _values = MutableStateFlow(NotificationSettings())
    val values: StateFlow<NotificationSettings> = _values.asStateFlow()

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
        load()
    }

    fun load() {
        val p = prefs()
        _values.value = NotificationSettings(
            general = p.getBoolean("general", true),
            music = p.getBoolean("music", true),
            games = p.getBoolean("games", true),
            social = p.getBoolean("social", true),
            downloads = p.getBoolean("downloads", true),
            sound = p.getBoolean("sound", true),
            vibration = p.getBoolean("vibration", true),
            lockscreenContent = p.getBoolean("lockscreen_content", true)
        )
    }

    private fun prefs() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun save(key: String, value: Boolean, update: (NotificationSettings) -> NotificationSettings) {
        prefs().edit().putBoolean(key, value).apply()
        _values.value = update(_values.value)
    }

    fun setGeneral(enabled: Boolean) = save("general", enabled) { it.copy(general = enabled) }
    fun setMusic(enabled: Boolean) = save("music", enabled) { it.copy(music = enabled) }
    fun setGames(enabled: Boolean) = save("games", enabled) { it.copy(games = enabled) }
    fun setSocial(enabled: Boolean) = save("social", enabled) { it.copy(social = enabled) }
    fun setDownloads(enabled: Boolean) = save("downloads", enabled) { it.copy(downloads = enabled) }
    fun setSound(enabled: Boolean) = save("sound", enabled) { it.copy(sound = enabled) }
    fun setVibration(enabled: Boolean) = save("vibration", enabled) { it.copy(vibration = enabled) }
    fun setLockscreenContent(enabled: Boolean) = save("lockscreen_content", enabled) { it.copy(lockscreenContent = enabled) }

    fun isCategoryEnabled(category: NotificationCategory): Boolean {
        val s = _values.value
        return when (category) {
            NotificationCategory.MUSIC -> s.music
            NotificationCategory.GAMES -> s.games
            NotificationCategory.SOCIAL -> s.social
            NotificationCategory.DOWNLOADS -> s.downloads
            NotificationCategory.SYSTEM -> s.general
        }
    }
}