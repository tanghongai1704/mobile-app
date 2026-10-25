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
}
