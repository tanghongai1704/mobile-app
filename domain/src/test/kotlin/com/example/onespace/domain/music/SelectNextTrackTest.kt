package com.example.onespace.domain.music

import com.example.onespace.domain.music.model.Track
import com.example.onespace.domain.music.usecase.SelectNextTrack
import org.junit.Assert.*
import org.junit.Test

class SelectNextTrackTest {
    private val queue = listOf(Track("a", "A", "Artist", "3:00"), Track("b", "B", "Artist", "4:00"))
    @Test fun nextWrapsToBeginningOfQueue() {
        assertEquals("b", SelectNextTrack(queue, "a")?.id)
        assertEquals("a", SelectNextTrack(queue, "b")?.id)
    }
    @Test fun emptyOrMissingCurrentItemIsHandled() {
        assertNull(SelectNextTrack(emptyList(), "a"))
        assertEquals("a", SelectNextTrack(queue, "unknown")?.id)
    }
}
