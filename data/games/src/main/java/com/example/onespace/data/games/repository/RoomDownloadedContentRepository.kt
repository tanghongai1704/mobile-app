package com.example.onespace.data.games.repository

import com.example.onespace.core.database.ContentDatabase
import com.example.onespace.core.database.DownloadedContentEntity
import com.example.onespace.domain.session.repository.DownloadedContent
import com.example.onespace.domain.session.repository.DownloadedContentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** F-VID-07 / F-MUS-12: downloaded flags on top of the shared Room database (ADR-09). */
internal class RoomDownloadedContentRepository(
    private val database: ContentDatabase
) : DownloadedContentRepository {

    override fun observeAll(): Flow<List<DownloadedContent>> =
        database.downloadedContentDao().observeAll().map(::toDomain)

    override fun observeByType(type: String): Flow<List<DownloadedContent>> =
        database.downloadedContentDao().observeByType(type).map(::toDomain)

    override suspend fun add(contentId: String, type: String, title: String, subtitle: String) =
        database.downloadedContentDao().upsert(
            DownloadedContentEntity(contentId, type, title, subtitle, System.currentTimeMillis())
        )

    override suspend fun remove(contentId: String) =
        database.downloadedContentDao().delete(contentId)

    private fun toDomain(list: List<DownloadedContentEntity>) =
        list.map { DownloadedContent(it.contentId, it.type, it.title, it.subtitle) }
}

fun createDownloadedContentRepository(database: ContentDatabase): DownloadedContentRepository =
    RoomDownloadedContentRepository(database)
