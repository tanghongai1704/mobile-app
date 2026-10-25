package com.example.onespace.feature.video.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.example.onespace.core.common.ContentUiState
import com.example.onespace.core.media.MediaPlaybackCoordinator
import com.example.onespace.domain.session.repository.SessionPreferencesRepository
import com.example.onespace.domain.video.repository.ContinueEntry
import com.example.onespace.domain.video.usecase.GetVideo
import com.example.onespace.domain.video.usecase.ObserveContinueWatching
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
internal class VideoViewModel @Inject constructor(
    getVideo: GetVideo,
    observeContinue: ObserveContinueWatching,
    private val engine: MediaPlaybackCoordinator,
    preferences: SessionPreferencesRepository,
    private val viewingSession: com.example.onespace.domain.video.repository.ViewingSessionRepository,
    private val addPoints: com.example.onespace.domain.session.usecase.AddPoints,
    private val extractedAudio: com.example.onespace.domain.music.repository.ExtractedAudioRepository,
    private val downloadedContent: com.example.onespace.domain.session.repository.DownloadedContentRepository
) : ViewModel() {
    private val refresh = MutableStateFlow(0)

    val uiState = combine(getVideo(), refresh) { items, _ -> ContentUiState(items = items) }
        .catch { emit(ContentUiState(error = "Không tải được danh sách video.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContentUiState(isLoading = true))

    val continueWatching = observeContinue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<ContinueEntry>())

    /** F-VID-05: videos actually started, with the last stop position. */
    val history = viewingSession.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<ContinueEntry>())

    /** Demo switch from Settings: feeds render their offline state while on. */
    val offline = preferences.preferences.map { it.simulateOffline }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** F-VID-02: topics picked at onboarding drive the "Dành cho bạn" section of the feed. */
    val selectedTopics = preferences.preferences.map { it.selectedTopics }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** F-VID-09: audio-only mode for video detail screen. */
    private val _audioOnly = MutableStateFlow(false)
    val audioOnly = _audioOnly.asStateFlow()

    fun toggleAudioOnly() { setAudioOnly(!_audioOnly.value) }

    /** F-VID-09: the shell reads this flag to render the floating music card. */
    fun setAudioOnly(value: Boolean) {
        _audioOnly.value = value
        com.example.onespace.core.designsystem.AudioOnlyState.set(value)
    }

    /** F-VID-17: playback speed, applied to the shared player. */
    private val _speed = MutableStateFlow(1f)
    val speed = _speed.asStateFlow()

    fun setSpeed(value: Float) {
        _speed.value = value
        player.playbackParameters = androidx.media3.common.PlaybackParameters(value)
    }

    /** F-VID-06: manual quality pick; "Auto" leaves Media3 adaptive streaming in charge. */
    private val _quality = MutableStateFlow("Auto")
    val quality = _quality.asStateFlow()

    fun setQuality(value: String) { _quality.value = value }

    /** F-VID-04: videos saved for later. */
    val watchLaterIds = viewingSession.observeWatchlist()
        .map { list -> list.map { it.videoId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** F-VID-07: downloaded video ids, persisted in Room (DB v5). */
    val downloads = downloadedContent.observeByType("video")
        .map { list -> list.map { it.contentId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun toggleDownload(videoId: String, title: String = "", subtitle: String = "") {
        viewModelScope.launch {
            if (videoId in downloads.value) downloadedContent.remove(videoId)
            else downloadedContent.add(videoId, "video", title, subtitle)
        }
    }

    /** F-VID-10: recording the extraction makes the library entry real. */
    fun addExtracted(videoId: String, title: String, creator: String, url: String, duration: String) {
        viewModelScope.launch { extractedAudio.add(videoId, title, creator, url, duration) }
    }

    /** F-VID-18: reward for answering the end-of-video quiz correctly. */
    fun awardEndOfVideoQuiz() {
        viewModelScope.launch { addPoints("quiz_cuoi_video", 20) }
    }

    fun toggleWatchLater(videoId: String, title: String = "") {
        viewModelScope.launch {
            if (videoId in watchLaterIds.value) viewingSession.removeFromWatchlist(videoId)
            else viewingSession.addToWatchlist(videoId, title)
        }
    }

    fun retry() { refresh.value = refresh.value + 1 }

    /** The single shared video player instance (ADR-08: never recreated per screen). */
    val player: Player get() = engine.videoPlayer
}
