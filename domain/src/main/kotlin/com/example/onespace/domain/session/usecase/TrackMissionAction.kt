package com.example.onespace.domain.session.usecase

import com.example.onespace.domain.session.model.DailyMission
import com.example.onespace.domain.session.model.MissionType
import com.example.onespace.domain.session.repository.MissionRepository
import com.example.onespace.domain.session.repository.PetRepository
import com.example.onespace.domain.session.repository.PointRepository
import kotlinx.coroutines.flow.first

class TrackMissionAction(
    private val missionRepository: MissionRepository,
    private val pointRepository: PointRepository,
    private val petRepository: PetRepository
) {
    /** Record a user action (listen/read/play) and update mission progress + award XP. */
    suspend operator fun invoke(type: MissionType) {
        val updated = missionRepository.recordAction(type) ?: return
        if (updated.completed && updated.progress == updated.target) {
            pointRepository.add("mission_${updated.id}", MISSION_REWARD)
            // F-COM-07: completed missions feed the pet (3 completed hatch the egg).
            val completed = missionRepository.missionsFor(updated.date).first().count { it.completed }
            petRepository.onMissionsCompleted(completed)
        }
    }

    companion object {
        const val MISSION_REWARD = 20
    }
}

class ObserveDailyMissions(private val repository: MissionRepository) {
    operator fun invoke(date: String) = repository.missionsFor(date)
}

/** F-COM-05: seed today's mission rows once per day. */
class EnsureTodayMissions(private val repository: MissionRepository) {
    suspend operator fun invoke(date: String) = repository.ensureTodayMissions(date)
}
