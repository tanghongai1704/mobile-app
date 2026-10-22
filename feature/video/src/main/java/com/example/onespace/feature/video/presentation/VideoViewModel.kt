package com.example.onespace.feature.video.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.example.onespace.core.common.ContentUiState
import com.example.onespace.core.media.MediaPlaybackCoordinator
import com.example.onespace.domain.session.repository.SessionPreferencesRepository
import com.example.onespace.domain.video.repository.ContinueEntry
import com.example.onespace.domain.video.usecase.GetVideo
import com.example.onespace.domain.video.usecase.ObserveContinueWatching
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*

@HiltViewModel
internal class VideoViewModel @Inject constructor(
    getVideo: GetVideo,
    observeContinue: ObserveContinueWatching,
    private val engine: MediaPlaybackCoordinator,
    preferences: SessionPreferencesRepository
) : ViewModel() {
    private val refresh = MutableStateFlow(0)

    val uiState = combine(getVideo(), refresh) { items, _ -> ContentUiState(items = items) }
        .catch { emit(ContentUiState(error = "Không tải được danh sách video.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContentUiState(isLoading = true))

    val continueWatching = observeContinue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<ContinueEntry>())

    /** Demo switch from Settings: feeds render their offline state while on. */
    val offline = preferences.preferences.map { it.simulateOffline }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun retry() { refresh.value = refresh.value + 1 }

    /** The single shared video player instance (ADR-08: never recreated per screen). */
    val player: Player get() = engine.videoPlayer
}
