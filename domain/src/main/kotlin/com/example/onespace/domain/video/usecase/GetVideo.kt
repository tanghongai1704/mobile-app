package com.example.onespace.domain.video.usecase

import com.example.onespace.domain.video.repository.VideoRepository

class GetVideo(private val repository: VideoRepository) {
    operator fun invoke() = repository.observeCatalog()
}
