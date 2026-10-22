package com.example.onespace.data.video

import com.example.onespace.core.database.ContentDatabase
import com.example.onespace.domain.video.repository.VideoRepository
import com.example.onespace.domain.video.repository.ViewingSessionRepository
import com.example.onespace.data.video.repository.MockVideoRepository
import com.example.onespace.data.video.repository.ViewingSessionRepositoryImpl

/** Public wiring API; repository implementations remain internal to this module. */
fun createVideoRepository(): VideoRepository = MockVideoRepository()

fun createViewingSessionRepository(database: ContentDatabase): ViewingSessionRepository =
    ViewingSessionRepositoryImpl(database)
