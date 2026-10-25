package com.example.onespace.domain.session.usecase

import com.example.onespace.domain.session.model.StreakState
import com.example.onespace.domain.session.repository.StreakRepository

class ObserveStreak(private val repository: StreakRepository) {
    operator fun invoke() = repository.streak
}

class UpdateStreak(private val repository: StreakRepository) {
    suspend operator fun invoke(today: String, yesterday: String) {
        repository.onMissionCompleted(today, yesterday)
    }
}
