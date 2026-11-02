package com.example.onespace.domain.games.usecase

import com.example.onespace.domain.games.challenge.ChallengeReward
import com.example.onespace.domain.games.challenge.DailyChallenge
import com.example.onespace.domain.games.badge.RecordKeys
import com.example.onespace.domain.games.repository.ChallengeRepository
import com.example.onespace.domain.games.repository.GameRecordsRepository
import com.example.onespace.domain.session.repository.PetRepository
import com.example.onespace.domain.session.repository.PointRepository
import com.example.onespace.domain.session.repository.StreakRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.first

class ObserveDailyChallenge(private val repository: ChallengeRepository) {
    operator fun invoke(today: LocalDate = LocalDate.now()) = repository.challenge(today.toString())
}

/** What the player gets when a report completes the challenge. */
data class ChallengeCompletion(val challenge: DailyChallenge, val points: Int, val streakBonus: Int, val petTreat: Boolean)

/**
 * Games call this with their result. When the report completes today's challenge it pays
 * points (flat + streak bonus) and gives the pet a treat, exactly once per day.
 */
class ReportChallengeProgress(
    private val challenges: ChallengeRepository,
    private val points: PointRepository,
    private val streak: StreakRepository,
    private val pet: PetRepository,
    private val records: GameRecordsRepository
) {
    suspend operator fun invoke(gameId: String, value: Int, today: LocalDate = LocalDate.now()): ChallengeCompletion? {
        if (value <= 0) return null
        val done = challenges.report(today.toString(), gameId, value) ?: return null
        val days = streak.streak.first().currentStreak
        val reward = ChallengeReward.points(days)
        points.add("thu_thach_ngay", reward)
        pet.grantTreat()
        challenges.markRewarded(today.toString(), reward)
        records.increment(RecordKeys.CHALLENGES_DONE)
        return ChallengeCompletion(done.copy(rewardPoints = reward), reward, ChallengeReward.streakBonus(days), petTreat = true)
    }
}
