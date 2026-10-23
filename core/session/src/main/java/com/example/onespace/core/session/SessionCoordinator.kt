package com.example.onespace.core.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onespace.domain.music.usecase.GetMusic
import com.example.onespace.domain.session.MediaConflictRule
import com.example.onespace.domain.session.MusicStartPlan
import com.example.onespace.domain.session.Section
import com.example.onespace.domain.session.SessionPolicy
import com.example.onespace.domain.session.SessionState
import com.example.onespace.domain.session.port.MediaSessionPort
import com.example.onespace.domain.session.port.MusicConflictRequest
import com.example.onespace.domain.session.port.MusicPlayback
import com.example.onespace.domain.session.port.VideoPlayback
import com.example.onespace.domain.session.repository.SessionPreferencesRepository
import com.example.onespace.domain.video.repository.ViewingSessionRepository
import com.example.onespace.domain.video.usecase.GetVideo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * App-scoped session adapter (ADR-11): the single place that wires user intents
 * to the shared media engine and keeps the UI session state in sync. Playback
 * truth (positions, buffering, auto-advance) always comes from the engine via
 * [port]; this coordinator owns identity, section switching and the case C1
 * ask-once audio-conflict flow.
 */
@HiltViewModel
class SessionCoordinator @Inject constructor(
    private val savedState: SavedStateHandle,
    private val port: MediaSessionPort,
    private val preferences: SessionPreferencesRepository,
    private val getMusic: GetMusic,
    private val getVideo: GetVideo,
    private val viewingSession: ViewingSessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(restoreFromSavedState())
    val uiState: StateFlow<SessionState> = _uiState.asStateFlow()

    /** Engine playback truths for dock, PIP, slider and transport UI. */
    val music: StateFlow<MusicPlayback> = port.music
    val video: StateFlow<VideoPlayback> = port.video

    /** First-time video+music collision; the shell renders it as the C1 dialog. */
    private val _conflict = MutableStateFlow<MusicConflictRequest?>(null)
    val conflict: StateFlow<MusicConflictRequest?> = _conflict.asStateFlow()

    /** One-shot restored tab after a cold start (TC10 restore chain). */
    private val _restoredSection = MutableStateFlow<Section?>(null)
    val restoredSection: StateFlow<Section?> = _restoredSection.asStateFlow()
    private var restoreAllowed = true

    init {
        viewModelScope.launch {
            port.music.collect { playback -> updateMusic(playback) }
        }
        viewModelScope.launch {
            port.video.collect { playback -> updateVideo(playback) }
        }
        viewModelScope.launch { restoreColdStartSession() }
    }

    fun enter(section: Section) {
        cancelPendingRestore()
        update(SessionPolicy.enter(_uiState.value, section))
        viewModelScope.launch { preferences.setLastSection(section) }
    }

    fun playVideo(id: String, title: String = id) {
        cancelPendingRestore()
        viewModelScope.launch {
            val video = getVideo().firstOrNull()?.firstOrNull { it.id == id } ?: return@launch
            val resumeFrom = viewingSession.progressOf(id)
            port.openVideo(video, resumeFrom)
        }
    }

    fun playMusic(id: String, title: String = id) {
        cancelPendingRestore()
        viewModelScope.launch {
            val queue = getMusic().firstOrNull().orEmpty()
            val track = queue.firstOrNull { it.id == id } ?: return@launch
            val rule = preferences.preferences.first().conflictRule
            when (val plan = SessionPolicy.resolveMusicStart(port.video.value.playing, rule)) {
                is MusicStartPlan.Start -> {
                    port.requestMusic(track, queue, plan.videoEffect)
                    preferences.setLastTrackId(track.id)
                }
                MusicStartPlan.DoNotStart -> Unit
                MusicStartPlan.AskUser -> _conflict.value = MusicConflictRequest(track, queue)
            }
        }
    }

    /** Shell reports the user's C1 choice; it is remembered for the pair from now on. */
    fun resolveMusicConflict(rule: MediaConflictRule) {
        val request = _conflict.value ?: return
        _conflict.value = null
        port.resolveMusicConflict(request.track, request.queue, rule)
        viewModelScope.launch {
            preferences.setConflictRule(rule)
            if (rule != MediaConflictRule.KEEP_VIDEO) preferences.setLastTrackId(request.track.id)
        }
    }

    fun dismissMusicConflict() { _conflict.value = null }

    /** Shell navigation has applied the restored tab; do not apply it twice. */
    fun consumeRestoredSection() { _restoredSection.value = null }

    /** A slow disk/catalog read must never navigate over a newer user action. */
    fun cancelPendingRestore() {
        restoreAllowed = false
        _restoredSection.value = null
    }

    fun toggleVideo() = port.toggleVideo()
    fun toggleMusic() {
        val playback = port.music.value
        if (playback.playing) port.toggleMusic()
        else playback.trackId?.let { playMusic(it) }
    }
    fun seekVideoBy(deltaMs: Long) = port.seekVideoBy(deltaMs)
    fun seekMusicTo(positionMs: Long) = port.seekMusicTo(positionMs)
    fun previousTrack() = port.skipToPrevious()

    /** Floating window dismissal (case C3): stop and keep the video for later. */
    fun closeVideo() = port.dismissVideo(toWatchlist = true)
    fun closeMusic() {
        cancelPendingRestore()
        port.stopMusic()
        viewModelScope.launch { preferences.setLastTrackId(null) }
    }

    fun pauseGame() = update(_uiState.value.copy(gamePaused = !_uiState.value.gamePaused))
    fun resumeGame() = update(_uiState.value.copy(gamePaused = false))

    /** ADR-12: leaving the app pauses video and the game loop; dock music keeps playing. */
    fun background() {
        port.pauseVideo()
        update(SessionPolicy.background(_uiState.value))
    }

    private suspend fun restoreColdStartSession() {
        // SavedStateHandle wins when the OS recreated this exact ViewModel.
        if (!restoreAllowed || savedState.get<String>("trackId") != null) return
        val prefs = preferences.preferences.first()
        if (!restoreAllowed) return
        if (prefs.lastTrackId != null && port.music.value.trackId == null) {
            val queue = getMusic().firstOrNull().orEmpty()
            if (!restoreAllowed) return
            queue.firstOrNull { it.id == prefs.lastTrackId }?.let {
                port.prepareRestoredMusic(it, queue)
            }
        }
        if (restoreAllowed && prefs.lastSection != Section.HOME) _restoredSection.value = prefs.lastSection
    }

    private fun updateMusic(playback: MusicPlayback) {
        val previousId = _uiState.value.trackId
        val next = if (playback.trackId == null) {
            _uiState.value.copy(trackId = null, trackTitle = null, musicPlaying = false)
        } else {
            _uiState.value.copy(
                trackId = playback.trackId,
                trackTitle = playback.title.ifEmpty { _uiState.value.trackTitle ?: "" },
                musicPlaying = playback.playing
            )
        }
        update(next)
        if (playback.trackId != null && playback.trackId != previousId) {
            viewModelScope.launch { preferences.setLastTrackId(playback.trackId) }
        }
    }

    private fun updateVideo(playback: VideoPlayback) {
        val next = if (playback.videoId == null) {
            _uiState.value.copy(videoId = null, videoTitle = null, videoPlaying = false)
        } else {
            _uiState.value.copy(
                videoId = playback.videoId,
                videoTitle = playback.title.ifEmpty { _uiState.value.videoTitle ?: "" },
                videoPlaying = playback.playing
            )
        }
        update(next)
    }

    private fun restoreFromSavedState(): SessionState = SessionState(
        videoId = savedState["videoId"], videoTitle = savedState["videoTitle"],
        trackId = savedState["trackId"], trackTitle = savedState["trackTitle"]
    )

    private fun update(next: SessionState) {
        _uiState.value = next
        savedState["videoId"] = next.videoId
        savedState["videoTitle"] = next.videoTitle
        savedState["trackId"] = next.trackId
        savedState["trackTitle"] = next.trackTitle
    }
}
