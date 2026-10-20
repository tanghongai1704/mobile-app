package com.example.onespace.core.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.onespace.domain.session.Section
import com.example.onespace.domain.session.SessionPolicy
import com.example.onespace.domain.session.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** App-scoped UI session adapter. Real media ownership will belong to controller/service.
 * Features receive the SAME instance from the shell, never create their own coordinator.
 */
@HiltViewModel
class SessionCoordinator @Inject constructor(private val savedState: SavedStateHandle) : ViewModel() {
    private val _uiState = MutableStateFlow(SessionState(
        videoId = savedState["videoId"], videoTitle = savedState["videoTitle"],
        trackId = savedState["trackId"], trackTitle = savedState["trackTitle"]
    ))
    val uiState = _uiState.asStateFlow()

    fun enter(section: Section) = update(SessionPolicy.enter(_uiState.value, section))
    fun playVideo(id: String, title: String = id) = update(SessionPolicy.playVideo(_uiState.value, id, title))
    fun playMusic(id: String, title: String = id) = update(SessionPolicy.playMusic(_uiState.value, id, title))
    fun toggleVideo() = update(SessionPolicy.toggleVideo(_uiState.value))
    fun toggleMusic() = update(SessionPolicy.toggleMusic(_uiState.value))
    fun closeVideo() = update(SessionPolicy.closeVideo(_uiState.value))
    fun closeMusic() = update(SessionPolicy.closeMusic(_uiState.value))
    fun pauseGame() = update(_uiState.value.copy(gamePaused = !_uiState.value.gamePaused))
    fun resumeGame() = update(_uiState.value.copy(gamePaused = false))
    fun background() = update(SessionPolicy.background(_uiState.value))
    private fun update(next: SessionState) {
        _uiState.value = next
        savedState["videoId"] = next.videoId
        savedState["videoTitle"] = next.videoTitle
        savedState["trackId"] = next.trackId
        savedState["trackTitle"] = next.trackTitle
    }
}
