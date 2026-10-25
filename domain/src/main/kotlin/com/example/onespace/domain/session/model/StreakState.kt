package com.example.onespace.domain.session.model

data class StreakState(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastActiveDate: String? = null
) {
    val isActiveToday: Boolean get() = lastActiveDate != null
}

object StreakCalculator {
    /**
     * Update streak when user completes at least one mission today.
     * Consecutive days increment; a missed day resets to 0.
     */
    fun onMissionCompleted(current: StreakState, today: String, yesterday: String): StreakState {
        return when {
            current.lastActiveDate == today -> current // already counted today
            current.lastActiveDate == yesterday -> {
                val newStreak = current.currentStreak + 1
                current.copy(
                    currentStreak = newStreak,
                    bestStreak = maxOf(newStreak, current.bestStreak),
                    lastActiveDate = today
                )
            }
            else -> StreakState(currentStreak = 1, bestStreak = maxOf(1, current.bestStreak), lastActiveDate = today)
        }
    }
}
