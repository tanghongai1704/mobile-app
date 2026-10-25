package com.example.onespace.domain.video.repository

import kotlinx.coroutines.flow.Flow

/** A video the user has started, with the last known playback position. */
data class ContinueEntry(val videoId: String, val title: String, val positionMs: Long, val updatedAt: Long = 0)

/** Watchlist (F-VID-04) and resume positions (F-VID-05) — persisted viewing session. */
interface ViewingSessionRepository {
    fun observeContinue(): Flow<List<ContinueEntry>>
    fun observeWatchlist(): Flow<List<ContinueEntry>>
    /** F-VID-05: videos the user actually started, newest first. */
    fun observeHistory(): Flow<List<ContinueEntry>>
    suspend fun addToWatchlist(videoId: String, title: String)
    suspend fun removeFromWatchlist(videoId: String)
    suspend fun progressOf(videoId: String): Long
    suspend fun saveProgress(videoId: String, positionMs: Long)
}
