package com.example.onespace.data.music.repository

import com.example.onespace.domain.music.model.Track
import com.example.onespace.domain.music.repository.MusicRepository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

/**
 * Bundled demo catalog: SoundHelix's freely distributable sample songs, so the
 * audio pipeline runs for real. Replace this binding, not the UI, later.
 */
internal class MockMusicRepository : MusicRepository {
    override fun observeCatalog() = flow {
        delay(400)
        emit(listOf(
        Track(
            "m1", "Nắng sớm", "Nhóm Lo-fi", "5:40",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3", true
        ),
        Track(
            "m2", "Nơi nào yên tĩnh", "Đài Chủ nhật", "6:10",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"
        ),
        Track(
            "m3", "Suy nghĩ đêm muộn", "Trạm Chill", "6:32",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3", true
        ),
        Track(
            "m4", "Chuyện kể chậm rãi", "Đài Chủ nhật", "5:28",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3"
        )
        ))
    }
}
