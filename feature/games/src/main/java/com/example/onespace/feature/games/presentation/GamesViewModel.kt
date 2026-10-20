package com.example.onespace.feature.games.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onespace.core.common.ContentUiState
import com.example.onespace.domain.games.usecase.GetGames
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*

@HiltViewModel
internal class GamesViewModel @Inject constructor(getGames: GetGames) : ViewModel() {
    val uiState = getGames()
        .map { ContentUiState(items = it) }
        .catch { emit(ContentUiState(error = "Unable to load games")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContentUiState(isLoading = true))
}
