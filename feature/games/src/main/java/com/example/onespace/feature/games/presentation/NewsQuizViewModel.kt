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
    private val points: PointRepository
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
        viewModelScope.launch { points.add("quiz_tin_tuc", 15) }
    }
}

/** F-GAME-09: minigame wins add XP to the same wallet as quiz answers. */
@HiltViewModel
internal class PlayAwardViewModel @Inject constructor(
    private val points: PointRepository,
    private val trackMissionAction: TrackMissionAction,
    media: com.example.onespace.core.media.MediaPlaybackCoordinator
) : ViewModel() {
    /** F-GAME-08: the shared video player renders on top of the game board. */
    val player: androidx.media3.common.Player = media.videoPlayer

    /** A won game pays XP (F-GAME-09) and completes the PLAY mission (F-COM-05). */
    fun awardGame(source: String, amount: Int) {
        viewModelScope.launch {
            points.add(source, amount)
            trackMissionAction(MissionType.PLAY)
        }
    }
}
