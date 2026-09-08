package com.example.kivo.ai.service

import com.example.kivo.ai.models.AiIntent
import com.example.kivo.ai.models.AiResponse
import com.example.kivo.data.models.kivoSongs
import kotlinx.coroutines.delay

interface KivoAiService {
    suspend fun processQuery(query: String, context: Map<String, Any?>): AiResponse
}

class MockKivoAiService : KivoAiService {
    override suspend fun processQuery(query: String, context: Map<String, Any?>): AiResponse {
        delay(600) // Fast response
        
        val q = query.lowercase().trim()
        
        // 1. Better Song Matching - Search title in query
        val foundSong = kivoSongs.find { song ->
            val title = song.title.lowercase()
            q.contains(title) 
        }
        
        if (foundSong != null && (q.contains("pon") || q.contains("reproduce") || q.contains("escucha") || q.length < 15)) {
            return AiResponse(
                replyText = "¡Claro! ' ${foundSong.title}' de ${foundSong.artist} a la orden. 🎧",
                intent = AiIntent.PLAY_SONG,
                recommendedSong = foundSong
            )
        }

        // 2. Artist Matching
        val foundByArtist = kivoSongs.filter { it.artist.lowercase().contains(q) || q.contains(it.artist.lowercase()) }
        if (foundByArtist.isNotEmpty() && (q.contains("pon") || q.contains("algo de") || q.contains("musica"))) {
            val song = foundByArtist.random()
            return AiResponse(
                replyText = "¡Excelente elección! Aquí tienes algo de ${song.artist}: '${song.title}'.",
                intent = AiIntent.SEARCH_ARTIST,
                recommendedSong = song
            )
        }

        // 3. Mood & Intent
        return when {
            q.contains("triste") || q.contains("sad") || q.contains("llorar") -> {
                val song = kivoSongs.filter { s -> listOf("coco", "lloro", "dancing", "glimpse", "someone").any { s.title.lowercase().contains(it) } }.randomOrNull() ?: kivoSongs.random()
                AiResponse(
                    replyText = "Entiendo... Aquí tienes algo para acompañar ese sentimiento: '${song.title}' de ${song.artist}. 😔",
                    intent = AiIntent.MUSIC_BY_MOOD,
                    recommendedSong = song
                )
            }
            q.contains("entrenar") || q.contains("gym") || q.contains("fiesta") || q.contains("energia") -> {
                val song = kivoSongs.filter { s -> listOf("monaco", "punto", "lokera", "party", "calle").any { s.title.lowercase().contains(it) } }.randomOrNull() ?: kivoSongs.random()
                AiResponse(
                    replyText = "¡A darle con todo! 🔥 Ponle energía con '${song.title}' de ${song.artist}.",
                    intent = AiIntent.MUSIC_BY_MOOD,
                    recommendedSong = song
                )
            }
            q.contains("guarda") || q.contains("favorito") || q.contains("me gusta") -> {
                AiResponse(
                    replyText = "¡Hecho! He guardado la canción actual en tus favoritos. ❤️",
                    intent = AiIntent.SAVE_SONG
                )
            }
            q.contains("recomienda") || q.contains("sorprende") || q.contains("que escucho") -> {
                val song = kivoSongs.random()
                AiResponse(
                    replyText = "Te recomiendo esta: '${song.title}' de ${song.artist}. ¡Dime si te gusta! 🔥",
                    intent = AiIntent.RECOMMEND_MUSIC,
                    recommendedSong = song
                )
            }
            else -> AiResponse(
                replyText = "Hola, soy KIVO AI. Puedo poner cualquier canción de tu lista (como 'Julietota' o 'MONACO'), recomendarte música según tu mood o guardar tus favoritas. ¿Qué quieres escuchar?",
                intent = AiIntent.GENERAL_CHAT
            )
        }
    }
}
