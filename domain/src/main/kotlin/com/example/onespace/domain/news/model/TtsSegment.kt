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
    /** Exact position inside the fallback audio stream, 0f..1f, when one is used. */
    val fraction: Float = 0f,
    /** The spoken volume, 0..1. */
    val volume: Float = 1f,
    val error: String? = null
) {
    val progress: Float get() = when {
        totalSegments == 0 -> 0f
        fraction > 0f -> fraction.coerceIn(0f, 1f)
        else -> currentSegment.toFloat() / totalSegments
    }
}
