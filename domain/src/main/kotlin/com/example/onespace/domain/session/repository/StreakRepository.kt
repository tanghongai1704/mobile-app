package com.example.onespace.domain.session.repository

import com.example.onespace.domain.session.model.StreakState
import kotlinx.coroutines.flow.Flow

interface StreakRepository {
    val streak: Flow<StreakState>
    suspend fun onMissionCompleted(today: String, yesterday: String)
}
