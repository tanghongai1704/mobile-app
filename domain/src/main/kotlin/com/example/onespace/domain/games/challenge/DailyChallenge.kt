package com.example.onespace.domain.games.challenge

import java.time.LocalDate

/** How a game's reports add up towards the target. */
enum class ProgressMode {
    /** The best single value counts (a score in one round). */
    BEST,
    /** Each report adds to the total (wins, correct answers). */
    TOTAL
}

/** One kind of challenge; the day decides which template is offered. */
data class ChallengeTemplate(
    val gameId: String,
    val gameTitle: String,
    val goal: String,
    val target: Int,
    val mode: ProgressMode
)

data class DailyChallenge(
    val date: String,
    val template: ChallengeTemplate,
    val progress: Int = 0,
    val completed: Boolean = false,
    /** Points paid on completion; 0 until claimed. */
    val rewardPoints: Int = 0
) {
    val fraction: Float get() = if (template.target <= 0) 0f else (progress.toFloat() / template.target).coerceIn(0f, 1f)
}

object DailyChallenges {
    val pool = listOf(
        ChallengeTemplate("g1", "2048", "Đạt 300 điểm trong một ván 2048", 300, ProgressMode.BEST),
        ChallengeTemplate("g2", "Tic-Tac-Toe", "Thắng máy 2 ván Tic-Tac-Toe", 2, ProgressMode.TOTAL),
        ChallengeTemplate("g3", "Đoán bài hát", "Đoán đúng 3 bài hát", 3, ProgressMode.TOTAL),
        ChallengeTemplate("g4", "Quiz tin tức", "Trả lời đúng 3 câu hỏi tin tức", 3, ProgressMode.TOTAL),
        ChallengeTemplate("g5", "Rắn săn mồi", "Ăn 6 mồi trong một ván Rắn", 6, ProgressMode.BEST),
        ChallengeTemplate("g6", "Beat Tiles", "Đạt 10 điểm trong một ván Beat Tiles", 10, ProgressMode.BEST)
    )

    /** Same date always gives the same template; consecutive days give different games. */
    fun templateFor(date: String): ChallengeTemplate =
        pool[Math.floorMod(LocalDate.parse(date).toEpochDay(), pool.size.toLong()).toInt()]

    /** Applies a report from a game; progress never goes down. */
    fun apply(template: ChallengeTemplate, current: Int, reported: Int): Int = when (template.mode) {
        ProgressMode.BEST -> maxOf(current, reported)
        ProgressMode.TOTAL -> current + reported
    }
}

/** Reward for finishing a challenge: a flat part plus a bonus that grows with the day streak. */
object ChallengeReward {
    const val BASE_POINTS = 50
    private const val STREAK_STEP = 5
    private const val MAX_STREAK_DAYS = 7

    fun streakBonus(streakDays: Int): Int = STREAK_STEP * streakDays.coerceIn(0, MAX_STREAK_DAYS)
    fun points(streakDays: Int): Int = BASE_POINTS + streakBonus(streakDays)
}
