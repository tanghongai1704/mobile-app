package com.example.onespace.domain.news.usecase

import com.example.onespace.domain.session.repository.PointRepository

class SubmitQuizAnswer(private val pointRepository: PointRepository) {
    /** Award points for a correct quiz answer: +15 per question. */
    suspend operator fun invoke(isCorrect: Boolean) {
        if (isCorrect) pointRepository.add("quiz_answer", POINTS_PER_CORRECT)
    }

    companion object { const val POINTS_PER_CORRECT = 15 }
}
