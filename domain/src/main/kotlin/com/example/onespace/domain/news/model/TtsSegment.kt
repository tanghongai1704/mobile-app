package com.example.onespace.domain.news.model

/** A paragraph-level chunk for TTS to read aloud. */
data class TtsSegment(
    val index: Int,
    val text: String
)

data class TtsState(
    val isPlaying: Boolean = false,
    val isReady: Boolean = false,
    val currentSegment: Int = 0,
    val totalSegments: Int = 0,
    val error: String? = null
) {
    val progress: Float get() = if (totalSegments == 0) 0f else currentSegment.toFloat() / totalSegments
}
