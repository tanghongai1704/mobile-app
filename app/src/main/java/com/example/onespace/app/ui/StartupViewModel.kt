package com.example.onespace.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onespace.domain.auth.repository.AuthRepository
import com.example.onespace.domain.session.repository.SessionPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class StartupDestination { ONBOARDING, AUTH, MAIN }

sealed interface StartupUiState {
    data object Loading : StartupUiState
    data class Ready(val destination: StartupDestination) : StartupUiState
    data object Error : StartupUiState
}

/** Resolves persisted launch state before the main graph becomes visible. */
@HiltViewModel
class StartupViewModel @Inject constructor(
    private val preferencesRepository: SessionPreferencesRepository,
    private val auth: AuthRepository
) : ViewModel() {
    val preferences = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val authSession = auth.session
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _uiState = MutableStateFlow<StartupUiState>(StartupUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init { load() }

    fun load() {
        _uiState.value = StartupUiState.Loading
        viewModelScope.launch {
            try {
                val prefs = preferencesRepository.preferences.first()
                val session = auth.session.first()
                _uiState.value = StartupUiState.Ready(when {
                    !prefs.onboarded -> StartupDestination.ONBOARDING
                    session.hasAccess -> StartupDestination.MAIN
                    else -> StartupDestination.AUTH
                })
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.value = StartupUiState.Error }
        }
    }
}
