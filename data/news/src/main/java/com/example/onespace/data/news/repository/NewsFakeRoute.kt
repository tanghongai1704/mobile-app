package com.example.onespace.data.news.repository

import com.example.onespace.core.network.api.ApiErrorCode
import com.example.onespace.core.network.api.ArticleDto
import com.example.onespace.core.network.api.PageDto
import com.example.onespace.core.network.api.QuizQuestionDto
import com.example.onespace.core.network.fake.FakeJson
import com.example.onespace.core.network.fake.FakeRequest
import com.example.onespace.core.network.fake.FakeResponse
import com.example.onespace.core.network.fake.FakeRoute

/** Server side of `GET /news/articles` and `GET /news/articles/{id}` for the fake backend. */
internal class NewsFakeRoute(
    private val articles: () -> List<ArticleDto> = { NewsSeed.catalog.mapIndexed { i, a -> a.toDto(i) } }
) : FakeRoute {

    override fun handle(request: FakeRequest): FakeResponse? {
        if (request.method != "GET" || !request.path.startsWith("news/articles")) return null
        val all = articles()
        val id = request.path.removePrefix("news/articles").trim('/')
        if (id.isNotEmpty()) {
            val article = all.firstOrNull { it.id == id }
                ?: return FakeJson.error(404, ApiErrorCode.NOT_FOUND, "Không tìm thấy bài viết")
            return FakeJson.ok(ArticleDto.serializer(), article)
        }
        val page = request.query["page"]?.toIntOrNull() ?: 1
        val limit = request.query["limit"]?.toIntOrNull() ?: 20
        if (page < 1 || limit !in 1..100) {
            return FakeJson.error(400, ApiErrorCode.INVALID_PARAMS, "Tham số phân trang không hợp lệ")
        }
        val from = (page - 1) * limit
        val items = all.drop(from).take(limit)
        return FakeJson.ok(
            PageDto.serializer(ArticleDto.serializer()),
            PageDto(items, page, hasMore = from + items.size < all.size)
        )
    }
}

private const val BASE_PUBLISHED_AT = 1_793_520_000_000L // 2026-11-01, newest first

internal fun com.example.onespace.domain.news.model.Article.toDto(index: Int) = ArticleDto(
    id = id,
    title = title,
    category = category,
    readTime = readTime,
    body = body,
    publishedAt = BASE_PUBLISHED_AT - index * 3_600_000L,
    quiz = quiz.map { QuizQuestionDto(it.question, it.options, it.correctIndex) }
)
