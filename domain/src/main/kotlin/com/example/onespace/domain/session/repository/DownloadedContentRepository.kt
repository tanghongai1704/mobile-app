package com.example.onespace.domain.session.repository

import kotlinx.coroutines.flow.Flow

/** F-VID-07 / F-MUS-12: one entry per "downloaded" content item (video | music | news). */
data class DownloadedContent(
    val contentId: String,
    val type: String,
    val title: String,
    val subtitle: String
)

interface DownloadedContentRepository {
    fun observeAll(): Flow<List<DownloadedContent>>
    fun observeByType(type: String): Flow<List<DownloadedContent>>
    suspend fun add(contentId: String, type: String, title: String, subtitle: String)
    suspend fun remove(contentId: String)
}
