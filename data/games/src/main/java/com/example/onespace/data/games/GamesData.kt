package com.example.onespace.data.games

import com.example.onespace.domain.games.repository.GamesRepository
import com.example.onespace.data.games.repository.MockGamesRepository

/** Public wiring API; repository implementations remain internal to this module. */
fun createGamesRepository(): GamesRepository = MockGamesRepository()
