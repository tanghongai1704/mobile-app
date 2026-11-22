package com.example.onespace.backend

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApiTest {
    private var clock = 1_795_000_000_000L

    private fun api(block: suspend ApplicationTestBuilder.(HttpClient) -> Unit) = testApplication {
        val db = Db.init(DbConfig(url = "jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1"))
        application {
            module(CatalogService(db, "http://test/media"), AuthService(db, now = { clock }), MeService(db, now = { clock }))
        }
        val client = createClient { install(ContentNegotiation) { json(ApiJson) } }
        block(client)
    }

    private suspend fun HttpClient.signIn(identity: String = "0901234567"): String {
        post("/v1/auth/otp") { contentType(ContentType.Application.Json); setBody(OtpRequest(identity)) }
        return post("/v1/auth/verify") { contentType(ContentType.Application.Json); setBody(VerifyRequest(identity, AuthService.DEMO_CODE)) }
            .body<TokenDto>().accessToken
    }

    @Test fun `catalog is seeded and paginated`() = api { client ->
        val first = client.get("/v1/catalog/videos?page=1&limit=20").body<PageDto<VideoDto>>()
        assertEquals(20, first.items.size)
        assertTrue(first.hasMore)
        val last = client.get("/v1/catalog/videos?page=3&limit=20").body<PageDto<VideoDto>>()
        assertEquals(10, last.items.size)
        assertFalse(last.hasMore)
        assertEquals(50, client.get("/v1/catalog/tracks?limit=50").body<PageDto<TrackDto>>().items.size)
        val article = client.get("/v1/news/articles/n1").body<ArticleDto>()
        assertTrue(article.quiz.isNotEmpty())
        assertTrue('\n' in article.body)
    }

    @Test fun `bad params and unknown ids use the error envelope`() = api { client ->
        val bad = client.get("/v1/catalog/tracks?limit=500")
        assertEquals(HttpStatusCode.BadRequest, bad.status)
        assertEquals(ErrorCode.INVALID_PARAMS, bad.body<ErrorEnvelope>().error.code)
        val missing = client.get("/v1/news/articles/nope")
        assertEquals(HttpStatusCode.NotFound, missing.status)
        assertEquals(ErrorCode.NOT_FOUND, missing.body<ErrorEnvelope>().error.code)
    }

    @Test fun `otp rejects wrong codes, locks after five and expires`() = api { client ->
        val id = "a@b.vn"
        client.post("/v1/auth/otp") { contentType(ContentType.Application.Json); setBody(OtpRequest(id)) }
        repeat(AuthService.MAX_ATTEMPTS) {
            val r = client.post("/v1/auth/verify") { contentType(ContentType.Application.Json); setBody(VerifyRequest(id, "000000")) }
            assertEquals(ErrorCode.OTP_INVALID, r.body<ErrorEnvelope>().error.code)
        }
        val locked = client.post("/v1/auth/verify") { contentType(ContentType.Application.Json); setBody(VerifyRequest(id, AuthService.DEMO_CODE)) }
        assertEquals(HttpStatusCode.TooManyRequests, locked.status)

        client.post("/v1/auth/otp") { contentType(ContentType.Application.Json); setBody(OtpRequest(id)) }
        clock += AuthService.CODE_TTL_MS + 1
        val expired = client.post("/v1/auth/verify") { contentType(ContentType.Application.Json); setBody(VerifyRequest(id, AuthService.DEMO_CODE)) }
        assertEquals(ErrorCode.OTP_EXPIRED, expired.body<ErrorEnvelope>().error.code)
    }

    @Test fun `password login registers then checks the password and refresh rotates`() = api { client ->
        val ok = client.post("/v1/auth/login") { contentType(ContentType.Application.Json); setBody(LoginRequest("c@d.vn", "secret1")) }
        assertEquals(HttpStatusCode.OK, ok.status)
        val wrong = client.post("/v1/auth/login") { contentType(ContentType.Application.Json); setBody(LoginRequest("c@d.vn", "secret2")) }
        assertEquals(HttpStatusCode.Unauthorized, wrong.status)
        val short = client.post("/v1/auth/login") { contentType(ContentType.Application.Json); setBody(LoginRequest("c@d.vn", "123")) }
        assertEquals("password", short.body<ErrorEnvelope>().error.details?.get("field")?.toString()?.trim('"'))

        val token = ok.body<TokenDto>()
        val refreshed = client.post("/v1/auth/refresh") { contentType(ContentType.Application.Json); setBody(RefreshRequest(token.refreshToken)) }
        assertEquals(HttpStatusCode.OK, refreshed.status)
        val reused = client.post("/v1/auth/refresh") { contentType(ContentType.Application.Json); setBody(RefreshRequest(token.refreshToken)) }
        assertEquals(HttpStatusCode.Unauthorized, reused.status)
    }

    @Test fun `me endpoints need a bearer token`() = api { client ->
        assertEquals(HttpStatusCode.Unauthorized, client.get("/v1/me/playlists").status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/v1/me/points") { bearerAuth("at_forged") }.status)
    }

    @Test fun `playlist update with a stale version is a 409 conflict`() = api { client ->
        val token = client.signIn()
        val created = client.post("/v1/me/playlists") {
            bearerAuth(token); contentType(ContentType.Application.Json); setBody(PlaylistDto(name = "Sáng", items = listOf("m1", "v1", "n1")))
        }.body<PlaylistDto>()
        assertEquals(1, created.version)
        val updated = client.put("/v1/me/playlists/${created.id}") {
            bearerAuth(token); contentType(ContentType.Application.Json); setBody(created.copy(items = listOf("m2"), version = 1))
        }.body<PlaylistDto>()
        assertEquals(2, updated.version)
        val stale = client.put("/v1/me/playlists/${created.id}") {
            bearerAuth(token); contentType(ContentType.Application.Json); setBody(created.copy(name = "Cũ", version = 1))
        }
        assertEquals(HttpStatusCode.Conflict, stale.status)
        val error = stale.body<ErrorEnvelope>().error
        assertEquals(ErrorCode.VERSION_CONFLICT, error.code)
        assertEquals("\"2\"", error.details?.get("serverVersion").toString())

        val other = client.signIn("0907654321")
        assertTrue(client.get("/v1/me/playlists") { bearerAuth(other) }.body<List<PlaylistDto>>().isEmpty())
    }

    @Test fun `events are idempotent and advance today's mission`() = api { client ->
        val token = client.signIn()
        val event = EventDto(eventId = "evt-1", source = "music", amount = 5, missionType = "LISTEN")
        repeat(3) {
            val points = client.post("/v1/me/events") { bearerAuth(token); contentType(ContentType.Application.Json); setBody(event) }.body<PointsDto>()
            assertEquals(5, points.balance)
        }
        val missions = client.get("/v1/me/missions/today") { bearerAuth(token) }.body<List<MissionDto>>()
        assertEquals(3, missions.size)
        assertEquals(1, missions.single { it.targetType == "LISTEN" }.progress)
        assertEquals(1, client.get("/v1/me/points") { bearerAuth(token) }.body<PointsDto>().entries.size)
    }

    @Test fun `progress is versioned and listed since a timestamp`() = api { client ->
        val token = client.signIn()
        val first = client.put("/v1/me/progress/v1") { bearerAuth(token); contentType(ContentType.Application.Json); setBody(ProgressUpdate(1000)) }
            .body<ProgressDto>()
        clock += 10
        client.put("/v1/me/progress/v1") { bearerAuth(token); contentType(ContentType.Application.Json); setBody(ProgressUpdate(2000, version = 1)) }
        val stale = client.put("/v1/me/progress/v1") { bearerAuth(token); contentType(ContentType.Application.Json); setBody(ProgressUpdate(1500, version = 1)) }
        assertEquals(HttpStatusCode.Conflict, stale.status)
        val since = client.get("/v1/me/progress?since=${first.updatedAt}") { bearerAuth(token) }.body<List<ProgressDto>>()
        assertEquals(listOf(2000L), since.map { it.positionMs })
    }
}
