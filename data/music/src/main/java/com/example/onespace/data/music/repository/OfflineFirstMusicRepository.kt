package com.example.onespace.data.music.repository

import com.example.onespace.core.database.CatalogItemDao
import com.example.onespace.core.database.CatalogItemEntity
import com.example.onespace.core.network.api.CatalogApi
import com.example.onespace.core.network.api.TrackDto
import com.example.onespace.core.network.apiCall
import com.example.onespace.domain.music.model.Track
import com.example.onespace.domain.music.repository.MusicRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Offline-first music catalogue: Room (`catalog_items`, type = music) feeds the UI and
 * `GET /catalog/tracks` refreshes it. Lyrics and BPM travel with the track.
 */
internal class OfflineFirstMusicRepository(
    private val dao: CatalogItemDao,
    private val api: CatalogApi,
    private val clock: () -> Long = System::currentTimeMillis
) : MusicRepository {

    private val lines = ListSerializer(String.serializer())

    override fun observeCatalog(): Flow<List<Track>> = channelFlow {
        val refresh = async { runCatching { refresh() } }
        launch {
            val error = refresh.await().exceptionOrNull()
            if (error != null && dao.count(TYPE) == 0) throw error
        }
        dao.observeByType(TYPE)
            .filter { it.isNotEmpty() }
            .map { rows -> rows.map { it.toDomain() } }
            .collect { send(it) }
    }

    suspend fun refresh() {
        val fetched = mutableListOf<TrackDto>()
        var page = 1
        do {
            val result = apiCall { api.tracks(page = page, limit = PAGE_SIZE) }
            fetched += result.items
            page++
        } while (result.hasMore && page <= MAX_PAGES)
        val now = clock()
        dao.upsertAll(fetched.mapIndexed { i, dto -> dto.toEntity(i, now) })
        dao.deleteAllExcept(TYPE, fetched.map { it.id })
    }

    private fun TrackDto.toEntity(order: Int, now: Long) = CatalogItemEntity(
        id = id, type = TYPE, title = title, creator = artist,
        duration = durationLabel.ifEmpty { "%d:%02d".format(duration / 60, duration % 60) }, url = url, topic = "",
        lyricsJson = Json.encodeToString(lines, lyrics), bpm = bpm, sortOrder = order, cachedAt = now
    )

    private fun CatalogItemEntity.toDomain() = Track(
        id = id, title = title, artist = creator, duration = duration, url = url,
        lyrics = runCatching { Json.decodeFromString(lines, lyricsJson) }.getOrDefault(emptyList()),
        bpm = bpm
    )

    private companion object {
        const val TYPE = "music"
        const val PAGE_SIZE = 50
        const val MAX_PAGES = 10
    }
}
