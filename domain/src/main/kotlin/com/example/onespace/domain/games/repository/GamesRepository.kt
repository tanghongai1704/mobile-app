package com.example.onespace.domain.games.repository

import com.example.onespace.domain.games.model.Game
import kotlinx.coroutines.flow.Flow

interface GamesRepository {
    fun observeCatalog(): Flow<List<Game>>
}
