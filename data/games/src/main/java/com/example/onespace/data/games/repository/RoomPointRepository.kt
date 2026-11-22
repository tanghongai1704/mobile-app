package com.example.onespace.data.games.repository

import com.example.onespace.core.database.ContentDatabase
import com.example.onespace.core.database.PendingEventEntity
import com.example.onespace.core.database.PointEntryEntity
import com.example.onespace.domain.session.model.PointEntry
import com.example.onespace.domain.session.repository.PointRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room-backed implementation. Wired in app/di/RepositoryModule. */
class RoomPointRepository(
    private val database: ContentDatabase
) : PointRepository {

    private val dao = database.pointDao()

    override val totalPoints: Flow<Int> = dao.observeTotal()

    override val recentEntries: Flow<List<PointEntry>> = dao.observeRecent().map { list ->
        list.map { PointEntry(id = it.id, source = it.source, amount = it.amount, timestamp = it.timestamp) }
    }

    override suspend fun add(source: String, amount: Int) {
        val now = System.currentTimeMillis()
        dao.insert(PointEntryEntity(source = source, amount = amount, timestamp = now))
        // Queued for POST /me/events; the sync worker sends it once the user is signed in.
        database.pendingEventDao().insert(PendingEventEntity(newEventId(), source, amount, missionType = null, createdAt = now))
    }

    override suspend fun spend(amount: Int, description: String): Boolean {
        val current = dao.total()
        if (current < amount) return false
        dao.insert(PointEntryEntity(source = "spend:$description", amount = -amount, timestamp = System.currentTimeMillis()))
        return true
    }
}

internal fun newEventId(): String = "evt_" + java.util.UUID.randomUUID().toString()

fun createPointRepository(database: ContentDatabase): PointRepository = RoomPointRepository(database)
