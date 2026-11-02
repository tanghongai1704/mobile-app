package com.example.onespace.domain.games

import com.example.onespace.domain.games.challenge.ChallengeReward
import com.example.onespace.domain.games.challenge.DailyChallenge
import com.example.onespace.domain.games.challenge.DailyChallenges
import com.example.onespace.domain.games.challenge.ProgressMode
import com.example.onespace.domain.games.repository.ChallengeRepository
import com.example.onespace.domain.games.usecase.ReportChallengeProgress
import com.example.onespace.domain.session.model.PetState
import com.example.onespace.domain.session.model.StreakState
import com.example.onespace.domain.session.repository.PetRepository
import com.example.onespace.domain.session.repository.PointRepository
import com.example.onespace.domain.session.repository.StreakRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyChallengeTest {
    @Test fun sameDaySameChallengeConsecutiveDaysDiffer() {
        val d = LocalDate.of(2026, 11, 1)
        assertEquals(DailyChallenges.templateFor(d.toString()), DailyChallenges.templateFor(d.toString()))
        val week = (0 until DailyChallenges.pool.size).map { DailyChallenges.templateFor(d.plusDays(it.toLong()).toString()).gameId }
        assertEquals(DailyChallenges.pool.size, week.toSet().size)
        assertNotEquals(DailyChallenges.templateFor(d.toString()), DailyChallenges.templateFor(d.plusDays(1).toString()))
    }

    @Test fun bestAndTotalModesAccumulateDifferently() {
        val best = DailyChallenges.pool.first { it.mode == ProgressMode.BEST }
        val total = DailyChallenges.pool.first { it.mode == ProgressMode.TOTAL }
        assertEquals(40, DailyChallenges.apply(best, 40, 25))
        assertEquals(90, DailyChallenges.apply(best, 40, 90))
        assertEquals(3, DailyChallenges.apply(total, 2, 1))
    }

    @Test fun rewardGrowsWithStreakAndCaps() {
        assertEquals(50, ChallengeReward.points(0))
        assertEquals(65, ChallengeReward.points(3))
        assertEquals(85, ChallengeReward.points(7))
        assertEquals(85, ChallengeReward.points(30))
    }

    @Test fun completionPaysOnceWithStreakBonusAndPetTreat() = runBlocking {
        val date = LocalDate.of(2026, 11, 1)
        val template = DailyChallenges.templateFor(date.toString())
        val challenges = FakeChallenges(template.target)
        val points = FakePoints()
        val pet = FakePet()
        val records = FakeRecords()
        val use = ReportChallengeProgress(challenges, points, FakeStreak(4), pet, records)
        assertNull(use(template.gameId, 0, date))
        val first = use(template.gameId, template.target, date)
        assertNotNull(first)
        assertEquals(70, first!!.points)
        assertEquals(20, first.streakBonus)
        assertEquals(70, points.total)
        assertEquals(1, pet.treats)
        // Already completed: no second payout.
        assertNull(use(template.gameId, template.target, date))
        assertEquals(70, points.total)
        assertTrue(pet.treats == 1)
        assertEquals(1, records.counters["challenges_done"])
    }

    private class FakeRecords : com.example.onespace.domain.games.repository.GameRecordsRepository {
        val counters = mutableMapOf<String, Int>()
        override fun best(gameId: String) = flowOf(counters[gameId] ?: 0)
        override fun allBests() = flowOf(counters.toMap())
        override suspend fun submit(gameId: String, score: Int) = false
        override suspend fun increment(key: String): Int { counters[key] = (counters[key] ?: 0) + 1; return counters[key]!! }
    }

    private class FakeChallenges(private val target: Int) : ChallengeRepository {
        private var done = false
        override fun challenge(date: String): Flow<DailyChallenge> =
            flowOf(DailyChallenge(date, DailyChallenges.templateFor(date)))
        override suspend fun report(date: String, gameId: String, value: Int): DailyChallenge? {
            if (done || value < target) return null
            done = true
            return DailyChallenge(date, DailyChallenges.templateFor(date), value, completed = true)
        }
        override suspend fun markRewarded(date: String, points: Int) = Unit
    }

    private class FakePoints : PointRepository {
        var total = 0
        override val totalPoints = MutableStateFlow(0)
        override val recentEntries = flowOf(emptyList<com.example.onespace.domain.session.model.PointEntry>())
        override suspend fun add(source: String, amount: Int) { total += amount }
        override suspend fun spend(amount: Int, description: String) = false
    }

    private class FakeStreak(days: Int) : StreakRepository {
        override val streak = flowOf(StreakState(currentStreak = days))
        override suspend fun onMissionCompleted(today: String, yesterday: String) = Unit
    }

    private class FakePet : PetRepository {
        var treats = 0
        override val petState = flowOf(PetState())
        override suspend fun onMissionsCompleted(count: Int) = Unit
        override suspend fun canFeedToday() = true
        override suspend fun recordFeed() = Unit
        override suspend fun grantTreat() { treats += 1 }
        override suspend fun consumeTreat() = false
    }
}
