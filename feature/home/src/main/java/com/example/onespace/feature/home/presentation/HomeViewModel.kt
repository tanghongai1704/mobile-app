package com.example.onespace.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onespace.domain.music.usecase.GetMusic
import com.example.onespace.domain.news.usecase.GetNews
import com.example.onespace.domain.session.usecase.ObserveTotalPoints
import com.example.onespace.domain.video.usecase.GetVideo
import com.example.onespace.domain.video.usecase.ObserveContinueWatching
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
internal class HomeViewModel @Inject constructor(
    getVideo: GetVideo,
    getMusic: GetMusic,
    getNews: GetNews,
    observeContinue: ObserveContinueWatching,
    observeTotalPoints: ObserveTotalPoints,
    preferences: com.example.onespace.domain.session.repository.SessionPreferencesRepository,
    viewingSession: com.example.onespace.domain.video.repository.ViewingSessionRepository
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(
        getVideo(), getMusic(), getNews(), observeContinue()
    ) { videos, tracks, articles, continueWatching ->
        HomeUiState(
            videos = videos,
            tracks = tracks,
            articles = articles,
            continueWatching = continueWatching
        )
    }.catch { emit(HomeUiState()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private val _gamification = MutableStateFlow(defaultMissions())
    val gamification: StateFlow<GamificationUiState> = _gamification.asStateFlow()

    init {
        viewModelScope.launch {
            observeTotalPoints().collect { points ->
                _gamification.value = _gamification.value.copy(totalPoints = points)
            }
        }
    }

    /** F-VID-04: the watchlist lives on Home, next to the global search. */
    val watchLater = viewingSession.observeWatchlist()
        .map { list -> list.map { it.videoId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** Demo switch from Settings: renders the offline banner while on. */
    val offline = preferences.preferences.map { it.simulateOffline }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private fun defaultMissions() = GamificationUiState(
        missions = listOf(
            MissionUi(id = "listen", title = "Nghe 1 bài nhạc"),
            MissionUi(id = "read", title = "Đọc 1 bài tin"),
            MissionUi(id = "play", title = "Thắng 1 ván game")
        )
    )
}
