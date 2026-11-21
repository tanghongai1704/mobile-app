package com.example.onespace.data.news.repository

import com.example.onespace.core.database.NewsArticleDao
import com.example.onespace.core.database.NewsArticleEntity
import com.example.onespace.core.network.NetworkUnavailableException
import com.example.onespace.core.network.api.ArticleDto
import com.example.onespace.core.network.api.NewsApi
import com.example.onespace.core.network.api.PageDto
import com.example.onespace.core.network.fake.FakeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

private class InMemoryNewsDao : NewsArticleDao {
    val rows = MutableStateFlow<Map<String, NewsArticleEntity>>(emptyMap())
    override fun observeAll() = rows.map { it.values.sortedBy(NewsArticleEntity::sortOrder) }
    override suspend fun count() = rows.value.size
    override suspend fun upsertAll(articles: List<NewsArticleEntity>) {
        rows.value = rows.value + articles.associateBy { it.id }
    }
    override suspend fun deleteAllExcept(keepIds: List<String>) {
        rows.value = rows.value.filterKeys { it in keepIds }
    }
}

private class FakeNewsApi : NewsApi {
    var online = true
    var feed = (1..25).map { ArticleDto("n$it", "Bài $it", "Đời sống", "3 phút đọc", "Nội dung $it") }
    override suspend fun articles(page: Int, limit: Int): PageDto<ArticleDto> {
        if (!online) throw NetworkUnavailableException(IOException("offline"))
        val from = (page - 1) * limit
        val items = feed.drop(from).take(limit)
        return PageDto(items, page, from + items.size < feed.size)
    }
    override suspend fun article(id: String) = feed.first { it.id == id }
}

class OfflineFirstNewsRepositoryTest {
    private val dao = InMemoryNewsDao()
    private val api = FakeNewsApi()
    private val repository = OfflineFirstNewsRepository(dao, api, saved = object : BookmarkStore {
        override val ids = MutableStateFlow(emptySet<String>())
        override suspend fun toggle(id: String) { ids.value = ids.value + id }
    })

    @Test fun refreshWalksEveryPageIntoRoom() = runTest {
        repository.refresh()
        assertEquals(25, dao.count())
        assertEquals("n1", dao.observeAll().first().first().id)
    }

    @Test fun offlineKeepsTheCache() = runTest {
        repository.refresh()
        api.online = false
        val articles = repository.observeCatalog().first()
        assertEquals(25, articles.size)
    }

    @Test fun offlineWithEmptyCacheSurfacesTheError() = runTest {
        api.online = false
        try {
            repository.observeCatalog().first()
            fail("expected an error for the empty cache")
        } catch (e: NetworkUnavailableException) {
            assertTrue(e.message!!.isNotEmpty())
        }
    }

    @Test fun articlesRemovedOnServerLeaveTheCache() = runTest {
        repository.refresh()
        api.feed = api.feed.take(3)
        repository.refresh()
        assertEquals(setOf("n1", "n2", "n3"), dao.rows.value.keys)
    }

    @Test fun fakeRouteServesTheContractShape() {
        val route = NewsFakeRoute()
        val page = route.handle(FakeRequest("GET", "news/articles", mapOf("page" to "1", "limit" to "5"), null, null))!!
        assertEquals(200, page.status)
        assertTrue(page.body.contains("\"hasMore\":true"))
        val missing = route.handle(FakeRequest("GET", "news/articles/zzz", emptyMap(), null, null))!!
        assertEquals(404, missing.status)
        assertTrue(missing.body.contains("NOT_FOUND"))
    }
}
