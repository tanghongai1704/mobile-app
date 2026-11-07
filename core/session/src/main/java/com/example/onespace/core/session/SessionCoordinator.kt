package com.example.onespace.core.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onespace.core.designsystem.AppPlaylistStore
import com.example.onespace.domain.music.model.Track
import com.example.onespace.domain.music.usecase.SelectNextTrack
import com.example.onespace.domain.music.usecase.SelectPreviousTrack
import com.example.onespace.domain.music.usecase.GetMusic
import com.example.onespace.domain.session.MediaConflictRule
import com.example.onespace.domain.session.MusicStartPlan
import com.example.onespace.domain.session.Section
import com.example.onespace.domain.session.SessionPolicy
import com.example.onespace.domain.session.SessionState
import com.example.onespace.domain.session.VideoAudioEffect
import com.example.onespace.domain.session.model.MissionType
import com.example.onespace.domain.session.port.PlaybackStatus
import com.example.onespace.domain.session.port.MediaSessionPort
import com.example.onespace.domain.session.VideoStartPlan
import com.example.onespace.domain.session.port.MusicConflictRequest
import com.example.onespace.domain.session.port.VideoConflictRequest
import com.example.onespace.domain.session.port.MusicPlayback
import com.example.onespace.domain.session.port.VideoPlayback
import com.example.onespace.domain.session.repository.SessionPreferencesRepository
import com.example.onespace.domain.session.usecase.TrackMissionAction
import com.example.onespace.domain.session.usecase.UpdateStreak
import com.example.onespace.domain.video.model.Video
import com.example.onespace.domain.video.repository.ViewingSessionRepository
import com.example.onespace.domain.video.usecase.GetVideo
import java.time.LocalDate
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** One slot of a mixed playlist session: exactly one of video / track is set. */
data class PlaylistSessionItem(val video: Video? = null, val track: Track? = null) {
    val id: String get() = video?.id ?: track?.id ?: ""
    val isVideo: Boolean get() = video != null
}

/**
 * F-VID-15 meta-playlist: ONE queue spanning videos and music tracks. The
 * session plays [items] in order — a video can roll into a track and a
 * track into a video — until it ends, repeats or the user plays something else.
 */
data class PlaylistSession(
    val name: String,
    val items: List<PlaylistSessionItem>,
    val index: Int
) {
    val current: PlaylistSessionItem? get() = items.getOrNull(index)
}

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
    private val viewingSession: ViewingSessionRepository,
    private val trackMissionAction: TrackMissionAction,
    updateStreak: UpdateStreak
) : ViewModel() {

    private val _uiState = MutableStateFlow(restoreFromSavedState())
    val uiState: StateFlow<SessionState> = _uiState.asStateFlow()

    /** Engine playback truths for dock, floating surface, slider and transport UI. */
    val music: StateFlow<MusicPlayback> = port.music
    val video: StateFlow<VideoPlayback> = port.video

    /** First-time video+music collision; the shell renders it as the C1 dialog. */
    private val _conflict = MutableStateFlow<MusicConflictRequest?>(null)
    val conflict: StateFlow<MusicConflictRequest?> = _conflict.asStateFlow()

    /** C2: opening a video while music plays also asks, mirroring C1. */
    private val _videoConflict = MutableStateFlow<VideoConflictRequest?>(null)
    val videoConflict: StateFlow<VideoConflictRequest?> = _videoConflict.asStateFlow()

    /** One-shot restored tab after a cold start (TC10 restore chain). */
    private val _restoredSection = MutableStateFlow<Section?>(null)
    val restoredSection: StateFlow<Section?> = _restoredSection.asStateFlow()
    private var restoreAllowed = true
    private var requestVersion = 0L

    /** The running meta-playlist, or null when no playlist is playing. */
    private val _playlistSession = MutableStateFlow<PlaylistSession?>(null)
    val playlistSession: StateFlow<PlaylistSession?> = _playlistSession.asStateFlow()

    init {
        viewModelScope.launch {
            port.music.collect { playback ->
                updateMusic(playback)
                syncPlaylistIndexToTrack(playback.trackId)
                val endedTrackId = playback.trackId
                if (playback.status == PlaybackStatus.ENDED && endedTrackId != null) {
                    advancePlaylist(endedTrackId, isVideo = false)
                }
            }
        }
        viewModelScope.launch {
            port.video.collect { playback ->
                updateVideo(playback)
                val endedVideoId = playback.videoId
                if (playback.status == PlaybackStatus.ENDED && endedVideoId != null) {
                    advancePlaylist(endedVideoId, isVideo = true)
                }
            }
        }
        viewModelScope.launch { restoreColdStartSession() }
        // F-COM-06: opening the app is today's streak activity — the
        // calculator keeps the count, increments or resets by date itself.
        viewModelScope.launch {
            val today = LocalDate.now()
            updateStreak(today.toString(), today.minusDays(1).toString())
        }
    }

    fun enter(section: Section) {
        cancelPendingRestore()
        update(SessionPolicy.enter(_uiState.value, section))
        viewModelScope.launch { preferences.setLastSection(section) }
    }

    fun playVideo(id: String, title: String = id) {
        endPlaylistSession() // a deliberate single-video play replaces the playlist queue
        val version = newPlaybackRequest()
        viewModelScope.launch {
            val video = getVideo().firstOrNull()?.firstOrNull { it.id == id } ?: return@launch
            val resumeFrom = viewingSession.progressOf(id)
            if (version != requestVersion) return@launch
            // C2: if music is playing, ask before opening the video.
            val music = port.music.value
            val musicPlaying = music.playRequested && music.error == null && music.trackId != null
            if (musicPlaying) {
                // C2: check saved preference first — null = still asking.
                val savedChoice = preferences.preferences.first().videoConflictPauseMusic
                if (savedChoice != null) {
                    port.openVideo(video, resumeFrom, VideoStartPlan.Play(pauseMusic = savedChoice))
                } else {
                    _videoConflict.value = VideoConflictRequest(id, title, resumeFrom)
                }
            } else {
                port.openVideo(video, resumeFrom, VideoStartPlan.Play(pauseMusic = true))
            }
        }
    }

    /** Resolve the C2 video conflict: open the video with or without pausing music. */
    fun resolveVideoConflict(pauseMusic: Boolean, remember: Boolean = false) {
        val request = _videoConflict.value ?: return
        _videoConflict.value = null
        viewModelScope.launch {
            val video = getVideo().firstOrNull()?.firstOrNull { it.id == request.videoId } ?: return@launch
            port.openVideo(video, request.resumeFrom, VideoStartPlan.Play(pauseMusic = pauseMusic))
            if (remember) preferences.setVideoConflictChoice(pauseMusic)
        }
    }

    fun dismissVideoConflict() { _videoConflict.value = null }

    fun playMusic(id: String, title: String = id) {
        endPlaylistSession() // a deliberate single-track play replaces the playlist queue
        val version = newPlaybackRequest()
        viewModelScope.launch {
            val queue = getMusic().firstOrNull().orEmpty()
            val track = queue.firstOrNull { it.id == id } ?: return@launch
            startMusic(track, queue, version)
        }
    }

    /** F-MUS-04: "Phát tất cả" / "Ngẫu nhiên" plays a playlist as the queue. */
    fun playMusicPlaylist(track: com.example.onespace.domain.music.model.Track, queue: List<com.example.onespace.domain.music.model.Track>) {
        endPlaylistSession()
        val version = newPlaybackRequest()
        viewModelScope.launch { startMusic(track, queue, version) }
    }

    // ---- Meta-playlist session (F-VID-15) ----

    /**
     * Starts a playlist as ONE mixed session: the ordered ids interleave
     * videos and tracks, and the session auto-advances through them whatever
     * the type. Returns the session so the caller can navigate to the
     * first item's screen.
     */
    suspend fun startPlaylistSession(name: String, startId: String? = null): PlaylistSession? {
        val playlist = AppPlaylistStore.playlists[name] ?: return null
        newPlaybackRequest()
        val videos = getVideo().firstOrNull().orEmpty()
        val tracks = getMusic().firstOrNull().orEmpty()
        val settings = AppPlaylistStore.settingsOf(name)
        val ids = playlist.orderedIds().let { if (settings.order == "shuffle") it.shuffled() else it }
        val items = ids.mapNotNull { id ->
            videos.firstOrNull { it.id == id }?.let { PlaylistSessionItem(video = it) }
                ?: tracks.firstOrNull { it.id == id }?.let { PlaylistSessionItem(track = it) }
        }
        if (items.isEmpty()) return null
        val index = startId?.let { id -> items.indexOfFirst { it.id == id }.takeIf { it >= 0 } } ?: 0
        val session = PlaylistSession(name, items, index)
        _playlistSession.value = session
        // Tracks must reach STATE_ENDED instead of looping, so the session owns every transition.
        port.setMusicQueueLoop(false)
        startSessionItem()
        return session
    }

    /** Jumps to an item tapped in a session screen (queue view, playlist list). */
    fun jumpToPlaylistItem(id: String) {
        val session = _playlistSession.value ?: return
        val index = session.items.indexOfFirst { it.id == id }
        if (index < 0 || index == session.index) return
        _playlistSession.value = session.copy(index = index)
        startSessionItem()
    }

    /** Manual skip inside a session (the player's next/previous buttons): sequential, honouring repeat. */
    fun skipPlaylistItem(forward: Boolean) {
        val session = _playlistSession.value ?: return
        val repeat = AppPlaylistStore.settingsOf(session.name).repeat
        val nextIndex = if (forward) {
            if (session.index < session.items.lastIndex) session.index + 1
            else if (repeat) 0 else return
        } else {
            if (session.index > 0) session.index - 1
            else if (repeat) session.items.lastIndex else return
        }
        _playlistSession.value = session.copy(index = nextIndex)
        startSessionItem()
    }

    /** Ends the mixed session and restores the music engine's normal queue looping. */
    fun endPlaylistSession() {
        if (_playlistSession.value == null) return
        _playlistSession.value = null
        port.setMusicQueueLoop(true)
    }

    /** Plays the session's current item on the right engine, cleaning up the other one's leftovers. */
    private fun startSessionItem() {
        val session = _playlistSession.value ?: return
        val item = session.current ?: return
        if (item.video != null) {
            // An ended run of music must not linger as the dock's mini player.
            if (port.music.value.status == PlaybackStatus.ENDED) port.stopMusic()
            viewModelScope.launch {
                val resumeFrom = viewingSession.progressOf(item.video.id)
                port.openVideo(item.video, resumeFrom, VideoStartPlan.Play(pauseMusic = true))
            }
        } else if (item.track != null) {
            // An ended video must not linger as the floating window over the music screen.
            if (port.video.value.status == PlaybackStatus.ENDED) port.dismissVideo(toWatchlist = false)
            // Consecutive tracks share one queue so the player's "View queue"
            // list stays meaningful; the queue ends where the next video takes over.
            val run = session.items.drop(session.index).takeWhile { it.track != null }.mapNotNull { it.track }
            if (run.isEmpty()) return
            // A deliberate playlist play takes the voice: pause any video, no conflict dialog.
            applyMusicPlan(MusicConflictRequest(item.track, run), MusicStartPlan.Start(VideoAudioEffect.PAUSE))
        }
    }

    /**
     * The session's heartbeat: a really-ended item (status ENDED) moves the
     * queue to the next one — video to track, track to video, wrap on repeat.
     */
    private fun advancePlaylist(endedId: String, isVideo: Boolean) {
        val session = _playlistSession.value ?: return
        val current = session.current ?: return
        // Stale ends: the engine that just ended is not the session's current item.
        if (endedId != current.id || isVideo != current.isVideo) return
        val settings = AppPlaylistStore.settingsOf(session.name)
        val nextIndex = when {
            settings.order == "shuffle" && session.items.size > 1 ->
                session.items.indices.filter { it != session.index }.random()
            session.index < session.items.lastIndex -> session.index + 1
            settings.repeat -> 0
            else -> { endPlaylistSession(); return }
        }
        _playlistSession.value = session.copy(index = nextIndex)
        startSessionItem()
    }

    /** A run of consecutive tracks advances inside the music engine; the session follows it. */
    private fun syncPlaylistIndexToTrack(trackId: String?) {
        val session = _playlistSession.value ?: return
        if (trackId == null) return
        val current = session.current ?: return
        if (current.isVideo) return // stale music state while a session video plays
        val index = session.items.indexOfFirst { !it.isVideo && it.id == trackId }
        if (index > session.index) _playlistSession.value = session.copy(index = index)
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
                // F-COM-05: a track the user actually started counts toward
                // the daily listen mission (auto-advance lands here too).
                viewModelScope.launch { trackMissionAction(MissionType.LISTEN) }
            }
            MusicStartPlan.DoNotStart -> Unit
            MusicStartPlan.AskUser -> _conflict.value = request
        }
    }

    /**
     * Re-evaluate against current playback; the video may have stopped while
     * the dialog was open. The choice applies now; "remember" (the dialog's
     * "Không hỏi lại" tick) also saves it as the device rule.
     */
    fun resolveMusicConflict(rule: MediaConflictRule, remember: Boolean = false) {
        val request = _conflict.value ?: return
        _conflict.value = null
        applyMusicPlan(request, SessionPolicy.resolveMusicChoice(port.video.value.audible, rule))
        if (remember) viewModelScope.launch { preferences.setConflictRule(rule) }
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
            port.resumeVideo(VideoStartPlan.Play(pauseMusic = !playback.muted))
        }
    }

    /** Pause dock music so a spoken stream (TTS) can take the voice. */
    fun toggleMusicOff() = port.pauseMusic()

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

    /** F-MUS-01: shuffle and repeat change how "next" is picked, not how audio plays. */
    private val _musicShuffle = kotlinx.coroutines.flow.MutableStateFlow(false)
    val musicShuffle: kotlinx.coroutines.flow.StateFlow<Boolean> = _musicShuffle
    private val _musicRepeat = kotlinx.coroutines.flow.MutableStateFlow(false)
    val musicRepeat: kotlinx.coroutines.flow.StateFlow<Boolean> = _musicRepeat

    fun toggleShuffle() { _musicShuffle.value = !_musicShuffle.value }

    fun toggleRepeat() { _musicRepeat.value = !_musicRepeat.value }

    fun removeFromQueue(trackId: String) = port.removeTrackFromQueue(trackId)
    fun addToQueue(track: Track) = port.addTrackToQueue(track)

    /** Music playback speed for this session: x1 -> x1.5 -> x2 -> x0.5 -> x1 ... */
    private val _musicSpeed = MutableStateFlow(1f)
    val musicSpeed: StateFlow<Float> = _musicSpeed.asStateFlow()

    fun cycleMusicSpeed() {
        _musicSpeed.value = when (_musicSpeed.value) {
            1f -> 1.5f
            1.5f -> 2f
            2f -> 0.5f
            else -> 1f
        }
        port.setMusicSpeed(_musicSpeed.value)
    }

    /** Video playback speed for this session: x1 -> x1.5 -> x2 -> x0.5 -> x1 ... */
    private val _videoSpeed = MutableStateFlow(1f)
    val videoSpeed: StateFlow<Float> = _videoSpeed.asStateFlow()

    fun cycleVideoSpeed() {
        _videoSpeed.value = when (_videoSpeed.value) {
            1f -> 1.5f
            1.5f -> 2f
            2f -> 0.5f
            else -> 1f
        }
        port.setVideoSpeed(_videoSpeed.value)
    }
    fun moveQueueItem(fromIndex: Int, toIndex: Int) = port.moveTrackInQueue(fromIndex, toIndex)

    fun nextTrack() {
        // Inside a playlist session "next" is the next session item, even when it is a video.
        if (_playlistSession.value != null) { skipPlaylistItem(forward = true); return }
        val playback = port.music.value
        if (_musicRepeat.value) {
            playback.queue.firstOrNull { it.id == playback.trackId }
                ?.let { requestQueuedTrack(it, playback.queue) }
            return
        }
        if (_musicShuffle.value && playback.queue.size > 1) {
            playback.queue
                .filter { it.id != playback.trackId }
                .randomOrNull()
                ?.let { requestQueuedTrack(it, playback.queue) }
            return
        }
        SelectNextTrack(playback.queue, playback.trackId)?.let { requestQueuedTrack(it, playback.queue) }
    }

    fun previousTrack() {
        if (_playlistSession.value != null) { skipPlaylistItem(forward = false); return }
        val playback = port.music.value
        SelectPreviousTrack(playback.queue, playback.trackId)?.let { requestQueuedTrack(it, playback.queue) }
    }

    fun seekVideoBy(deltaMs: Long) = port.seekVideoBy(deltaMs)

    fun seekVideoTo(positionMs: Long) = port.seekVideoTo(positionMs)
    fun seekMusicTo(positionMs: Long) = port.seekMusicTo(positionMs)

    /** User's video volume for this session; independent of the C1 conflict mute. */
    private val _videoVolume = MutableStateFlow(1f)
    val videoVolume: StateFlow<Float> = _videoVolume.asStateFlow()

    fun setVideoVolume(volume: Float) {
        _videoVolume.value = volume.coerceIn(0f, 1f)
        port.setVideoVolume(_videoVolume.value)
    }

    /** User's music volume for this session; the Now Playing slider drives it. */
    private val _musicVolume = MutableStateFlow(1f)
    val musicVolume: StateFlow<Float> = _musicVolume.asStateFlow()

    fun setMusicVolume(volume: Float) {
        _musicVolume.value = volume.coerceIn(0f, 1f)
        port.setMusicVolume(_musicVolume.value)
    }

    /** Floating window dismissal (case C3): stop and keep the video for later. */
    fun closeVideo() {
        endPlaylistSession()
        newPlaybackRequest()
        port.dismissVideo(toWatchlist = true)
    }
    fun closeMusic() {
        endPlaylistSession()
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
