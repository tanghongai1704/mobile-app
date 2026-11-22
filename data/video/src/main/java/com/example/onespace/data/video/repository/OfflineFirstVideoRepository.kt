package com.example.onespace.data.video.repository

import com.example.onespace.core.database.CatalogItemDao
import com.example.onespace.core.database.CatalogItemEntity
import com.example.onespace.core.network.api.CatalogApi
import com.example.onespace.core.network.api.VideoDto
import com.example.onespace.core.network.apiCall
import com.example.onespace.domain.video.model.Video
import com.example.onespace.domain.video.repository.VideoRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Offline-first video catalogue, same pattern as news: Room (`catalog_items`, type = video) is
 * what the UI reads; `GET /catalog/videos` refreshes it in the background. A failed refresh keeps
 * the cache; only an empty cache surfaces the error.
 */
internal class OfflineFirstVideoRepository(
    private val dao: CatalogItemDao,
    private val api: CatalogApi,
    private val clock: () -> Long = System::currentTimeMillis
) : VideoRepository {

    override fun observeCatalog(): Flow<List<Video>> = channelFlow {
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
        val fetched = mutableListOf<VideoDto>()
        var page = 1
        do {
            val result = apiCall { api.videos(page = page, limit = PAGE_SIZE) }
            fetched += result.items
            page++
        } while (result.hasMore && page <= MAX_PAGES)
        val now = clock()
        dao.upsertAll(fetched.mapIndexed { i, dto -> dto.toEntity(i, now) })
        dao.deleteAllExcept(TYPE, fetched.map { it.id })
    }

    private fun VideoDto.toEntity(order: Int, now: Long) = CatalogItemEntity(
        id = id, type = TYPE, title = title, creator = creator,
        duration = durationLabel.ifEmpty { formatSeconds(duration) }, url = url, topic = topic,
        lyricsJson = "[]", bpm = null, sortOrder = order, cachedAt = now
    )

    private fun CatalogItemEntity.toDomain() = Video(id, title, creator, duration, url, topic)

    private companion object {
        const val TYPE = "video"
        const val PAGE_SIZE = 50
        const val MAX_PAGES = 10
    }
}

internal fun formatSeconds(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

internal fun parseDuration(label: String): Int =
    label.split(':').fold(0) { acc, part -> acc * 60 + (part.trim().toIntOrNull() ?: 0) }
