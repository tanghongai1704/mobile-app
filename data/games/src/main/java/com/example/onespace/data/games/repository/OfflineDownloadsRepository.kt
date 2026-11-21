package com.example.onespace.data.games.repository

import com.example.onespace.domain.music.repository.MusicRepository
import com.example.onespace.domain.session.port.OfflineMediaStore
import com.example.onespace.domain.session.repository.DownloadedContent
import com.example.onespace.domain.session.repository.DownloadedContentRepository
import com.example.onespace.domain.video.repository.VideoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

/**
 * F-VID-07 / F-MUS-12: "Tải về" really downloads. Video and music files are saved by
 * [OfflineMediaStore] before the entry is recorded, so the Downloads list only shows what can play
 * offline; removing an entry deletes its file. News stays a bookmark-like entry because articles are
 * already cached for offline reading.
 */
internal class OfflineDownloadsRepository(
    private val entries: DownloadedContentRepository,
    private val files: OfflineMediaStore,
    private val videos: VideoRepository,
    private val music: MusicRepository
) : DownloadedContentRepository {

    override fun observeAll(): Flow<List<DownloadedContent>> = entries.observeAll()

    override fun observeByType(type: String): Flow<List<DownloadedContent>> = entries.observeByType(type)

    override suspend fun add(contentId: String, type: String, title: String, subtitle: String) {
        val url = when (type) {
            "video" -> videos.observeCatalog().firstOrNull()?.firstOrNull { it.id == contentId }?.url
            "music" -> music.observeCatalog().firstOrNull()?.firstOrNull { it.id == contentId }?.url
            else -> null
        }
        if (url != null && !files.save(contentId, url)) return // nothing playable offline: do not list it
        entries.add(contentId, type, title, subtitle)
    }

    override suspend fun remove(contentId: String) {
        files.delete(contentId)
        entries.remove(contentId)
    }
}

fun createOfflineDownloadsRepository(
    entries: DownloadedContentRepository,
    files: OfflineMediaStore,
    videos: VideoRepository,
    music: MusicRepository
): DownloadedContentRepository = OfflineDownloadsRepository(entries, files, videos, music)
