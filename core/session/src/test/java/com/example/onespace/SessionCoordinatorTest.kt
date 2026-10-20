package com.example.onespace

import androidx.lifecycle.SavedStateHandle
import com.example.onespace.core.session.SessionCoordinator
import org.junit.Assert.*
import org.junit.Test

class SessionCoordinatorTest {
    @Test fun restoredSessionKeepsMediaIdentityAndTitlesWithoutAutoplay() {
        val handle = SavedStateHandle()
        SessionCoordinator(handle).apply {
            playVideo("v1", "A video")
            playMusic("m1", "A track")
        }
        val restored = SessionCoordinator(handle).uiState.value
        assertEquals("v1", restored.videoId)
        assertEquals("A video", restored.videoTitle)
        assertEquals("m1", restored.trackId)
        assertEquals("A track", restored.trackTitle)
        assertFalse(restored.videoPlaying)
        assertFalse(restored.musicPlaying)
    }
}
