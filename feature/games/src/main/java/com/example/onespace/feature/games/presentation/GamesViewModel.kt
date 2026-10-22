package com.example.onespace.feature.games.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onespace.core.common.ContentUiState
import com.example.onespace.domain.games.usecase.GetGames
import com.example.onespace.domain.session.repository.SessionPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*

@HiltViewModel
internal class GamesViewModel @Inject constructor(
    getGames: GetGames,
    preferences: SessionPreferencesRepository
) : ViewModel() {
    private val refresh = MutableStateFlow(0)

    val uiState = combine(getGames(), refresh) { games, _ -> ContentUiState(items = games) }
        .catch { emit(ContentUiState(error = "Không tải được danh sách game.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContentUiState(isLoading = true))

    /** Demo switch from Settings: feeds render their offline state while on. */
    val offline = preferences.preferences.map { it.simulateOffline }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun retry() { refresh.value = refresh.value + 1 }
}
