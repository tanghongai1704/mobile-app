package com.example.onespace.data.games.repository

import com.example.onespace.domain.games.model.Game
import com.example.onespace.domain.games.repository.GamesRepository

import kotlinx.coroutines.flow.flowOf

/** Bundled demo data. Replace this binding, not the UI, when integrating real sources. */
internal class MockGamesRepository : GamesRepository {
    override fun observeCatalog() = flowOf(listOf(
        Game("g1", "2048", "A little focus. A bigger number.", 2048),
        Game("g2", "Tic-Tac-Toe", "A classic break for two.", 12)
    ))
}
