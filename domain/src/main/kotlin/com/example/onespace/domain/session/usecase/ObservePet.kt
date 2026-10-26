package com.example.onespace.domain.session.usecase

import com.example.onespace.domain.session.model.PetState
import com.example.onespace.domain.session.model.PetStage
import com.example.onespace.domain.session.repository.PetRepository
import com.example.onespace.domain.session.repository.PointRepository

class ObservePet(
    private val petRepository: PetRepository,
    private val pointRepository: PointRepository
) {
    operator fun invoke() = petRepository.petState
}

class FeedPet(
    private val petRepository: PetRepository,
    private val pointRepository: PointRepository
) {
    /** Feed the pet: costs 30 points, once per day. */
    suspend operator fun invoke(): FeedResult {
        if (!petRepository.canFeedToday()) return FeedResult.LIMIT_REACHED
        val spent = pointRepository.spend(FEED_COST, "feed_pet")
        if (!spent) return FeedResult.NOT_ENOUGH_POINTS
        petRepository.recordFeed()
        return FeedResult.SUCCESS
    }

    companion object { const val FEED_COST = 30 }
}

enum class FeedResult { SUCCESS, NOT_ENOUGH_POINTS, LIMIT_REACHED }
