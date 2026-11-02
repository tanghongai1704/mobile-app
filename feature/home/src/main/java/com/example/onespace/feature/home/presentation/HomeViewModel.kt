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
    observeTotalPoints: ObserveTotalPoints,
    observeStreak: ObserveStreak,
    observeDailyMissions: ObserveDailyMissions,
    ensureTodayMissions: EnsureTodayMissions,
    preferences: com.example.onespace.domain.session.repository.SessionPreferencesRepository,
    private val viewingSession: com.example.onespace.domain.video.repository.ViewingSessionRepository
) : ViewModel() {
    /** F-VID-02 flavor on the Home feed: topic picks first when topics exist. */
    private val topics = preferences.preferences.map { it.selectedTopics }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val uiState: StateFlow<HomeUiState> = combine(
        getVideo(), getMusic(), getNews(), topics
    ) { videos, tracks, articles, topics ->
        HomeUiState(
            videos = if (topics.isEmpty()) videos
            else videos.sortedByDescending { it.topic in topics },
            tracks = tracks,
            articles = articles
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

    /** Remove a single video from the watch later list (swipe/X on the card). */
    fun removeFromWatchLater(videoId: String) {
        viewModelScope.launch { viewingSession.removeFromWatchlist(videoId) }
    }

    /** Watching a video from the list means it's no longer "watch later". */
    fun watchedFromLater(videoId: String) {
        viewModelScope.launch { viewingSession.removeFromWatchlist(videoId) }
    }

    /** Demo switch from Settings: renders the offline banner while on. */
    val offline = preferences.preferences.map { it.simulateOffline }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}
