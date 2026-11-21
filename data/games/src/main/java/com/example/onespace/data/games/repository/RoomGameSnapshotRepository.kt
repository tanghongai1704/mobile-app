package com.example.onespace.data.games.repository

import com.example.onespace.core.database.ContentDatabase
import com.example.onespace.core.database.GameSnapshotDao
import com.example.onespace.core.database.GameSnapshotEntity
import com.example.onespace.domain.games.repository.GameSnapshot
import com.example.onespace.domain.games.repository.GameSnapshotRepository

/** Room-backed saved rounds (table `game_snapshots`). Wired in app/di/RepositoryModule. */
internal class RoomGameSnapshotRepository(
    private val dao: GameSnapshotDao,
    private val clock: () -> Long = System::currentTimeMillis
) : GameSnapshotRepository {

    override suspend fun load(gameId: String): GameSnapshot? = dao.find(gameId)?.let { row ->
        val board = row.board.split(',').mapNotNull { it.trim().toIntOrNull() }
        GameSnapshot(row.gameId, row.version, board, row.score, row.reachedTarget, row.savedAt)
    }

    override suspend fun save(snapshot: GameSnapshot) = dao.upsert(
        GameSnapshotEntity(
            gameId = snapshot.gameId,
            version = snapshot.version,
            board = snapshot.board.joinToString(","),
            score = snapshot.score,
            reachedTarget = snapshot.reachedTarget,
            savedAt = clock()
        )
    )

    override suspend fun clear(gameId: String) = dao.delete(gameId)
}

fun createGameSnapshotRepository(database: ContentDatabase): GameSnapshotRepository =
    RoomGameSnapshotRepository(database.gameSnapshotDao())
