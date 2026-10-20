package com.example.onespace.data.video.repository

import com.example.onespace.domain.video.model.Video
import com.example.onespace.domain.video.repository.VideoRepository

import kotlinx.coroutines.flow.flowOf

/** Bundled demo data. Replace this binding, not the UI, when integrating real sources. */
internal class MockVideoRepository : VideoRepository {
    override fun observeCatalog() = flowOf(listOf(
        Video("v1", "A little closer to nature", "OneSpace Studio", "12:48"),
        Video("v2", "Saigon, after hours", "City Stories", "08:24"),
        Video("v3", "The art of slowing down", "Slow Living", "16:05")
    ))
}
