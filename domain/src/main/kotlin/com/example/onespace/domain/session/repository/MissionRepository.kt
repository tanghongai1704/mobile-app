package com.example.onespace.domain.session.repository

import com.example.onespace.domain.session.model.DailyMission
import com.example.onespace.domain.session.model.MissionType
import kotlinx.coroutines.flow.Flow

interface MissionRepository {
    fun missionsFor(date: String): Flow<List<DailyMission>>
    suspend fun recordAction(type: MissionType): DailyMission?
    suspend fun ensureTodayMissions(date: String)
}
