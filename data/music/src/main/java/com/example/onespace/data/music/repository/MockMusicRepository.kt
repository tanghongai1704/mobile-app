package com.example.onespace.data.music.repository

import com.example.onespace.domain.music.model.Track
import com.example.onespace.domain.music.repository.MusicRepository

import kotlinx.coroutines.flow.flowOf

/** Bundled demo data. Replace this binding, not the UI, when integrating real sources. */
internal class MockMusicRepository : MusicRepository {
    override fun observeCatalog() = flowOf(listOf(
        Track("m1", "Morning light", "Lo-fi Collective", "3:42", true),
        Track("m2", "Somewhere quiet", "Sunday Radio", "4:08"),
        Track("m3", "Late night thoughts", "Chill Station", "2:56", true)
    ))
}
