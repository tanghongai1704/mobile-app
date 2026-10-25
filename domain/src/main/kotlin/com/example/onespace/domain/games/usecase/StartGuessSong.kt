package com.example.onespace.domain.games.usecase

import com.example.onespace.domain.games.model.GuessSongQuestion
import com.example.onespace.domain.music.model.Track
import com.example.onespace.domain.session.repository.PointRepository
import kotlin.random.Random

class StartGuessSong(
    private val pointRepository: PointRepository
) {
    /** Generate a new question from the music catalog: pick 1 correct + 3 wrong titles. */
    suspend fun generate(tracks: List<Track>): GuessSongQuestion? {
        if (tracks.size < 4) return null
        val correct = tracks[Random.nextInt(tracks.size)]
        val wrongOptions = tracks.filter { it.id != correct.id }
            .shuffled()
            .take(3)
            .map { it.title }
        val options = (wrongOptions + correct.title).shuffled()
        return GuessSongQuestion(
            trackId = correct.id,
            trackTitle = correct.title,
            artist = correct.artist,
            options = options
        )
    }

    /** Check answer and award points if correct. */
    suspend fun answer(selected: String, question: GuessSongQuestion): Boolean {
        val isCorrect = selected == question.trackTitle
        if (isCorrect) pointRepository.add("guess_song", POINTS_PER_CORRECT)
        return isCorrect
    }

    companion object { const val POINTS_PER_CORRECT = 10 }
}
