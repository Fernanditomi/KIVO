package com.example.kivo.ai.models

import com.example.kivo.data.models.Song

data class AiMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

enum class AiIntent {
    RECOMMEND_MUSIC,
    PLAY_SONG,
    SEARCH_ARTIST,
    SEARCH_SONG,
    CREATE_PLAYLIST,
    SHOW_FAVORITES,
    SHOW_HISTORY,
    SAVE_SONG,
    MUSIC_BY_MOOD,
    MUSIC_BY_GENRE,
    CURRENT_SONG,
    GENERAL_CHAT,
    UNKNOWN
}

data class AiResponse(
    val replyText: String,
    val intent: AiIntent = AiIntent.GENERAL_CHAT,
    val recommendedSong: Song? = null,
    val suggestedAction: String? = null
)
