package com.example.onespace.feature.account.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onespace.domain.session.repository.Language
import com.example.onespace.domain.session.repository.SessionPreferences
import com.example.onespace.domain.session.repository.SessionPreferencesRepository
import com.example.onespace.domain.session.repository.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val preferences: SessionPreferencesRepository
) : ViewModel() {
    val uiState: StateFlow<SessionPreferences> = preferences.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionPreferences())

    fun setThemeMode(mode: ThemeMode) { viewModelScope.launch { preferences.setThemeMode(mode) } }
    fun setDataSaver(enabled: Boolean) { viewModelScope.launch { preferences.setDataSaver(enabled) } }
    fun setLanguage(language: Language) { viewModelScope.launch { preferences.setLanguage(language) } }
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setNotificationsEnabled(enabled) }
    }
    fun setSimulateOffline(enabled: Boolean) {
        viewModelScope.launch { preferences.setSimulateOffline(enabled) }
    }

    /** C1 rules can be changed in Settings per the MVP decision list. */
    fun resetConflictRule() { viewModelScope.launch { preferences.setConflictRule(null) } }

    /** TC08: leave the account and return to the mock login screen. */
    fun exitAccount(onDone: () -> Unit) {
        viewModelScope.launch {
            preferences.setAuthEmail(null)
            preferences.setOnboarded(false)
            onDone()
        }
    }
}
