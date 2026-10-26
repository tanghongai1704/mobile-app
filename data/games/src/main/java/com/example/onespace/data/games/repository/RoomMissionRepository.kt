package com.example.onespace.data.games.repository

import com.example.onespace.core.database.ContentDatabase
import com.example.onespace.core.database.DailyMissionEntity
import com.example.onespace.domain.session.model.DailyMission
import com.example.onespace.domain.session.model.MissionType
import com.example.onespace.domain.session.repository.MissionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomMissionRepository(
    private val database: ContentDatabase
) : MissionRepository {

    private val dao = database.dailyMissionDao()

    override fun missionsFor(date: String): Flow<List<DailyMission>> =
        dao.observeForDate(date).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun recordAction(type: MissionType): DailyMission? {
        val today = todayString()
        ensureTodayMissions(today)
        val target = dao.forDate(today).find { it.targetType == type.name && !it.completed }
            ?: return null
        val newProgress = target.progress + 1
        val completed = newProgress >= target.target
        dao.updateProgress(target.id, today, newProgress, completed)
        return DailyMission(
            id = target.id, title = target.title,
            targetType = MissionType.valueOf(target.targetType),
            target = target.target, progress = newProgress,
            completed = completed, date = today
        )
    }

    override suspend fun ensureTodayMissions(date: String) {
        // Room returns an empty list (never null) before the rows are seeded.
        if (dao.forDate(date).isNotEmpty()) return
        dao.upsertAll(defaultMissions(date))
    }

    private fun defaultMissions(date: String) = listOf(
        DailyMissionEntity(id = "listen", title = "Nghe 1 bài nhạc", targetType = "LISTEN", target = 1, date = date),
        DailyMissionEntity(id = "read", title = "Đọc 1 bài tin", targetType = "READ", target = 1, date = date),
        DailyMissionEntity(id = "play", title = "Thắng 1 ván game", targetType = "PLAY", target = 1, date = date)
    )

    private fun DailyMissionEntity.toDomain() = DailyMission(
        id = id, title = title,
        targetType = MissionType.valueOf(targetType),
        target = target, progress = progress,
        completed = completed, date = date
    )

    companion object {
        fun todayString(): String {
            return java.time.LocalDate.now().toString()
        }
    }
}

fun createMissionRepository(database: ContentDatabase): MissionRepository = RoomMissionRepository(database)
