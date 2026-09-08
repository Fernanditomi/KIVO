package com.example.kivo.data.models

data class WhoSaidThatQuestion(
    val id: Int,
    val quote: String,
    val correctAnswer: String,
    val incorrectOptions: List<String>,
    val songTitle: String,
    val genre: String,
    val difficulty: String,
    val year: Int
) {
    fun getAllOptionsShuffled(): List<String> {
        return (incorrectOptions + correctAnswer).shuffled()
    }
}
