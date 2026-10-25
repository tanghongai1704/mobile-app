package com.example.onespace.domain.session.repository

import com.example.onespace.domain.session.model.PetState
import kotlinx.coroutines.flow.Flow

interface PetRepository {
    val petState: Flow<PetState>
    suspend fun onMissionsCompleted(count: Int)
    suspend fun canFeedToday(): Boolean
    suspend fun recordFeed()
}
