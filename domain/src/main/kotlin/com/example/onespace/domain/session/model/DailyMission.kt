package com.example.onespace.domain.session.model

enum class MissionType { LISTEN, READ, PLAY }

data class DailyMission(
    val id: String,
    val title: String,
    val targetType: MissionType,
    val target: Int = 1,
    val progress: Int = 0,
    val completed: Boolean = false,
    val date: String
) {
    val progressText: String get() = if (completed) "Hoàn thành" else "$progress/$target"
}
