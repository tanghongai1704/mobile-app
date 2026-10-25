package com.example.onespace

import androidx.lifecycle.SavedStateHandle
import com.example.onespace.core.session.SessionCoordinator
import com.example.onespace.domain.music.model.Track
import com.example.onespace.domain.music.repository.MusicRepository
import com.example.onespace.domain.music.usecase.GetMusic
import com.example.onespace.domain.session.MediaConflictRule
import com.example.onespace.domain.session.Section
import com.example.onespace.domain.session.VideoStartPlan
import com.example.onespace.domain.session.port.PlaybackStatus
import com.example.onespace.domain.session.VideoAudioEffect
import com.example.onespace.domain.session.port.MediaSessionPort
import com.example.onespace.domain.session.port.MusicPlayback
import com.example.onespace.domain.session.port.VideoPlayback
import com.example.onespace.domain.session.repository.SessionPreferences
import com.example.onespace.domain.session.repository.SessionPreferencesRepository
import com.example.onespace.domain.session.repository.ThemeMode
import com.example.onespace.domain.video.model.Video
import com.example.onespace.domain.video.repository.ContinueEntry
import com.example.onespace.domain.video.repository.VideoRepository
import com.example.onespace.domain.video.repository.ViewingSessionRepository
import com.example.onespace.domain.video.usecase.GetVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionCoordinatorTest {

    private val dispatcher = StandardTestDispatcher()

    private class FakePort : MediaSessionPort {
        val musicState = MutableStateFlow(MusicPlayback())
        val videoState = MutableStateFlow(VideoPlayback())
        val musicRequests = mutableListOf<Pair<String, VideoAudioEffect>>()

        override val music get() = musicState
        override val video get() = videoState

        override fun requestMusic(track: Track, queue: List<Track>, effect: VideoAudioEffect) {
            musicRequests.add(track.id to effect)
            when (effect) {
                VideoAudioEffect.MUTE -> videoState.value = videoState.value.copy(muted = true)
                VideoAudioEffect.PAUSE -> pauseVideo()
                VideoAudioEffect.NONE -> Unit
            }
            musicState.value = MusicPlayback(track.id, track.title, track.artist, playing = true, queue = queue)
        }

        override fun prepareRestoredMusic(track: Track, queue: List<Track>) {
            musicState.value = MusicPlayback(track.id, track.title, track.artist, playing = false, queue = queue)
        }

        override fun pauseMusic() { musicState.value = musicState.value.copy(playing = false, playRequested = false) }
        override fun stopMusic() { musicState.value = MusicPlayback() }
        override fun seekMusicTo(positionMs: Long) { musicState.value = musicState.value.copy(positionMs = positionMs) }
        override fun removeTrackFromQueue(trackId: String) = Unit
        override fun moveTrackInQueue(fromIndex: Int, toIndex: Int) {
            val q = musicState.value.queue.toMutableList()
            if (fromIndex in q.indices && toIndex in q.indices && fromIndex != toIndex) {
                q.add(toIndex, q.removeAt(fromIndex))
                musicState.value = musicState.value.copy(queue = q)
            }
        }

        override fun openVideo(video: Video, startPositionMs: Long, plan: VideoStartPlan) {
            if (plan.pauseMusic) pauseMusic()
            videoState.value = VideoPlayback(video.id, video.title, playing = true, positionMs = startPositionMs)
        }

        override fun resumeVideo(plan: VideoStartPlan) {
            if (plan.pauseMusic) pauseMusic()
            videoState.value = videoState.value.copy(playing = true, playRequested = true, status = PlaybackStatus.READY, error = null)
        }
        override fun pauseVideo() { videoState.value = videoState.value.copy(playing = false, playRequested = false) }
        override fun seekVideoBy(deltaMs: Long) = Unit
        override fun seekVideoTo(positionMs: Long) { videoState.value = videoState.value.copy(positionMs = positionMs) }
        override fun dismissVideo(toWatchlist: Boolean) { videoState.value = VideoPlayback() }
    }

    private class FakePrefs : SessionPreferencesRepository {
        val state = MutableStateFlow(SessionPreferences())
        override val preferences: Flow<SessionPreferences> get() = state
        override suspend fun setLastSection(section: Section) = Unit
        override suspend fun setLastTrackId(trackId: String?) { state.value = state.value.copy(lastTrackId = trackId) }
        override suspend fun setConflictRule(rule: MediaConflictRule?) {
            state.value = state.value.copy(conflictRule = rule)
        }
        override suspend fun setDataSaver(enabled: Boolean) = Unit
        override suspend fun setThemeMode(mode: ThemeMode) = Unit
        override suspend fun setOnboarded(done: Boolean) = Unit
        override suspend fun setAuthSession(email: String?, guest: Boolean) = Unit
        override suspend fun setSelectedTopics(topics: Set<String>) = Unit
        override suspend fun setLanguage(language: com.example.onespace.domain.session.repository.Language) = Unit
        override suspend fun setSimulateOffline(enabled: Boolean) = Unit
        override suspend fun setNotificationsEnabled(enabled: Boolean) = Unit
    }

    private val tracks = listOf(Track("m1", "A track", "Artist", "3:00", "https://example.com/m1.mp3"))
    private val videos = listOf(Video("v1", "A video", "Studio", "9:00", "https://example.com/v1.mp4"))

    private fun coordinator(handle: SavedStateHandle, port: FakePort, prefs: FakePrefs = FakePrefs()): SessionCoordinator {
        val musicRepository = object : MusicRepository { override fun observeCatalog() = flowOf(tracks) }
        val videoRepository = object : VideoRepository { override fun observeCatalog() = flowOf(videos) }
        val viewing = object : ViewingSessionRepository {
            override fun observeContinue(): Flow<List<ContinueEntry>> = flowOf(emptyList())
            override fun observeWatchlist(): Flow<List<ContinueEntry>> = flowOf(emptyList())
            override fun observeHistory(): Flow<List<ContinueEntry>> = flowOf(emptyList())
            override suspend fun addToWatchlist(videoId: String, title: String) = Unit
            override suspend fun removeFromWatchlist(videoId: String) = Unit
            override suspend fun progressOf(videoId: String): Long = 0
            override suspend fun saveProgress(videoId: String, positionMs: Long) = Unit
        }
        return SessionCoordinator(
            handle, port, prefs, GetMusic(musicRepository), GetVideo(videoRepository), viewing
        )
    }

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun restoredSessionKeepsMediaIdentityAndTitlesWithoutAutoplay() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        handle["videoId"] = "v1"; handle["videoTitle"] = "A video"
        handle["trackId"] = "m1"; handle["trackTitle"] = "A track"
        val restored = coordinator(handle, FakePort()).uiState.value
        assertEquals("v1", restored.videoId)
        assertEquals("A video", restored.videoTitle)
        assertEquals("m1", restored.trackId)
        assertEquals("A track", restored.trackTitle)
    }

    @Test fun engineStateDrivesTheUiSessionState() = runTest(dispatcher) {
        val port = FakePort()
        val coordinator = coordinator(SavedStateHandle(), port)
        port.musicState.value = MusicPlayback("m1", "A track", "Artist", playing = true)
        advanceUntilIdle()
        assertEquals("m1", coordinator.uiState.value.trackId)
        assertEquals("A track", coordinator.uiState.value.trackTitle)
        assertTrue(coordinator.music.value.playing)
        port.musicState.value = MusicPlayback()
        advanceUntilIdle()
        assertNull(coordinator.uiState.value.trackId)
        assertFalse(coordinator.music.value.playing)
    }

    @Test fun firstVideoMusicCollisionAsksOnceThenAppliesTheRememberedRule() = runTest(dispatcher) {
        val port = FakePort()
        val prefs = FakePrefs()
        val coordinator = coordinator(SavedStateHandle(), port, prefs)
        port.videoState.value = VideoPlayback("v1", "A video", playing = true)

        coordinator.playMusic("m1", "A track")
        advanceUntilIdle()
        // First collision: the engine was not touched and the shell got the request.
        assertTrue(port.musicRequests.isEmpty())
        assertNotNull(coordinator.conflict.value)

        coordinator.resolveMusicConflict(MediaConflictRule.MUTE_VIDEO)
        advanceUntilIdle()
        assertEquals("m1" to VideoAudioEffect.MUTE, port.musicRequests.single())
        assertEquals(MediaConflictRule.MUTE_VIDEO, prefs.state.value.conflictRule)
        assertNull(coordinator.conflict.value)

        // Muted video no longer competes for audio, even when another track is requested.
        coordinator.playMusic("m1", "A track")
        advanceUntilIdle()
        assertEquals(2, port.musicRequests.size)
        assertEquals(VideoAudioEffect.NONE, port.musicRequests.last().second)
        assertNull(coordinator.conflict.value)
    }

    @Test fun openingAVideoStartsPlaybackThroughTheEngine() = runTest(dispatcher) {
        val port = FakePort()
        val coordinator = coordinator(SavedStateHandle(), port)
        coordinator.playVideo("v1", "A video")
        advanceUntilIdle()
        assertEquals("v1", port.videoState.value.videoId)
        assertTrue(port.videoState.value.playing)
    }

    @Test fun coldStartRestoresTheLastTabAndPreparesMusicPaused() = runTest(dispatcher) {
        val port = FakePort()
        val prefs = FakePrefs()
        prefs.state.value = SessionPreferences(lastSection = Section.NEWS, lastTrackId = "m1")
        val coordinator = coordinator(SavedStateHandle(), port, prefs)
        advanceUntilIdle()
        assertEquals(Section.NEWS, coordinator.restoredSection.value)
        assertEquals("m1", port.music.value.trackId)
        assertFalse(port.music.value.playing)
    }

    @Test fun userNavigationWinsOverPendingColdStartRestore() = runTest(dispatcher) {
        val port = FakePort()
        val prefs = FakePrefs()
        prefs.state.value = SessionPreferences(lastSection = Section.NEWS, lastTrackId = "m1")
        val coordinator = coordinator(SavedStateHandle(), port, prefs)
        coordinator.enter(Section.GAMES)
        advanceUntilIdle()
        assertNull(coordinator.restoredSection.value)
        assertNull(port.music.value.trackId)
    }

    @Test fun closingTheDockDoesNotBringItBackOnColdStart() = runTest(dispatcher) {
        val port = FakePort()
        val prefs = FakePrefs()
        val coordinator = coordinator(SavedStateHandle(), port, prefs)
        coordinator.playMusic("m1")
        advanceUntilIdle()
        coordinator.closeMusic()
        advanceUntilIdle()
        assertNull(port.music.value.trackId)
        assertNull(prefs.state.value.lastTrackId)
    }

    @Test fun musicCatalogDoesNotChangeEnginePlaybackOrOpenAConflict() = runTest(dispatcher) {
        val port = FakePort()
        val coordinator = coordinator(SavedStateHandle(), port)
        port.videoState.value = VideoPlayback("v1", "A video", playing = true)
        advanceUntilIdle()
        coordinator.enter(Section.MUSIC)
        advanceUntilIdle()
        assertTrue(coordinator.video.value.playing)
        assertNull(coordinator.conflict.value)
        assertTrue(port.musicRequests.isEmpty())
    }

    @Test fun nextAndPreviousUseTheActiveQueueInsteadOfTheCatalog() = runTest(dispatcher) {
        val port = FakePort()
        val coordinator = coordinator(SavedStateHandle(), port)
        val second = tracks.single().copy(id = "outside-catalog", title = "Queue only")
        val queue = listOf(tracks.single(), second)
        port.musicState.value = MusicPlayback("m1", queue = queue)
        coordinator.nextTrack()
        advanceUntilIdle()
        assertEquals(second.id, port.music.value.trackId)
        coordinator.previousTrack()
        advanceUntilIdle()
        assertEquals("m1", port.music.value.trackId)
        assertEquals(queue, port.music.value.queue)
    }

    @Test fun resumingMutedVideoKeepsMusicButAudibleVideoPausesIt() = runTest(dispatcher) {
        val port = FakePort()
        val coordinator = coordinator(SavedStateHandle(), port)
        port.musicState.value = MusicPlayback("m1", playing = true)
        port.videoState.value = VideoPlayback("v1", muted = true)
        coordinator.toggleVideo()
        assertTrue(port.video.value.playing)
        assertTrue(port.music.value.playing)
        port.videoState.value = VideoPlayback("v1", muted = false)
        coordinator.toggleVideo()
        assertFalse(port.music.value.playRequested)
    }

    @Test fun bufferingMusicCanBePausedWithoutRestartingTheQueue() = runTest(dispatcher) {
        val port = FakePort()
        val coordinator = coordinator(SavedStateHandle(), port)
        port.musicState.value = MusicPlayback("m1", playRequested = true, status = PlaybackStatus.BUFFERING, queue = tracks)
        coordinator.toggleMusic()
        advanceUntilIdle()
        assertFalse(port.music.value.playRequested)
        assertTrue(port.musicRequests.isEmpty())
    }

    @Test fun closingMediaCancelsAnOlderPendingPlayRequest() = runTest(dispatcher) {
        val port = FakePort()
        val coordinator = coordinator(SavedStateHandle(), port)
        coordinator.playVideo("v1")
        coordinator.playMusic("m1")
        coordinator.closeMusic()
        coordinator.closeVideo()
        advanceUntilIdle()
        assertNull(port.video.value.videoId)
        assertNull(port.music.value.trackId)
    }

    @Test fun backgroundCannotBeOvertakenByAnOlderPendingVideoRequest() = runTest(dispatcher) {
        val port = FakePort()
        val coordinator = coordinator(SavedStateHandle(), port)
        coordinator.playVideo("v1")
        coordinator.background()
        advanceUntilIdle()
        assertNull(port.video.value.videoId)
    }

    @Test fun keepVideoNeverSendsAStartCommandToTheEngine() = runTest(dispatcher) {
        val port = FakePort()
        val coordinator = coordinator(SavedStateHandle(), port)
        port.videoState.value = VideoPlayback("v1", playing = true)
        coordinator.playMusic("m1")
        advanceUntilIdle()
        coordinator.resolveMusicConflict(MediaConflictRule.KEEP_VIDEO)
        advanceUntilIdle()
        assertTrue(port.musicRequests.isEmpty())
        assertTrue(port.video.value.playing)
    }
}
