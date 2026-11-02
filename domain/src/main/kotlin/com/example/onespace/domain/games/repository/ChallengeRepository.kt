package com.example.onespace.domain.games.repository

import com.example.onespace.domain.games.challenge.DailyChallenge
import kotlinx.coroutines.flow.Flow

interface ChallengeRepository {
    /** Today's challenge with persisted progress; a new day starts from zero. */
    fun challenge(date: String): Flow<DailyChallenge>

    /**
     * Adds a game report to [date]'s challenge. Returns the challenge when this report
     * just completed it (so the reward is paid once), otherwise null.
     */
    suspend fun report(date: String, gameId: String, value: Int): DailyChallenge?

    /** Remembers the points that were paid, for display. */
    suspend fun markRewarded(date: String, points: Int)
}
