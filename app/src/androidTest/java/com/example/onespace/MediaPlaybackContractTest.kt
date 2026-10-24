package com.example.onespace

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.example.onespace.core.datastore.SessionPreferencesStore
import com.example.onespace.core.media.MediaPlaybackCoordinator
import com.example.onespace.domain.music.model.Track
import com.example.onespace.domain.session.VideoAudioEffect
import com.example.onespace.domain.session.port.PlaybackStatus
import com.example.onespace.domain.video.repository.ContinueEntry
import com.example.onespace.domain.video.repository.ViewingSessionRepository
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Real Media3 contracts using local files: no backend or network timing dependency. */
@RunWith(AndroidJUnit4::class)
class MediaPlaybackContractTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private lateinit var engine: MediaPlaybackCoordinator
    private lateinit var activity: ActivityScenario<MainActivity>
    private val fixtures = mutableListOf<File>()

    @Before fun createEngine() {
        // API 35+ requires a foreground Activity (or a media service) for audio focus.
        activity = ActivityScenario.launch(MainActivity::class.java)
        val viewing = object : ViewingSessionRepository {
            override fun observeContinue(): Flow<List<ContinueEntry>> = flowOf(emptyList())
            override suspend fun addToWatchlist(videoId: String, title: String) = Unit
            override suspend fun removeFromWatchlist(videoId: String) = Unit
            override suspend fun progressOf(videoId: String): Long = 0
            override suspend fun saveProgress(videoId: String, positionMs: Long) = Unit
        }
        instrumentation.runOnMainSync {
            engine = MediaPlaybackCoordinator(context, viewing, SessionPreferencesStore(context))
        }
        await {
            var focused = false
            activity.onActivity { focused = it.hasWindowFocus() }
            focused
        }
    }

    @After fun releaseEngine() {
        if (::engine.isInitialized) instrumentation.runOnMainSync { engine.release() }
        if (::activity.isInitialized) activity.close()
        fixtures.forEach { it.delete() }
    }

    private fun await(condition: () -> Boolean) {
        val deadline = android.os.SystemClock.elapsedRealtime() + 20_000
        while (!condition() && android.os.SystemClock.elapsedRealtime() < deadline) Thread.sleep(25)
        assertTrue("Playback condition timed out: " + engine.music.value, condition())
    }

    @Test fun failedSourceReportsErrorWithoutClaimingActualPlayback() {
        val track = Track("missing", "Missing source", "Test", "0:01", File(context.cacheDir, "missing-section4-media.wav").toURI().toString())
        instrumentation.runOnMainSync { engine.requestMusic(track, listOf(track), VideoAudioEffect.NONE) }
        await { engine.music.value.status == PlaybackStatus.ERROR }
        val state = engine.music.value
        assertFalse(state.playing)
        assertNotNull(state.error)
        assertEquals(listOf(track), state.queue)
        // Retry keeps the same identity/queue and can be paused independently of load success.
        instrumentation.runOnMainSync {
            engine.requestMusic(track, state.queue, VideoAudioEffect.NONE)
            engine.pauseMusic()
        }
        await { !engine.music.value.playRequested }
        assertEquals(track.id, engine.music.value.trackId)
    }

    @Test fun autoAdvanceUsesTheSamePublishedPlaylist() {
        val queue = listOf(wavTrack("first", 700), wavTrack("second", 3_000))
        instrumentation.runOnMainSync { engine.requestMusic(queue.first(), queue, VideoAudioEffect.NONE) }
        await { engine.music.value.trackId == "second" }
        assertEquals(queue, engine.music.value.queue)
        instrumentation.runOnMainSync { engine.pauseMusic() }
        await { !engine.music.value.playRequested }
    }

    private fun wavTrack(id: String, durationMs: Int): Track {
        val samples = 8_000 * durationMs / 1_000
        val bytes = samples * 2
        val buffer = ByteBuffer.allocate(44 + bytes).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()).putInt(36 + bytes).put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray()).putInt(16).putShort(1.toShort()).putShort(1.toShort())
        buffer.putInt(8_000).putInt(16_000).putShort(2.toShort()).putShort(16.toShort())
        buffer.put("data".toByteArray()).putInt(bytes)
        val file = File.createTempFile("section4-" + id, ".wav", context.cacheDir)
        file.writeBytes(buffer.array())
        fixtures += file
        return Track(id, id, "Local fixture", "0:03", file.toURI().toString())
    }
}
