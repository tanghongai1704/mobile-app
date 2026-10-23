package com.example.onespace.domain.video.repository

import kotlinx.coroutines.flow.Flow

/** A video the user has started, with the last known playback position. */
data class ContinueEntry(val videoId: String, val title: String, val positionMs: Long)

/** Watchlist (F-VID-04) and resume positions (F-VID-05) — persisted viewing session. */
interface ViewingSessionRepository {
    fun observeContinue(): Flow<List<ContinueEntry>>
    suspend fun addToWatchlist(videoId: String, title: String)
    suspend fun removeFromWatchlist(videoId: String)
    suspend fun progressOf(videoId: String): Long
    suspend fun saveProgress(videoId: String, positionMs: Long)
}
