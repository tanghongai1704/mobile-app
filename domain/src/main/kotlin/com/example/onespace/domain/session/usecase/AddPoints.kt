package com.example.onespace.domain.session.usecase

import com.example.onespace.domain.session.repository.PointRepository

class AddPoints(private val repository: PointRepository) {
    suspend operator fun invoke(source: String, amount: Int) {
        require(amount > 0) { "Point amount must be positive" }
        repository.add(source, amount)
    }
}

class ObserveTotalPoints(private val repository: PointRepository) {
    operator fun invoke() = repository.totalPoints
}

class SpendPoints(private val repository: PointRepository) {
    suspend operator fun invoke(amount: Int, description: String): Boolean {
        require(amount > 0) { "Spend amount must be positive" }
        return repository.spend(amount, description)
    }
}
