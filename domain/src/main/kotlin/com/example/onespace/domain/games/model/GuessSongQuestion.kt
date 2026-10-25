package com.example.onespace.domain.games.model

data class GuessSongQuestion(
    val trackId: String,
    val trackTitle: String,
    val artist: String,
    val options: List<String>
) {
    init {
        require(trackTitle in options) { "Correct answer must be in options" }
        require(options.size >= 3) { "Need at least 3 options" }
    }
}

data class GuessSongState(
    val question: GuessSongQuestion? = null,
    val isPlaying: Boolean = false,
    val hasAnswered: Boolean = false,
    val selectedOption: String? = null,
    val isCorrect: Boolean? = null,
    val score: Int = 0,
    val round: Int = 0
)
