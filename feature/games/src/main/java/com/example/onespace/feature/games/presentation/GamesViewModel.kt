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
    preferences: SessionPreferencesRepository,
    points: com.example.onespace.domain.session.repository.PointRepository
) : ViewModel() {
    private val refresh = MutableStateFlow(0)

    val uiState = combine(getGames(), refresh) { games, _ -> ContentUiState(items = games) }
        .catch { emit(ContentUiState(error = "Không tải được danh sách game.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContentUiState(isLoading = true))

    /** Demo switch from Settings: feeds render their offline state while on. */
    val offline = preferences.preferences.map { it.simulateOffline }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** F-GAME-07: the real point total anchors "Bạn" on the leaderboard. */
    val totalPoints = points.totalPoints
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val weeklyBoard = listOf(
        "Minh Anh" to 340, "Quốc Bảo" to 280, "Thu Hà" to 250, "Văn Long" to 190, "Ngọc Linh" to 150
    )
    val monthlyBoard = listOf(
        "Quốc Bảo" to 980, "Minh Anh" to 920, "Thu Hà" to 810, "Hải Đăng" to 600, "Ngọc Linh" to 540
    )

    fun retry() { refresh.value = refresh.value + 1 }
}
