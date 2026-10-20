package com.example.onespace.data.video

import com.example.onespace.domain.video.repository.VideoRepository
import com.example.onespace.data.video.repository.MockVideoRepository

/** Public wiring API; repository implementations remain internal to this module. */
fun createVideoRepository(): VideoRepository = MockVideoRepository()
