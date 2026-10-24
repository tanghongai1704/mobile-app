package com.example.onespace.domain.music

import com.example.onespace.domain.music.model.Track
import com.example.onespace.domain.music.usecase.SelectPreviousTrack
import org.junit.Assert.*
import org.junit.Test

class SelectPreviousTrackTest {
    private val queue = listOf(Track("a", "A", "Artist", "1:00", "a"), Track("b", "B", "Artist", "1:00", "b"))
    @Test fun wrapsAtTheStartAndSelectsThePreviousItem() {
        assertEquals("b", SelectPreviousTrack(queue, "a")?.id)
        assertEquals("a", SelectPreviousTrack(queue, "b")?.id)
    }
    @Test fun handlesMissingIdentityAndEmptyQueue() {
        assertEquals("b", SelectPreviousTrack(queue, "missing")?.id)
        assertNull(SelectPreviousTrack(emptyList(), null))
    }
}
