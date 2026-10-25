package com.example.onespace.domain.session.usecase

import com.example.onespace.domain.session.model.DailyMission
import com.example.onespace.domain.session.model.MissionType
import com.example.onespace.domain.session.repository.MissionRepository
import com.example.onespace.domain.session.repository.PointRepository

class TrackMissionAction(
    private val missionRepository: MissionRepository,
    private val pointRepository: PointRepository
) {
    /** Record a user action (listen/read/play) and update mission progress + award XP. */
    suspend operator fun invoke(type: MissionType) {
        val updated = missionRepository.recordAction(type) ?: return
        if (updated.completed && updated.progress == updated.target) {
            pointRepository.add("mission_${updated.id}", MISSION_REWARD)
        }
    }

    companion object {
        const val MISSION_REWARD = 20
    }
}

class ObserveDailyMissions(private val repository: MissionRepository) {
    operator fun invoke(date: String) = repository.missionsFor(date)
}
