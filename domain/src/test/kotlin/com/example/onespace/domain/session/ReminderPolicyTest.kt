package com.example.onespace.domain.session

import com.example.onespace.domain.session.model.DailyMission
import com.example.onespace.domain.session.model.MissionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPolicyTest {
    private val today = "2026-11-21"
    private val open = listOf(
        DailyMission("listen", "Nghe 1 bài", MissionType.LISTEN, date = today),
        DailyMission("read", "Đọc 1 tin", MissionType.READ, date = today)
    )

    private fun decide(
        hour: Int,
        missions: List<DailyMission> = open,
        enabled: Boolean = true,
        streak: Boolean = true,
        quiet: Boolean = true,
        lastMissions: String? = null,
        lastStreak: String? = null
    ) = ReminderPolicy.decide(hour, today, enabled, streak, quiet, missions, lastMissions, lastStreak)

    @Test fun morningRemindsOnceADayWithOpenMissionNames() {
        val reminder = decide(hour = 9)
        assertTrue(reminder is Reminder.Missions)
        assertEquals(listOf("Nghe 1 bài", "Đọc 1 tin"), reminder!!.openMissions.map { it.title })
        assertNull(decide(hour = 10, lastMissions = today))
    }

    @Test fun eveningStreakOnlyWhenNothingDoneToday() {
        assertTrue(decide(hour = 20) is Reminder.Streak)
        val started = listOf(open[0].copy(progress = 1, completed = true), open[1])
        assertNull(decide(hour = 20, missions = started))
        assertNull(decide(hour = 21, lastStreak = today))
    }

    @Test fun switchesAndQuietHoursSilenceEverything() {
        assertNull(decide(hour = 9, enabled = false))
        assertNull(decide(hour = 20, streak = false, lastMissions = today))
        assertNull(decide(hour = 23))
        assertTrue(decide(hour = 23, quiet = false) is Reminder.Streak)
        assertNull(decide(hour = 8))
    }

    @Test fun allDoneMeansNoReminder() {
        assertNull(decide(hour = 12, missions = open.map { it.copy(progress = 1, completed = true) }))
    }
}
