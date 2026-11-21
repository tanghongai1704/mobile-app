package com.example.onespace.domain.games.repository

/**
 * An unfinished round saved so the player can continue after the app is closed or killed.
 * [version] is the rules version of the game; a snapshot from other rules is discarded.
 */
data class GameSnapshot(
    val gameId: String,
    val version: Int,
    val board: List<Int>,
    val score: Int,
    val reachedTarget: Boolean = false,
    val savedAt: Long = 0L
)

interface GameSnapshotRepository {
    suspend fun load(gameId: String): GameSnapshot?
    suspend fun save(snapshot: GameSnapshot)
    suspend fun clear(gameId: String)
}
