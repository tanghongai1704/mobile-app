package com.example.onespace.data.games.repository

import com.example.onespace.domain.games.model.Game
import com.example.onespace.domain.games.repository.GamesRepository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

/** Bundled demo data. Replace this binding, not the UI, when integrating real sources. */
internal class MockGamesRepository : GamesRepository {
    override fun observeCatalog() = flow {
        delay(400)
        emit(listOf(
        Game("g1", "2048", "Tập trung chút, số lớn hơn.", 2048),
        Game("g2", "Tic-Tac-Toe", "Trò cổ điển cho hai người.", 12)
        ))
    }
}
