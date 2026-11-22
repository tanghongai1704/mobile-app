package com.example.onespace.backend

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticFiles
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonObject
import org.slf4j.LoggerFactory
import java.io.File

data class ServerConfig(
    val port: Int = 8080,
    val db: DbConfig = DbConfig(),
    val mediaDir: File = File("media"),
    /** Prefix for relative media URLs in the catalogue; stands in for the CDN of the target architecture. */
    val publicBaseUrl: String = "http://localhost:8080",
    val demoOtp: Boolean = true
) {
    companion object {
        fun fromEnv(): ServerConfig {
            val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
            return ServerConfig(
                port = port,
                db = DbConfig.fromEnv(),
                mediaDir = File(System.getenv("MEDIA_DIR") ?: "media"),
                publicBaseUrl = System.getenv("PUBLIC_BASE_URL") ?: "http://localhost:$port",
                demoOtp = System.getenv("DEMO_OTP")?.toBoolean() ?: true
            )
        }
    }
}

private val log = LoggerFactory.getLogger("onespace")

fun main() {
    val config = ServerConfig.fromEnv()
    val db = Db.init(config.db)
    log.info("OneSpace API on http://0.0.0.0:${config.port}/v1 (db=${config.db.url.substringBefore(';')}, media=${config.mediaDir.absolutePath})")
    embeddedServer(Netty, port = config.port, host = "0.0.0.0") {
        module(
            catalog = CatalogService(db, config.publicBaseUrl + "/media", config.mediaDir),
            auth = AuthService(db, config.demoOtp),
            me = MeService(db),
            mediaDir = config.mediaDir
        )
    }.start(wait = true)
}

val ApiJson = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }

fun Application.module(catalog: CatalogService, auth: AuthService, me: MeService, mediaDir: File? = null) {
    install(ContentNegotiation) { json(ApiJson) }
    install(CallLogging)
    install(StatusPages) {
        exception<ApiError> { call, e -> call.respondError(e) }
        exception<BadRequestException> { call, e ->
            call.respondError(ApiError(400, ErrorCode.INVALID_PARAMS, "Dữ liệu gửi lên không hợp lệ: ${e.rootCause().message}"))
        }
        exception<Throwable> { call, e ->
            log.error("Unhandled", e)
            call.respondError(ApiError(500, ErrorCode.SERVER_ERROR, "Lỗi máy chủ"))
        }
        status(HttpStatusCode.NotFound) { call, _ ->
            call.respondError(ApiError(404, ErrorCode.NOT_FOUND, "Không tìm thấy đường dẫn"))
        }
    }
    routing {
        get("/health") { call.respondText("ok", ContentType.Text.Plain) }
        mediaDir?.takeIf { it.isDirectory }?.let { staticFiles("/media", it) }
        route("/v1") {
            catalogRoutes(catalog)
            authRoutes(auth)
            meRoutes(auth, me)
        }
    }
}

private fun Throwable.rootCause(): Throwable = generateSequence(this) { it.cause }.last()

private suspend fun ApplicationCall.respondError(e: ApiError) {
    val details = e.details.takeIf { it.isNotEmpty() }?.let { m -> JsonObject(m.mapValues { JsonPrimitive(it.value) }) }
    respond(HttpStatusCode.fromValue(e.status), ErrorEnvelope(ErrorBody(e.code, e.message, details)))
}

private fun ApplicationCall.intParam(name: String, default: Int): Int {
    val raw = request.queryParameters[name] ?: return default
    return raw.toIntOrNull() ?: throw ApiError(400, ErrorCode.INVALID_PARAMS, "$name phải là số", mapOf("field" to name))
}

private fun Route.catalogRoutes(catalog: CatalogService) {
    get("/catalog/videos") { call.respond(catalog.videos(call.intParam("page", 1), call.intParam("limit", 20))) }
    get("/catalog/tracks") { call.respond(catalog.tracks(call.intParam("page", 1), call.intParam("limit", 20))) }
    get("/news/articles") { call.respond(catalog.articles(call.intParam("page", 1), call.intParam("limit", 20))) }
    get("/news/articles/{id}") { call.respond(catalog.article(call.parameters["id"]!!)) }
}

private fun ApplicationCall.bearer(): String? =
    request.headers["Authorization"]?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }?.substring(7)?.trim()

private fun Route.authRoutes(auth: AuthService) {
    post("/auth/otp") {
        val body = call.receive<OtpRequest>()
        val code = auth.requestOtp(body.identity.trim())
        log.info("OTP for ${body.identity.trim()}: $code (no SMS gateway in this phase)")
        call.respond(HttpStatusCode.NoContent)
    }
    post("/auth/verify") {
        val body = call.receive<VerifyRequest>()
        call.respond(auth.verify(body.identity.trim(), body.code.trim()))
    }
    post("/auth/login") {
        val body = call.receive<LoginRequest>()
        call.respond(auth.login(body.identity.trim(), body.password))
    }
    post("/auth/refresh") { call.respond(auth.refresh(call.receive<RefreshRequest>().refreshToken)) }
    post("/auth/logout") {
        val token = call.bearer()
        auth.authenticate(token)
        auth.logout(token!!)
        call.respond(HttpStatusCode.NoContent)
    }
}

private fun Route.meRoutes(auth: AuthService, me: MeService) {
    route("/me") {
        suspend fun ApplicationCall.user() = auth.authenticate(bearer())
        get("/playlists") { call.respond(me.playlists(call.user())) }
        post("/playlists") {
            val user = call.user()
            call.respond(HttpStatusCode.Created, me.createPlaylist(user, call.receive()))
        }
        put("/playlists/{id}") {
            val user = call.user()
            call.respond(me.updatePlaylist(user, call.parameters["id"]!!, call.receive()))
        }
        delete("/playlists/{id}") {
            me.deletePlaylist(call.user(), call.parameters["id"]!!)
            call.respond(HttpStatusCode.NoContent)
        }
        get("/progress") {
            val since = call.request.queryParameters["since"]?.let {
                it.toLongOrNull() ?: throw ApiError(400, ErrorCode.INVALID_PARAMS, "since là epoch ms", mapOf("field" to "since"))
            } ?: 0L
            call.respond(me.progress(call.user(), since))
        }
        put("/progress/{itemId}") {
            val user = call.user()
            call.respond(me.saveProgress(user, call.parameters["itemId"]!!, call.receive()))
        }
        get("/missions/today") { call.respond(me.missionsToday(call.user())) }
        post("/events") {
            val user = call.user()
            call.respond(me.postEvent(user, call.receive()))
        }
        get("/points") { call.respond(me.points(call.user())) }
    }
}
