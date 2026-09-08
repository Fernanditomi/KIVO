package com.example.kivo.data.models

data class Trend(
    val tag: String,
    val title: String,
    val comments: String
)

val dummyTrends = listOf(
    Trend("#DramaDelDía", "El artista que está rompiendo Internet", "24.8K"),
    Trend("#NuevaPareja", "¿Nueva pareja confirmada?", "15.2K"),
    Trend("#CanciónViral", "La canción que todos están escuchando", "10.1K"),
    Trend("#KivoGames", "El juego del momento que todos aman", "8.5K")
)
