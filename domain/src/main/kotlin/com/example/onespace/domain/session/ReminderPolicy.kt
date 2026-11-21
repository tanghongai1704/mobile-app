package com.example.onespace.domain.session

import com.example.onespace.domain.session.model.DailyMission

/** Which daily reminder to show, with the missions still open (shown by name in the notification). */
sealed interface Reminder {
    val openMissions: List<DailyMission>

    /** Morning nudge (from 9:00): today's missions are not all done yet. */
    data class Missions(override val openMissions: List<DailyMission>) : Reminder

    /** Evening nudge (from 20:00): nothing done today, so the streak is at risk. */
    data class Streak(override val openMissions: List<DailyMission>) : Reminder
}

/**
 * F-COM-12 / US-COM-09 rules, pure Kotlin so they are unit-tested without Android:
 * each kind fires at most once per day, nothing is shown during quiet hours (22:00-7:00),
 * and every switch in Settings is respected.
 */
object ReminderPolicy {
    const val MORNING_HOUR = 9
    const val EVENING_HOUR = 20
    private const val QUIET_START = 22
    private const val QUIET_END = 7

    fun decide(
        hour: Int,
        today: String,
        notificationsEnabled: Boolean,
        streakReminder: Boolean,
        quietHours: Boolean,
        missions: List<DailyMission>,
        lastMissionsReminder: String?,
        lastStreakReminder: String?
    ): Reminder? {
        if (!notificationsEnabled) return null
        if (quietHours && (hour >= QUIET_START || hour < QUIET_END)) return null
        val todays = missions.filter { it.date == today }
        val open = todays.filter { !it.completed }
        if (todays.isEmpty() || open.isEmpty()) return null
        val nothingDone = todays.none { it.completed || it.progress > 0 }
        return when {
            hour >= EVENING_HOUR && streakReminder && nothingDone && lastStreakReminder != today ->
                Reminder.Streak(open)
            hour >= MORNING_HOUR && hour < EVENING_HOUR && lastMissionsReminder != today ->
                Reminder.Missions(open)
            else -> null
        }
    }
}
