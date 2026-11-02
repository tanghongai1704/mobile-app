package com.example.onespace.feature.games.presentation

import com.example.onespace.domain.games.usecase.ChallengeCompletion
import com.example.onespace.domain.games.usecase.ReportChallengeProgress
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Single entry point for games to feed the daily challenge. Completions are broadcast so
 * whichever screen is open can celebrate, no matter which game finished the challenge.
 */
@Singleton
internal class ChallengeReporter @Inject constructor(
    private val reportProgress: ReportChallengeProgress
) {
    private val _completions = MutableSharedFlow<ChallengeCompletion>(extraBufferCapacity = 4)
    val completions: SharedFlow<ChallengeCompletion> = _completions.asSharedFlow()

    suspend fun report(gameId: String, value: Int) {
        reportProgress(gameId, value)?.let { _completions.emit(it) }
    }
}
