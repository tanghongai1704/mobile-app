package com.example.onespace.domain.games.usecase

import com.example.onespace.domain.games.repository.GamesRepository

class GetGames(private val repository: GamesRepository) {
    operator fun invoke() = repository.observeCatalog()
}
