package com.example.kivo.data.notifications

enum class NotificationCategory(val label: String, val channelId: String) {
    MUSIC("Música", "KIVO_MUSIC"),
    GAMES("Juegos", "KIVO_GAMES"),
    SOCIAL("Social", "KIVO_SOCIAL"),
    SYSTEM("Sistema", "KIVO_GENERAL"),
    DOWNLOADS("Descargas", "KIVO_DOWNLOADS")
}

enum class NotificationPriority {
    LOW, DEFAULT, HIGH
}

enum class NotificationDestination {
    NOTIFICATIONS, PLAYER, MUSIC, GAMES, GAME, PLAYLIST, CHAT, PROFILE
}

object NotificationTypes {
    const val RECOMMENDATION = "recommendation"
    const val FAVORITE_ADDED = "favorite_added"
    const val PLAYLIST_UPDATED = "playlist_updated"
    const val PLAYLIST_AVAILABLE = "playlist_available"
    const val ARTIST_NEW_SONG = "artist_new_song"
    const val SONG_DOWNLOADED = "song_downloaded"
    const val SONG_ADDED_TO_PLAYLIST = "song_added_to_playlist"
    const val NEW_RECORD = "new_record"
    const val LEVEL_UNLOCKED = "level_unlocked"
    const val REWARD_OBTAINED = "reward_obtained"
    const val CHALLENGE_COMPLETED = "challenge_completed"
    const val ACHIEVEMENT_UNLOCKED = "achievement_unlocked"
    const val UPDATE_AVAILABLE = "update_available"
    const val MAINTENANCE = "maintenance"
    const val IMPORTANT_CHANGE = "important_change"
    const val NEW_FEATURE = "new_feature"
    const val ACTION_CONFIRMATION = "action_confirmation"
    const val WAITING_ROOM = "waiting_room"
    const val WELCOME = "welcome"
    const val CHAT_MESSAGE = "chat_message"
    const val SOCIAL_NEW_FOLLOWER = "new_follower"
    const val SOCIAL_LIKE = "like"
    const val SOCIAL_COMMENT = "comment"
    const val SOCIAL_SHARE = "share"
}

data class KivoNotification(
    val id: String,
    val category: NotificationCategory,
    val type: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean,
    val destination: NotificationDestination,
    val destinationId: String = "",
    val priority: NotificationPriority = NotificationPriority.DEFAULT
)