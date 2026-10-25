package com.example.onespace.data.music

import com.example.onespace.domain.music.repository.MusicRepository
import com.example.onespace.data.music.repository.MockMusicRepository
import com.example.onespace.core.database.ContentDatabase
import com.example.onespace.domain.music.repository.ExtractedAudioRepository

/** Public wiring API; repository implementations remain internal to this module. */
fun createMusicRepository(): MusicRepository = MockMusicRepository()

fun createExtractedAudioRepository(database: ContentDatabase): ExtractedAudioRepository =
    com.example.onespace.data.music.repository.createExtractedAudioRepository(database)
