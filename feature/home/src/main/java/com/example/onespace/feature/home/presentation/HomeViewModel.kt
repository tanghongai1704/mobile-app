package com.example.onespace.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onespace.domain.music.usecase.GetMusic
import com.example.onespace.domain.news.usecase.GetNews
import com.example.onespace.domain.session.usecase.EnsureTodayMissions
import com.example.onespace.domain.session.usecase.ObserveDailyMissions
import com.example.onespace.domain.session.usecase.ObserveStreak
import com.example.onespace.domain.session.usecase.ObserveTotalPoints
import com.example.onespace.domain.video.usecase.GetVideo
import com.example.onespace.domain.video.usecase.ObserveContinueWatching
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    observeStreak: ObserveStreak,
    observeDailyMissions: ObserveDailyMissions,
    ensureTodayMissions: EnsureTodayMissions,
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

    private val today = LocalDate.now().toString()

    init {
        // F-COM-05: seed today's mission rows so the banner has live data.
        viewModelScope.launch { ensureTodayMissions(today) }
    }

    /** F-COM-05/06: real missions, streak and points from Room/DataStore. */
    val gamification: StateFlow<GamificationUiState> = combine(
        observeTotalPoints(), observeStreak(), observeDailyMissions(today)
    ) { points, streak, missions ->
        GamificationUiState(
            totalPoints = points,
            streak = streak.currentStreak,
            missions = missions.map {
                MissionUi(it.id, it.title, it.progress, it.target, it.completed)
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GamificationUiState())

    /** F-VID-04: the watchlist lives on Home, next to the global search. */
    val watchLater = viewingSession.observeWatchlist()
        .map { list -> list.map { it.videoId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** Demo switch from Settings: renders the offline banner while on. */
    val offline = preferences.preferences.map { it.simulateOffline }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}
