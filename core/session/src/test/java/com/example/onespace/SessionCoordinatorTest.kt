package com.example.onespace

import androidx.lifecycle.SavedStateHandle
import com.example.onespace.core.session.SessionCoordinator
import com.example.onespace.domain.music.model.Track
import com.example.onespace.domain.music.repository.MusicRepository
import com.example.onespace.domain.music.usecase.GetMusic
import com.example.onespace.domain.session.MediaConflictRule
import com.example.onespace.domain.session.Section
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
            musicState.value = MusicPlayback(track.id, track.title, track.artist, playing = true)
        }

        override fun resolveMusicConflict(track: Track, queue: List<Track>, rule: MediaConflictRule) {
            if (rule != MediaConflictRule.KEEP_VIDEO) {
                requestMusic(track, queue, when (rule) {
                    MediaConflictRule.MUTE_VIDEO -> VideoAudioEffect.MUTE
                    MediaConflictRule.PAUSE_VIDEO -> VideoAudioEffect.PAUSE
                    MediaConflictRule.KEEP_VIDEO -> VideoAudioEffect.NONE
                })
            }
        }

        override fun prepareRestoredMusic(track: Track, queue: List<Track>) {
            musicState.value = MusicPlayback(track.id, track.title, track.artist, playing = false)
        }

        override fun toggleMusic() { musicState.value = musicState.value.copy(playing = !musicState.value.playing) }
        override fun stopMusic() { musicState.value = MusicPlayback() }
        override fun seekMusicTo(positionMs: Long) { musicState.value = musicState.value.copy(positionMs = positionMs) }
        override fun skipToNext() = Unit
        override fun skipToPrevious() = Unit

        override fun openVideo(video: Video, startPositionMs: Long) {
            videoState.value = VideoPlayback(video.id, video.title, playing = true, positionMs = startPositionMs)
        }

        override fun toggleVideo() { videoState.value = videoState.value.copy(playing = !videoState.value.playing) }
        override fun pauseVideo() { videoState.value = videoState.value.copy(playing = false) }
        override fun seekVideoBy(deltaMs: Long) = Unit
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
        override suspend fun setAuthEmail(email: String?) = Unit
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
        assertFalse(restored.videoPlaying)
        assertFalse(restored.musicPlaying)
    }

    @Test fun engineStateDrivesTheUiSessionState() = runTest(dispatcher) {
        val port = FakePort()
        val coordinator = coordinator(SavedStateHandle(), port)
        port.musicState.value = MusicPlayback("m1", "A track", "Artist", playing = true)
        advanceUntilIdle()
        assertEquals("m1", coordinator.uiState.value.trackId)
        assertEquals("A track", coordinator.uiState.value.trackTitle)
        assertTrue(coordinator.uiState.value.musicPlaying)
        port.musicState.value = MusicPlayback()
        advanceUntilIdle()
        assertNull(coordinator.uiState.value.trackId)
        assertFalse(coordinator.uiState.value.musicPlaying)
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

        // Same pair again: silent, the remembered rule is applied without asking.
        coordinator.playMusic("m1", "A track")
        advanceUntilIdle()
        assertEquals(2, port.musicRequests.size)
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
}
