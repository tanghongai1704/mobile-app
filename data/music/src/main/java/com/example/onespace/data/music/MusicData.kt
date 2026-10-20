package com.example.onespace.data.music

import com.example.onespace.domain.music.repository.MusicRepository
import com.example.onespace.data.music.repository.MockMusicRepository

/** Public wiring API; repository implementations remain internal to this module. */
fun createMusicRepository(): MusicRepository = MockMusicRepository()
