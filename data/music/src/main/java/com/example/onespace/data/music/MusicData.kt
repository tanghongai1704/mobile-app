package com.example.onespace.data.music

import com.example.onespace.core.database.ContentDatabase
import com.example.onespace.core.network.api.CatalogApi
import com.example.onespace.core.network.fake.FakeRoute
import com.example.onespace.data.music.repository.MusicFakeRoute
import com.example.onespace.data.music.repository.OfflineFirstMusicRepository
import com.example.onespace.domain.music.repository.ExtractedAudioRepository
import com.example.onespace.domain.music.repository.MusicRepository

/** Public wiring API; repository implementations remain internal to this module. */
fun createMusicRepository(database: ContentDatabase, api: CatalogApi): MusicRepository =
    OfflineFirstMusicRepository(database.catalogItemDao(), api)

/** The music slice of the fake backend. */
fun createMusicFakeRoute(): FakeRoute = MusicFakeRoute()

fun createExtractedAudioRepository(database: ContentDatabase): ExtractedAudioRepository =
    com.example.onespace.data.music.repository.createExtractedAudioRepository(database)
