package com.example.onespace.data.news.repository

import com.example.onespace.core.database.NewsArticleDao
import com.example.onespace.core.database.NewsArticleEntity
import com.example.onespace.core.network.api.ArticleDto
import com.example.onespace.core.network.api.NewsApi
import com.example.onespace.core.network.api.QuizQuestionDto
import com.example.onespace.core.network.apiCall
import com.example.onespace.domain.news.model.Article
import com.example.onespace.domain.news.model.QuizQuestion
import com.example.onespace.domain.news.repository.NewsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Offline-first news (Section 4, sequence "Tải danh sách tin theo offline-first"):
 * Room is the single source of truth for the UI. Each subscription shows the cache at once and
 * refreshes from [NewsApi] in parallel; fresh pages are upserted into Room and reach the UI
 * through the same Flow. A failed refresh keeps the cache; only an empty cache surfaces the error.
 */
internal class OfflineFirstNewsRepository(
    private val dao: NewsArticleDao,
    private val api: NewsApi,
    private val saved: BookmarkStore,
    private val clock: () -> Long = System::currentTimeMillis
) : NewsRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val quizSerializer = ListSerializer(QuizQuestionDto.serializer())

    override fun observeCatalog(): Flow<List<Article>> = channelFlow {
        val refresh = async { runCatching { refresh() } }
        launch {
            val result = refresh.await()
            val error = result.exceptionOrNull()
            if (error != null && dao.count() == 0) throw error
        }
        dao.observeAll()
            .filter { it.isNotEmpty() }
            .map { rows -> rows.map { it.toDomain() } }
            .collect { send(it) }
    }

    override fun observeSavedIds(): Flow<Set<String>> = saved.ids

    override suspend fun toggleBookmark(id: String) = saved.toggle(id)

    /** Downloads every page of the feed, then replaces the cache in one pass. */
    suspend fun refresh() {
        val fetched = mutableListOf<ArticleDto>()
        var page = 1
        do {
            val result = apiCall { api.articles(page = page, limit = PAGE_SIZE) }
            fetched += result.items
            page++
        } while (result.hasMore && page <= MAX_PAGES)
        val now = clock()
        dao.upsertAll(fetched.mapIndexed { index, dto -> dto.toEntity(index, now) })
        dao.deleteAllExcept(fetched.map { it.id })
    }

    private fun ArticleDto.toEntity(order: Int, now: Long) = NewsArticleEntity(
        id = id,
        title = title,
        category = category,
        readTime = readTime,
        body = body,
        quizJson = json.encodeToString(quizSerializer, quiz),
        publishedAt = publishedAt,
        sortOrder = order,
        cachedAt = now
    )

    private fun NewsArticleEntity.toDomain() = Article(
        id = id,
        title = title,
        category = category,
        readTime = readTime,
        body = body,
        quiz = runCatching { json.decodeFromString(quizSerializer, quizJson) }.getOrDefault(emptyList())
            .map { QuizQuestion(it.question, it.options, it.answerIndex) }
    )

    private companion object {
        const val PAGE_SIZE = 20
        const val MAX_PAGES = 10
    }
}
