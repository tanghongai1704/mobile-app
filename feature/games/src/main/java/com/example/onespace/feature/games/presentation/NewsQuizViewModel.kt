package com.example.onespace.feature.games.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onespace.domain.news.model.QuizQuestion
import com.example.onespace.domain.news.repository.NewsRepository
import com.example.onespace.domain.session.model.MissionType
import com.example.onespace.domain.session.repository.PointRepository
import com.example.onespace.domain.session.usecase.TrackMissionAction
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** F-GAME-03 / F-GAME-09: the news quiz game with per-answer XP. */
@HiltViewModel
internal class NewsQuizViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    private val points: PointRepository,
    private val challenge: ChallengeReporter
) : ViewModel() {
    private val _questions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val questions = _questions.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val articles = newsRepository.observeCatalog().first()
            _questions.value = articles.flatMap { it.quiz }.shuffled()
        }
    }

    /** F-GAME-09: a correct answer pays into the shared point wallet. */
    fun award() {
        viewModelScope.launch {
            points.add("quiz_tin_tuc", 15)
            challenge.report("g4", 1)
        }
    }
}

/** F-GAME-09: minigame wins add XP to the same wallet as quiz answers. */
@HiltViewModel
internal class PlayAwardViewModel @Inject constructor(
    private val points: PointRepository,
    private val trackMissionAction: TrackMissionAction,
    private val submitScore: com.example.onespace.domain.games.usecase.SubmitGameScore,
    private val challenge: ChallengeReporter,
    observeBests: com.example.onespace.domain.games.usecase.ObserveAllBestScores,
    media: com.example.onespace.core.media.MediaPlaybackCoordinator
) : ViewModel() {
    /** F-GAME-08: the shared video player renders on top of the game board. */
    val player: androidx.media3.common.Player = media.videoPlayer

    /**
     * Badges unlocked while this screen is open. The first emission of the records is the
     * baseline, so only badges earned from now on are announced.
     */
    val newBadges: kotlinx.coroutines.flow.Flow<List<com.example.onespace.domain.games.badge.GameBadge>> =
        kotlinx.coroutines.flow.flow {
            var previous: Map<String, Int>? = null
            observeBests().collect { now ->
                val before = previous
                previous = now
                if (before != null) {
                    val fresh = com.example.onespace.domain.games.badge.GameBadges.newlyEarned(before, now)
                    if (fresh.isNotEmpty()) emit(fresh)
                }
            }
        }

    /** Completions of the daily challenge, from any game. */
    val challengeCompletions = challenge.completions

    /** Keeps the personal best of a score-based game. */
    fun recordScore(gameId: String, score: Int) {
        viewModelScope.launch {
            submitScore(gameId, score)
            challenge.report(gameId, score)
        }
    }

    /** A won game pays XP (F-GAME-09) and completes the PLAY mission (F-COM-05). */
    fun awardGame(source: String, amount: Int) {
        viewModelScope.launch {
            points.add(source, amount)
            trackMissionAction(MissionType.PLAY)
        }
    }
}
