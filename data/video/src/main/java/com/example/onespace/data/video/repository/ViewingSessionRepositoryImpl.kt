package com.example.onespace.data.video.repository

import com.example.onespace.core.database.ContentDatabase
import com.example.onespace.core.database.VideoProgressEntity
import com.example.onespace.core.database.WatchlistEntity
import com.example.onespace.domain.video.repository.ContinueEntry
import com.example.onespace.domain.video.repository.ViewingSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Watchlist + resume positions on top of the shared Room database (ADR-09). */
internal class ViewingSessionRepositoryImpl(private val database: ContentDatabase) : ViewingSessionRepository {

    override fun observeContinue(): Flow<List<ContinueEntry>> =
        combine(
            database.watchlistDao().observeAll(),
            database.videoProgressDao().observeRecent()
        ) { watchlist, progress ->
            val positions = progress.associate { it.videoId to it.positionMs }
            (watchlist.map { it.videoId } + progress.map { it.videoId })
                .distinct()
                .map { videoId ->
                    ContinueEntry(
                        videoId = videoId,
                        title = watchlist.firstOrNull { it.videoId == videoId }?.title
                            ?: progress.firstOrNull { it.videoId == videoId }?.title.orEmpty(),
                        positionMs = positions[videoId] ?: 0
                    )
                }
        }

    override fun observeHistory(): Flow<List<ContinueEntry>> =
        database.videoProgressDao().observeRecent().map { list ->
            list.map { ContinueEntry(it.videoId, it.title, it.positionMs, it.updatedAt) }
        }

    override fun observeWatchlist(): Flow<List<ContinueEntry>> =
        database.watchlistDao().observeAll().map { list ->
            list.map { ContinueEntry(it.videoId, it.title, 0) }
        }

    override suspend fun addToWatchlist(videoId: String, title: String) =
        database.watchlistDao().upsert(WatchlistEntity(videoId, title, System.currentTimeMillis()))

    override suspend fun removeFromWatchlist(videoId: String) =
        database.watchlistDao().delete(videoId)

    override suspend fun progressOf(videoId: String): Long =
        database.videoProgressDao().positionOf(videoId) ?: 0

    override suspend fun saveProgress(videoId: String, positionMs: Long) {
        // Keep the last known title; the feed merges it with live catalog titles.
        val existing = database.videoProgressDao().find(videoId)
        database.videoProgressDao().upsert(
            VideoProgressEntity(
                videoId = videoId,
                title = existing?.title.orEmpty(),
                positionMs = positionMs,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
