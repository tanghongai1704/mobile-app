package com.example.onespace.domain.session.repository

import com.example.onespace.domain.session.model.PetState
import kotlinx.coroutines.flow.Flow

interface PetRepository {
    val petState: Flow<PetState>
    suspend fun onMissionsCompleted(count: Int)
    suspend fun canFeedToday(): Boolean
    suspend fun recordFeed()

    /** A daily-challenge reward: one free feed that ignores the once-a-day limit. */
    suspend fun grantTreat()

    /** Uses one treat; false when there is none. */
    suspend fun consumeTreat(): Boolean
}
