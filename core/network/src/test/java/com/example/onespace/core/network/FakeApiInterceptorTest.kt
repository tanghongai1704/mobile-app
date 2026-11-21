package com.example.onespace.core.network

import com.example.onespace.core.network.api.ApiErrorCode
import com.example.onespace.core.network.api.NewsApi
import com.example.onespace.core.network.api.PageDto
import com.example.onespace.core.network.api.ArticleDto
import com.example.onespace.core.network.fake.FakeApiInterceptor
import com.example.onespace.core.network.fake.FakeJson
import com.example.onespace.core.network.fake.FakeRequest
import com.example.onespace.core.network.fake.FakeRoute
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class FakeApiInterceptorTest {
    private val article = ArticleDto("n1", "Tiêu đề", "Đời sống", "3 phút đọc", "Nội dung")
    private var online = true
    private val route = object : FakeRoute {
        override fun handle(request: FakeRequest) = when {
            request.method == "GET" && request.path == "news/articles" ->
                FakeJson.ok(PageDto.serializer(ArticleDto.serializer()), PageDto(listOf(article), request.query["page"]!!.toInt(), false))
            request.path.startsWith("news/articles/") ->
                FakeJson.error(409, ApiErrorCode.VERSION_CONFLICT, "Xung đột", mapOf("serverVersion" to "6"))
            else -> null
        }
    }

    private val api: NewsApi = Retrofit.Builder()
        .baseUrl("https://api.test/v1/")
        .client(OkHttpClient.Builder().addInterceptor(FakeApiInterceptor(setOf(route), { online }, 0)).build())
        .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(NewsApi::class.java)

    @Test fun pageIsDecodedThroughRetrofit() = runTest {
        val page = apiCall { api.articles(page = 2) }
        assertEquals(2, page.page)
        assertEquals("n1", page.items.single().id)
    }

    @Test fun errorEnvelopeBecomesApiException() = runTest {
        try {
            apiCall { api.article("n1") }
            fail("expected ApiException")
        } catch (e: ApiException) {
            assertEquals(409, e.httpStatus)
            assertEquals(ApiErrorCode.VERSION_CONFLICT, e.code)
            assertEquals("6", e.details["serverVersion"])
        }
    }

    @Test fun offlineBecomesNetworkUnavailable() = runTest {
        online = false
        try {
            apiCall { api.articles() }
            fail("expected NetworkUnavailableException")
        } catch (_: NetworkUnavailableException) {
        }
    }
}
