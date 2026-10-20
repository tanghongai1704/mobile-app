package com.example.onespace.domain.music.usecase

import com.example.onespace.domain.music.repository.MusicRepository

class GetMusic(private val repository: MusicRepository) {
    operator fun invoke() = repository.observeCatalog()
}
