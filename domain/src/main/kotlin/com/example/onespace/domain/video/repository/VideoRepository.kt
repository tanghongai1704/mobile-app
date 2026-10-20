package com.example.onespace.domain.video.repository

import com.example.onespace.domain.video.model.Video
import kotlinx.coroutines.flow.Flow

interface VideoRepository {
    fun observeCatalog(): Flow<List<Video>>
}
