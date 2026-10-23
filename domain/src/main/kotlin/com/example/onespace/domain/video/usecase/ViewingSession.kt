package com.example.onespace.domain.video.usecase

import com.example.onespace.domain.video.repository.ContinueEntry
import com.example.onespace.domain.video.repository.ViewingSessionRepository
import kotlinx.coroutines.flow.Flow

/** Items for the "Continue watching" row on the video feed. */
class ObserveContinueWatching(private val repository: ViewingSessionRepository) {
    operator fun invoke(): Flow<List<ContinueEntry>> = repository.observeContinue()
}

/** Persist the playback position so the video can resume later (F-VID-05, TC10). */
class SaveVideoProgress(private val repository: ViewingSessionRepository) {
    suspend operator fun invoke(videoId: String, positionMs: Long) =
        repository.saveProgress(videoId, positionMs)
}

/** Put a video aside for later, e.g. when the floating window is dismissed (case C3). */
class WatchLater(private val repository: ViewingSessionRepository) {
    suspend operator fun invoke(videoId: String, title: String) =
        repository.addToWatchlist(videoId, title)
}
