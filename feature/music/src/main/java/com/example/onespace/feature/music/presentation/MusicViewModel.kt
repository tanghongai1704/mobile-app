package com.example.onespace.feature.music.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onespace.core.common.ContentUiState
import com.example.onespace.domain.music.usecase.GetMusic
import com.example.onespace.domain.music.usecase.SelectNextTrack
import com.example.onespace.domain.session.repository.SessionPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*

@HiltViewModel
internal class MusicViewModel @Inject constructor(
    getMusic: GetMusic,
    preferences: SessionPreferencesRepository
) : ViewModel() {
    private val refresh = MutableStateFlow(0)

    val uiState = combine(getMusic(), refresh) { items, _ -> ContentUiState(items = items) }
        .catch { emit(ContentUiState(error = "Không tải được kho nhạc.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContentUiState(isLoading = true))

    /** Demo switch from Settings: feeds render their offline state while on. */
    val offline = preferences.preferences.map { it.simulateOffline }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun retry() { refresh.value = refresh.value + 1 }

    fun nextTrack(currentId: String?) = SelectNextTrack(uiState.value.items, currentId)
}
