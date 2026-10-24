package com.example.onespace.domain.music.usecase

import com.example.onespace.domain.music.model.Track

object SelectPreviousTrack {
    operator fun invoke(queue: List<Track>, currentId: String?): Track? {
        if (queue.isEmpty()) return null
        val index = queue.indexOfFirst { it.id == currentId }
        return queue[if (index < 0) queue.lastIndex else (index - 1 + queue.size) % queue.size]
    }
}
