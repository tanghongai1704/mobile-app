package com.example.onespace.feature.video.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onespace.core.common.ContentUiState
import com.example.onespace.domain.video.usecase.GetVideo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*

@HiltViewModel
internal class VideoViewModel @Inject constructor(getVideo: GetVideo) : ViewModel() {
    val uiState = getVideo()
        .map { ContentUiState(items = it) }
        .catch { emit(ContentUiState(error = "Unable to load video")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContentUiState(isLoading = true))
}
