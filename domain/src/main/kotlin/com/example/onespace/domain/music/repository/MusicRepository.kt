package com.example.onespace.domain.music.repository

import com.example.onespace.domain.music.model.Track
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    fun observeCatalog(): Flow<List<Track>>
}
