package com.example.onespace.domain.news.repository

import com.example.onespace.domain.news.model.TtsSegment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface TtsEngine {
    val state: StateFlow<com.example.onespace.domain.news.model.TtsState>
    suspend fun prepare(segments: List<TtsSegment>)
    suspend fun play()
    suspend fun pause()
    suspend fun stop()
    suspend fun release()

    /**
     * Reads [paragraphs] into one audio file named after [name]. Returns where it is stored and
     * how long it is, or null when the device cannot do it (no engine or no Vietnamese voice).
     */
    suspend fun synthesizeToFile(paragraphs: List<String>, name: String): SynthesizedAudio? = null
}

/** A finished text-to-speech file: a playable [url] and its length. */
data class SynthesizedAudio(val url: String, val durationMs: Long)
