package com.example.onespace.data.video

import com.example.onespace.core.database.ContentDatabase
import com.example.onespace.core.network.api.CatalogApi
import com.example.onespace.core.network.fake.FakeRoute
import com.example.onespace.data.video.repository.OfflineFirstVideoRepository
import com.example.onespace.data.video.repository.VideoFakeRoute
import com.example.onespace.data.video.repository.ViewingSessionRepositoryImpl
import com.example.onespace.domain.video.repository.VideoRepository
import com.example.onespace.domain.video.repository.ViewingSessionRepository

/** Public wiring API; repository implementations remain internal to this module. */
fun createVideoRepository(database: ContentDatabase, api: CatalogApi): VideoRepository =
    OfflineFirstVideoRepository(database.catalogItemDao(), api)

/** The video slice of the fake backend. */
fun createVideoFakeRoute(): FakeRoute = VideoFakeRoute()

fun createViewingSessionRepository(database: ContentDatabase): ViewingSessionRepository =
    ViewingSessionRepositoryImpl(database)
