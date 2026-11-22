package com.example.onespace.core.sync

import com.example.onespace.core.database.CatalogItemDao
import com.example.onespace.core.database.CatalogItemEntity
import com.example.onespace.core.database.DailyMissionDao
import com.example.onespace.core.database.DailyMissionEntity
import com.example.onespace.core.database.PendingEventDao
import com.example.onespace.core.database.PendingEventEntity
import com.example.onespace.core.database.PlaylistSyncDao
import com.example.onespace.core.database.PlaylistSyncEntity
import com.example.onespace.core.database.VideoProgressDao
import com.example.onespace.core.database.VideoProgressEntity
import com.example.onespace.core.network.api.EventDto
import com.example.onespace.core.network.api.MeApi
import com.example.onespace.core.network.api.MissionDto
import com.example.onespace.core.network.api.PlaylistDto
import com.example.onespace.core.network.api.PointsDto
import com.example.onespace.core.network.api.ProgressDto
import com.example.onespace.core.network.api.ProgressUpdate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class SyncEngineTest {

    /** A tiny in-memory `/me` server with the same version rules as the backend. */
    private class FakeMe : MeApi {
        val playlists = linkedMapOf<String, PlaylistDto>()
        val seenEvents = mutableSetOf<String>()
        var balance = 0
        val progress = mutableMapOf<String, ProgressDto>()
        var missions = listOf<MissionDto>()
        private var nextId = 1

        private fun error(status: Int, code: String): Nothing = throw HttpException(
            Response.error<Any>(status, """{"error":{"code":"$code","message":"x"}}""".toResponseBody())
        )

        override suspend fun playlists() = playlists.values.toList()
        override suspend fun createPlaylist(body: PlaylistDto): PlaylistDto =
            body.copy(id = "pl${nextId++}", version = 1).also { playlists[it.id!!] = it }
        override suspend fun updatePlaylist(id: String, body: PlaylistDto): PlaylistDto {
            val current = playlists[id] ?: error(404, "NOT_FOUND")
            if (body.version != current.version) error(409, "VERSION_CONFLICT")
            return body.copy(id = id, version = current.version + 1).also { playlists[id] = it }
        }
        override suspend fun deletePlaylist(id: String) { playlists.remove(id) }
        override suspend fun postEvent(body: EventDto): PointsDto {
            if (body.amount > 100) error(400, "INVALID_PARAMS")
            if (seenEvents.add(body.eventId)) balance += body.amount
            return PointsDto(balance)
        }
        override suspend fun points() = PointsDto(balance)
        override suspend fun progress(since: Long) = progress.values.filter { it.updatedAt > since }
        override suspend fun saveProgress(itemId: String, body: ProgressUpdate) =
            ProgressDto(itemId, body.positionMs, 1, 5_000).also { progress[itemId] = it }
        override suspend fun missionsToday() = missions
    }

    private class Events : PendingEventDao {
        val rows = linkedMapOf<String, PendingEventEntity>()
        override suspend fun oldest(limit: Int) = rows.values.sortedBy { it.createdAt }.take(limit)
        override fun observeCount(): Flow<Int> = flowOf(rows.size)
        override suspend fun insert(event: PendingEventEntity) { rows[event.eventId] = event }
        override suspend fun delete(eventId: String) { rows.remove(eventId) }
    }

    private class Missions : DailyMissionDao {
        val rows = mutableListOf(DailyMissionEntity("listen", "Nghe 1 bài nhạc", "LISTEN", 1, date = "2026-11-22"))
        override fun observeForDate(date: String) = flowOf(rows.filter { it.date == date })
        override suspend fun forDate(date: String) = rows.filter { it.date == date }
        override suspend fun upsertAll(missions: List<DailyMissionEntity>) { rows += missions }
        override suspend fun updateProgress(id: String, date: String, progress: Int, completed: Boolean) {
            rows.replaceAll { if (it.id == id && it.date == date) it.copy(progress = progress, completed = completed) else it }
        }
    }

    private class Progress : VideoProgressDao {
        val rows = mutableMapOf<String, VideoProgressEntity>()
        override fun observeRecent() = flowOf(rows.values.toList())
        override suspend fun find(videoId: String) = rows[videoId]
        override suspend fun positionOf(videoId: String) = rows[videoId]?.positionMs
        override suspend fun upsert(entity: VideoProgressEntity) { rows[entity.videoId] = entity }
        override suspend fun delete(videoId: String) { rows.remove(videoId) }
        override suspend fun changedSince(since: Long) = rows.values.filter { it.updatedAt > since }.sortedBy { it.updatedAt }
    }

    private class Catalog : CatalogItemDao {
        override fun observeByType(type: String) = flowOf(emptyList<CatalogItemEntity>())
        override suspend fun count(type: String) = 0
        override suspend fun find(id: String) = CatalogItemEntity(id, "video", "Tên $id", "", "", "", "", "[]", null, 0, 0)
        override suspend fun upsertAll(items: List<CatalogItemEntity>) {}
        override suspend fun deleteAllExcept(type: String, keepIds: List<String>) {}
    }

    private class States : PlaylistSyncDao {
        val rows = mutableMapOf<String, PlaylistSyncEntity>()
        override suspend fun all() = rows.values.toList()
        override suspend fun upsert(state: PlaylistSyncEntity) { rows[state.name] = state }
        override suspend fun delete(name: String) { rows.remove(name) }
        override suspend fun clear() = rows.clear()
    }

    private class Local : PlaylistPort {
        val map = linkedMapOf<String, List<String>>()
        override suspend fun local(): Map<String, List<String>> = map.toMap()
        override suspend fun put(name: String, ids: List<String>) { map[name] = ids }
    }

    private class Cursor : SyncCursor {
        override var owner: String? = null
        override var progressPushedUntil = 0L
        override var progressPulledUntil = 0L
    }

    private val server = FakeMe()
    private val events = Events()
    private val missions = Missions()
    private val progress = Progress()
    private val states = States()
    private val local = Local()
    private val engine = SyncEngine(server, events, missions, progress, Catalog(), states, local, Cursor(), today = { "2026-11-22" })

    @Test fun `outbox is sent once and rejected events are dropped`() = runTest {
        events.insert(PendingEventEntity("e1", "game", 10, null, 1))
        events.insert(PendingEventEntity("e2", "music", 5, "LISTEN", 2))
        events.insert(PendingEventEntity("bad", "cheat", 999, null, 3))
        val report = engine.run("a@b.vn")
        assertEquals(2, report.eventsSent)
        assertEquals(1, report.eventsRejected)
        assertTrue(events.rows.isEmpty())
        assertEquals(15, server.balance)

        // A retry of an event the server already counted does not add points twice.
        events.insert(PendingEventEntity("e1", "game", 10, null, 4))
        engine.run("a@b.vn")
        assertEquals(15, server.balance)
    }

    @Test fun `mission progress from the server only moves forward`() = runTest {
        server.missions = listOf(MissionDto("u:2026-11-22:LISTEN", "Nghe", "LISTEN", 1, 1, true, "2026-11-22"))
        engine.run("a@b.vn")
        assertTrue(missions.rows.single().completed)
    }

    @Test fun `progress is pushed and new server rows are pulled`() = runTest {
        progress.upsert(VideoProgressEntity("v1", "Một", 4_000, 100))
        server.progress["v2"] = ProgressDto("v2", 9_000, 1, 200)
        val report = engine.run("a@b.vn")
        assertEquals(4_000, server.progress["v1"]!!.positionMs)
        assertEquals(1, report.progressPulled)
        assertEquals("Tên v2", progress.rows["v2"]!!.title)
    }

    @Test fun `playlist conflict merges both sides`() = runTest {
        local.map["Sáng"] = listOf("m1", "v1")
        engine.run("a@b.vn")
        val id = server.playlists.values.single().id!!
        assertEquals(1, server.playlists[id]!!.version)

        // Another device writes first (server version 2), then this device edits its copy.
        server.playlists[id] = server.playlists[id]!!.copy(items = listOf("m1", "v1", "n1"), version = 2)
        local.map["Sáng"] = listOf("m1", "v1", "m2")
        val report = engine.run("a@b.vn")

        assertEquals(1, report.conflictsMerged)
        assertEquals(listOf("m1", "v1", "m2", "n1"), server.playlists[id]!!.items)
        assertEquals(3, server.playlists[id]!!.version)
        assertEquals(listOf("m1", "v1", "m2", "n1"), local.map["Sáng"])
    }

    @Test fun `server playlists reach the device and local deletes reach the server`() = runTest {
        server.playlists["pl9"] = PlaylistDto("pl9", "Từ máy khác", listOf("m3"), 4)
        engine.run("a@b.vn")
        assertEquals(listOf("m3"), local.map["Từ máy khác"])

        local.map.remove("Từ máy khác")
        engine.run("a@b.vn")
        assertTrue(server.playlists.isEmpty())
    }
}
