package com.example.onespace.core.sync

import com.example.onespace.core.database.DailyMissionDao
import com.example.onespace.core.database.CatalogItemDao
import com.example.onespace.core.database.PendingEventDao
import com.example.onespace.core.database.PlaylistSyncDao
import com.example.onespace.core.database.PlaylistSyncEntity
import com.example.onespace.core.database.VideoProgressDao
import com.example.onespace.core.database.VideoProgressEntity
import com.example.onespace.core.network.ApiException
import com.example.onespace.core.network.api.EventDto
import com.example.onespace.core.network.api.MeApi
import com.example.onespace.core.network.api.PlaylistDto
import com.example.onespace.core.network.api.ProgressUpdate
import com.example.onespace.core.network.apiCall

/** The playlists the user sees, keyed by name, each as its mixed play order of item ids. */
interface PlaylistPort {
    suspend fun local(): Map<String, List<String>>
    suspend fun put(name: String, ids: List<String>)
}

/** Where the engine remembers how far progress has been pushed and pulled, per signed-in user. */
interface SyncCursor {
    var owner: String?
    var progressPushedUntil: Long
    var progressPulledUntil: Long
}

data class SyncReport(
    val eventsSent: Int = 0,
    val eventsRejected: Int = 0,
    val progressPushed: Int = 0,
    val progressPulled: Int = 0,
    val playlistsPushed: Int = 0,
    val playlistsPulled: Int = 0,
    val conflictsMerged: Int = 0
)

/**
 * One sync pass for a signed-in user (Section 4, "Đồng bộ dữ liệu"). Room stays the source of
 * truth on the device; this class only moves changes both ways through [MeApi]:
 *  1. the event outbox → `POST /me/events` (idempotent by eventId, so retries are safe);
 *  2. today's missions ← `GET /me/missions/today` (progress only ever moves forward);
 *  3. video positions ↔ `/me/progress` (last write wins by time);
 *  4. playlists ↔ `/me/playlists` with optimistic versions: a 409 VERSION_CONFLICT is resolved by
 *     merging both item lists (local order first) and writing again on the server's version.
 * Network errors propagate so WorkManager retries later; a 4xx on one item skips only that item.
 */
class SyncEngine(
    private val api: MeApi,
    private val events: PendingEventDao,
    private val missions: DailyMissionDao,
    private val progress: VideoProgressDao,
    private val catalog: CatalogItemDao,
    private val playlistState: PlaylistSyncDao,
    private val playlists: PlaylistPort,
    private val cursor: SyncCursor,
    private val today: () -> String = { java.time.LocalDate.now().toString() }
) {

    suspend fun run(user: String): SyncReport {
        if (cursor.owner != user) {
            // Another account signed in on this device: its server copy is unrelated to ours.
            playlistState.clear()
            cursor.owner = user
            cursor.progressPushedUntil = 0
            cursor.progressPulledUntil = 0
        }
        var report = SyncReport()
        report = sendEvents(report)
        mergeMissions()
        report = syncProgress(report)
        report = syncPlaylists(report)
        return report
    }

    private suspend fun sendEvents(start: SyncReport): SyncReport {
        var report = start
        while (true) {
            val batch = events.oldest(BATCH)
            if (batch.isEmpty()) return report
            for (e in batch) {
                try {
                    apiCall { api.postEvent(EventDto(e.eventId, e.source, e.amount, e.missionType)) }
                    report = report.copy(eventsSent = report.eventsSent + 1)
                } catch (error: ApiException) {
                    // 401 means the session ended: keep the event for the next sign-in.
                    if (error.httpStatus == 401 || error.httpStatus == 429 || error.httpStatus >= 500) throw error
                    report = report.copy(eventsRejected = report.eventsRejected + 1)
                }
                events.delete(e.eventId)
            }
        }
    }

    private suspend fun mergeMissions() {
        val server = apiCall { api.missionsToday() }
        val date = today()
        val local = missions.forDate(date)
        for (m in server) {
            val mine = local.firstOrNull { it.targetType == m.targetType } ?: continue
            if (m.progress > mine.progress) {
                val progress = m.progress.coerceAtMost(mine.target)
                missions.updateProgress(mine.id, date, progress, mine.completed || progress >= mine.target)
            }
        }
    }

    private suspend fun syncProgress(start: SyncReport): SyncReport {
        var pushed = 0
        var pulled = 0
        val changed = progress.changedSince(cursor.progressPushedUntil)
        for (row in changed) {
            try {
                apiCall { api.saveProgress(row.videoId, ProgressUpdate(row.positionMs)) }
                pushed++
            } catch (error: ApiException) {
                if (error.httpStatus == 401 || error.httpStatus >= 500) throw error
            }
            cursor.progressPushedUntil = row.updatedAt
        }
        val remote = apiCall { api.progress(since = cursor.progressPulledUntil) }
        for (r in remote) {
            val local = progress.find(r.itemId)
            if (local == null || (local.positionMs != r.positionMs && r.updatedAt > local.updatedAt)) {
                val title = local?.title ?: catalog.find(r.itemId)?.title.orEmpty()
                progress.upsert(VideoProgressEntity(r.itemId, title, r.positionMs, r.updatedAt))
                pulled++
            }
            cursor.progressPulledUntil = maxOf(cursor.progressPulledUntil, r.updatedAt)
        }
        return start.copy(progressPushed = pushed, progressPulled = pulled)
    }

    private suspend fun syncPlaylists(start: SyncReport): SyncReport {
        var pushed = 0
        var pulled = 0
        var merged = 0
        val remote = apiCall { api.playlists() }.associateBy { it.name }
        val states = playlistState.all().associateBy { it.name }
        val local = playlists.local()

        suspend fun save(p: PlaylistDto) =
            playlistState.upsert(PlaylistSyncEntity(p.name, p.id!!, p.version, hashOf(p.items)))

        suspend fun mergeInto(name: String, ids: List<String>, server: PlaylistDto) {
            val union = (ids + server.items).distinct()
            val written = apiCall { api.updatePlaylist(server.id!!, PlaylistDto(name = name, items = union, version = server.version)) }
            if (union != ids) playlists.put(name, union)
            save(written)
            merged++
        }

        for ((name, ids) in local) {
            val state = states[name]
            val server = remote[name]
            try {
                when {
                    server == null -> {
                        save(apiCall { api.createPlaylist(PlaylistDto(name = name, items = ids)) })
                        pushed++
                    }
                    // Same name made on another device before this one ever synced it.
                    state == null -> mergeInto(name, ids, server)
                    hashOf(ids) != state.itemsHash -> try {
                        save(apiCall { api.updatePlaylist(server.id!!, PlaylistDto(name = name, items = ids, version = state.version)) })
                        pushed++
                    } catch (conflict: ApiException) {
                        if (conflict.code != "VERSION_CONFLICT") throw conflict
                        mergeInto(name, ids, server)
                    }
                    server.version != state.version -> {
                        playlists.put(name, server.items)
                        save(server)
                        pulled++
                    }
                }
            } catch (error: ApiException) {
                // e.g. an item the server does not know: skip this playlist, sync the rest.
                if (error.httpStatus == 401 || error.httpStatus >= 500) throw error
            }
        }
        for (name in states.keys - local.keys) {
            remote[name]?.id?.let { id -> runCatching { apiCall { api.deletePlaylist(id) } } }
            playlistState.delete(name)
        }
        for ((name, server) in remote) {
            if (name !in local && name !in states) {
                playlists.put(name, server.items)
                save(server)
                pulled++
            }
        }
        return start.copy(playlistsPushed = pushed, playlistsPulled = pulled, conflictsMerged = merged)
    }

    private fun hashOf(ids: List<String>): String = ids.joinToString("\u0001").hashCode().toString(16)

    private companion object {
        const val BATCH = 50
    }
}
