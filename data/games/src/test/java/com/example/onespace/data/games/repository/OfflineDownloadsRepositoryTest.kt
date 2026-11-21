package com.example.onespace.data.games.repository

import com.example.onespace.domain.music.model.Track
import com.example.onespace.domain.music.repository.MusicRepository
import com.example.onespace.domain.session.port.OfflineMediaStore
import com.example.onespace.domain.session.repository.DownloadedContent
import com.example.onespace.domain.session.repository.DownloadedContentRepository
import com.example.onespace.domain.video.model.Video
import com.example.onespace.domain.video.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineDownloadsRepositoryTest {
    private class MemoryEntries : DownloadedContentRepository {
        val rows = MutableStateFlow<List<DownloadedContent>>(emptyList())
        override fun observeAll() = rows
        override fun observeByType(type: String) = rows.map { list -> list.filter { it.type == type } }
        override suspend fun add(contentId: String, type: String, title: String, subtitle: String) {
            rows.value = rows.value + DownloadedContent(contentId, type, title, subtitle)
        }
        override suspend fun remove(contentId: String) { rows.value = rows.value.filterNot { it.contentId == contentId } }
    }

    private class MemoryFiles : OfflineMediaStore {
        val files = mutableMapOf<String, String>()
        var failing = false
        override suspend fun save(contentId: String, url: String): Boolean {
            if (failing) return false
            files[contentId] = url
            return true
        }
        override suspend fun delete(contentId: String) { files.remove(contentId) }
        override fun localUri(contentId: String) = files[contentId]?.let { "file:///offline/$contentId" }
    }

    private val entries = MemoryEntries()
    private val files = MemoryFiles()
    private val repository = OfflineDownloadsRepository(
        entries, files,
        object : VideoRepository { override fun observeCatalog() = flowOf(listOf(Video("v1", "V", "S", "1:00", "https://x/v1.mp4"))) },
        object : MusicRepository { override fun observeCatalog() = flowOf(listOf(Track("m1", "M", "A", "3:00", "https://x/m1.mp3"))) }
    )

    @Test fun downloadSavesTheFileBeforeListingIt() = runBlocking {
        repository.add("m1", "music", "M", "A")
        assertEquals("https://x/m1.mp3", files.files["m1"])
        assertEquals(listOf("m1"), repository.observeByType("music").first().map { it.contentId })
    }

    @Test fun failedDownloadIsNotListed() = runBlocking {
        files.failing = true
        repository.add("v1", "video", "V", "S")
        assertTrue(repository.observeAll().first().isEmpty())
    }

    @Test fun removingDeletesTheFile() = runBlocking {
        repository.add("v1", "video", "V", "S")
        repository.remove("v1")
        assertNull(files.localUri("v1"))
        assertTrue(repository.observeAll().first().isEmpty())
    }

    @Test fun newsIsListedWithoutAFile() = runBlocking {
        repository.add("n1", "news", "Tin", "Đời sống")
        assertTrue(files.files.isEmpty())
        assertEquals(1, repository.observeAll().first().size)
    }
}
