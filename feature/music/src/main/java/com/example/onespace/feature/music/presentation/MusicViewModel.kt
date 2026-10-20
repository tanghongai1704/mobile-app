package com.example.onespace.feature.music.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onespace.core.common.ContentUiState
import com.example.onespace.domain.music.usecase.GetMusic
import com.example.onespace.domain.music.usecase.SelectNextTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*

@HiltViewModel
internal class MusicViewModel @Inject constructor(getMusic: GetMusic) : ViewModel() {
    val uiState = getMusic()
        .map { ContentUiState(items = it) }
        .catch { emit(ContentUiState(error = "Unable to load music")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContentUiState(isLoading = true))

    fun nextTrack(currentId: String?) = SelectNextTrack(uiState.value.items, currentId)
}
