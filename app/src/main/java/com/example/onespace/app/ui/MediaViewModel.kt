package com.example.onespace.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.example.onespace.core.media.MediaPlaybackCoordinator
import com.example.onespace.domain.session.repository.SessionPreferences
import com.example.onespace.domain.session.repository.SessionPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Shell-level bridge: the shared video player handle + user preferences (theme etc). */
@HiltViewModel
class MediaViewModel @Inject constructor(
    private val engine: MediaPlaybackCoordinator,
    preferences: SessionPreferencesRepository
) : ViewModel() {
    /** null until the first real DataStore emission — the onboarding gate must not act on defaults. */
    val preferences = preferences.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val videoPlayer: Player get() = engine.videoPlayer
}
