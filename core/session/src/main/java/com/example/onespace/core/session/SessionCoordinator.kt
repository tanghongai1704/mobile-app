package com.example.onespace.core.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onespace.domain.music.model.Track
import com.example.onespace.domain.music.usecase.SelectNextTrack
import com.example.onespace.domain.music.usecase.SelectPreviousTrack
import com.example.onespace.domain.music.usecase.GetMusic
import com.example.onespace.domain.session.MediaConflictRule
import com.example.onespace.domain.session.MusicStartPlan
import com.example.onespace.domain.session.Section
import com.example.onespace.domain.session.SessionPolicy
import com.example.onespace.domain.session.SessionState
import com.example.onespace.domain.session.port.PlaybackStatus
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Activity-scoped presentation adapter (Section 4 ADR-07): the single place that wires user intents
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

    /** Engine playback truths for dock, floating surface, slider and transport UI. */
    val music: StateFlow<MusicPlayback> = port.music
    val video: StateFlow<VideoPlayback> = port.video

    /** First-time video+music collision; the shell renders it as the C1 dialog. */
    private val _conflict = MutableStateFlow<MusicConflictRequest?>(null)
    val conflict: StateFlow<MusicConflictRequest?> = _conflict.asStateFlow()

    /** One-shot restored tab after a cold start (TC10 restore chain). */
    private val _restoredSection = MutableStateFlow<Section?>(null)
    val restoredSection: StateFlow<Section?> = _restoredSection.asStateFlow()
    private var restoreAllowed = true
    private var requestVersion = 0L

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
        val version = newPlaybackRequest()
        viewModelScope.launch {
            val video = getVideo().firstOrNull()?.firstOrNull { it.id == id } ?: return@launch
            val resumeFrom = viewingSession.progressOf(id)
            if (version == requestVersion) {
                port.openVideo(video, resumeFrom, SessionPolicy.resolveVideoStart(muted = false))
            }
        }
    }

    fun playMusic(id: String, title: String = id) {
        val version = newPlaybackRequest()
        viewModelScope.launch {
            val queue = getMusic().firstOrNull().orEmpty()
            val track = queue.firstOrNull { it.id == id } ?: return@launch
            startMusic(track, queue, version)
        }
    }

    private fun newPlaybackRequest(): Long {
        cancelPendingRestore()
        _conflict.value = null
        return ++requestVersion
    }

    private suspend fun startMusic(track: Track, queue: List<Track>, version: Long) {
        val rule = preferences.preferences.first().conflictRule
        if (version != requestVersion) return
        applyMusicPlan(MusicConflictRequest(track, queue), SessionPolicy.resolveMusicStart(port.video.value.audible, rule))
    }

    private fun applyMusicPlan(request: MusicConflictRequest, plan: MusicStartPlan) {
        when (plan) {
            is MusicStartPlan.Start -> {
                port.requestMusic(request.track, request.queue, plan.videoEffect)
                viewModelScope.launch { preferences.setLastTrackId(request.track.id) }
            }
            MusicStartPlan.DoNotStart -> Unit
            MusicStartPlan.AskUser -> _conflict.value = request
        }
    }

    /** Re-evaluate against current playback; the video may have stopped while the dialog was open. */
    fun resolveMusicConflict(rule: MediaConflictRule) {
        val request = _conflict.value ?: return
        _conflict.value = null
        applyMusicPlan(request, SessionPolicy.resolveMusicChoice(port.video.value.audible, rule))
        viewModelScope.launch { preferences.setConflictRule(rule) }
    }

    fun dismissMusicConflict() { _conflict.value = null }

    /** Shell navigation has applied the restored tab; do not apply it twice. */
    fun consumeRestoredSection() { _restoredSection.value = null }

    /** A slow disk/catalog read must never navigate over a newer user action. */
    fun cancelPendingRestore() {
        restoreAllowed = false
        _restoredSection.value = null
    }

    fun toggleVideo() {
        newPlaybackRequest()
        val playback = port.video.value
        if (playback.videoId == null) return
        if (playback.playRequested && playback.error == null && playback.status != PlaybackStatus.ENDED) {
            port.pauseVideo()
        } else {
            port.resumeVideo(SessionPolicy.resolveVideoStart(playback.muted))
        }
    }

    fun toggleMusic() {
        val playback = port.music.value
        if (playback.playRequested && playback.error == null && playback.status != PlaybackStatus.ENDED) {
            newPlaybackRequest()
            port.pauseMusic()
        } else {
            playback.queue.firstOrNull { it.id == playback.trackId }?.let { requestQueuedTrack(it, playback.queue) }
        }
    }

    private fun requestQueuedTrack(track: Track, queue: List<Track>) {
        val version = newPlaybackRequest()
        viewModelScope.launch { startMusic(track, queue, version) }
    }

    fun nextTrack() {
        val playback = port.music.value
        SelectNextTrack(playback.queue, playback.trackId)?.let { requestQueuedTrack(it, playback.queue) }
    }

    fun previousTrack() {
        val playback = port.music.value
        SelectPreviousTrack(playback.queue, playback.trackId)?.let { requestQueuedTrack(it, playback.queue) }
    }

    fun seekVideoBy(deltaMs: Long) = port.seekVideoBy(deltaMs)
    fun seekMusicTo(positionMs: Long) = port.seekMusicTo(positionMs)

    /** Floating window dismissal (case C3): stop and keep the video for later. */
    fun closeVideo() {
        newPlaybackRequest()
        port.dismissVideo(toWatchlist = true)
    }
    fun closeMusic() {
        newPlaybackRequest()
        port.stopMusic()
        viewModelScope.launch { preferences.setLastTrackId(null) }
    }

    fun pauseGame() = update(_uiState.value.copy(gamePaused = !_uiState.value.gamePaused))
    fun resumeGame() = update(_uiState.value.copy(gamePaused = false))

    /** ADR-12: leaving the app pauses video and the game loop; dock music keeps playing. */
    fun background() {
        newPlaybackRequest()
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
            _uiState.value.copy(trackId = null, trackTitle = null)
        } else {
            _uiState.value.copy(
                trackId = playback.trackId,
                trackTitle = playback.title.ifEmpty { _uiState.value.trackTitle ?: "" }
            )
        }
        update(next)
        if (playback.trackId != null && playback.trackId != previousId) {
            viewModelScope.launch { preferences.setLastTrackId(playback.trackId) }
        }
    }

    private fun updateVideo(playback: VideoPlayback) {
        val next = if (playback.videoId == null) {
            _uiState.value.copy(videoId = null, videoTitle = null)
        } else {
            _uiState.value.copy(
                videoId = playback.videoId,
                videoTitle = playback.title.ifEmpty { _uiState.value.videoTitle ?: "" }
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
