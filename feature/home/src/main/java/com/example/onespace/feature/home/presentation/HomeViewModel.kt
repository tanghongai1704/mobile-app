package com.example.onespace.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onespace.domain.music.usecase.GetMusic
import com.example.onespace.domain.news.usecase.GetNews
import com.example.onespace.domain.video.usecase.GetVideo
import com.example.onespace.domain.video.usecase.ObserveContinueWatching
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
internal class HomeViewModel @Inject constructor(
    getVideo: GetVideo,
    getMusic: GetMusic,
    getNews: GetNews,
    observeContinue: ObserveContinueWatching
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
}
