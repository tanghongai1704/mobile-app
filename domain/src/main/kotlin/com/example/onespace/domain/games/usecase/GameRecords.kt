package com.example.onespace.domain.games.usecase

import com.example.onespace.domain.games.repository.GameRecordsRepository
import kotlinx.coroutines.flow.map

class ObserveBestScore(private val repository: GameRecordsRepository) {
    operator fun invoke(gameId: String) = repository.best(gameId)
}

class ObserveAllBestScores(private val repository: GameRecordsRepository) {
    operator fun invoke() = repository.allBests()
}

class SubmitGameScore(private val repository: GameRecordsRepository) {
    suspend operator fun invoke(gameId: String, score: Int): Boolean =
        score > 0 && repository.submit(gameId, score)
}

class ObserveGameBadges(private val repository: GameRecordsRepository) {
    operator fun invoke() = repository.allBests().map { com.example.onespace.domain.games.badge.GameBadges.evaluate(it) }
}
