package com.example.onespace.domain.music.usecase

import com.example.onespace.domain.music.model.Track

/** Queue policy belongs to domain, not a navigation callback. */
object SelectNextTrack {
    operator fun invoke(queue: List<Track>, currentId: String?): Track? {
        if (queue.isEmpty()) return null
        val index = queue.indexOfFirst { it.id == currentId }
        return queue[(index + 1) % queue.size]
    }
}
