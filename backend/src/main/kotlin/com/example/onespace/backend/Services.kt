package com.example.onespace.backend

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.mindrot.jbcrypt.BCrypt
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Base64
import java.util.UUID

private val listJson = Json

/** Turns "m:ss" (seed format) into seconds. */
internal fun parseDuration(label: String?): Int {
    if (label.isNullOrBlank()) return 0
    return label.split(':').fold(0) { acc, part -> acc * 60 + (part.trim().toIntOrNull() ?: 0) }
}

internal fun sha256(value: String): String =
    MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

/** Public catalogue: videos, music and news, ordered by the editorial sort order. */
class CatalogService(private val db: Database, private val mediaBaseUrl: String, private val mediaDir: java.io.File? = null) {

    /**
     * Stand-in for the CDN: a file `MEDIA_DIR/<folder>/<id>.<ext>` replaces the item's seed URL, so the
     * app streams from this server. Without a local file the public sample URL from the seed is kept.
     */
    private fun resolve(id: String, folder: String, ext: String, url: String?): String {
        val local = mediaDir?.resolve("$folder/$id.$ext")
        return when {
            local != null && local.isFile -> "${mediaBaseUrl.trimEnd('/')}/$folder/$id.$ext"
            url.isNullOrBlank() -> ""
            url.startsWith("/") -> mediaBaseUrl.trimEnd('/') + url
            else -> url
        }
    }

    private fun <T> page(type: String, page: Int, limit: Int, map: (ResultRow) -> T): PageDto<T> {
        if (page < 1 || limit !in 1..50) {
            throw ApiError(400, ErrorCode.INVALID_PARAMS, "page >= 1, limit 1..50", mapOf("field" to if (page < 1) "page" else "limit"))
        }
        return transaction(db) {
            val rows = ContentItems.selectAll().where { ContentItems.type eq type }
                .orderBy(ContentItems.sortOrder to SortOrder.ASC)
                .limit(limit + 1).offset(((page - 1) * limit).toLong())
                .toList()
            PageDto(rows.take(limit).map(map), page, rows.size > limit)
        }
    }

    fun videos(page: Int, limit: Int) = page("video", page, limit) { r ->
        VideoDto(
            id = r[ContentItems.id], title = r[ContentItems.title], creator = r[ContentItems.creator].orEmpty(),
            duration = parseDuration(r[ContentItems.duration]), url = resolve(r[ContentItems.id], "video", "mp4", r[ContentItems.url]),
            thumbnail = r[ContentItems.thumbnail], topic = r[ContentItems.category].orEmpty(),
            durationLabel = r[ContentItems.duration].orEmpty()
        )
    }

    fun tracks(page: Int, limit: Int) = page("music", page, limit) { r ->
        TrackDto(
            id = r[ContentItems.id], title = r[ContentItems.title], artist = r[ContentItems.creator].orEmpty(),
            duration = parseDuration(r[ContentItems.duration]), url = resolve(r[ContentItems.id], "audio", "mp3", r[ContentItems.url]),
            durationLabel = r[ContentItems.duration].orEmpty(),
            lyrics = r[ContentItems.lyrics]?.split('\n') ?: emptyList(), bpm = r[ContentItems.bpm]
        )
    }

    fun articles(page: Int, limit: Int) = page("news", page, limit) { toArticle(it, withQuiz = false) }

    fun article(id: String): ArticleDto = transaction(db) {
        val row = ContentItems.selectAll().where { (ContentItems.id eq id) and (ContentItems.type eq "news") }.singleOrNull()
            ?: throw ApiError(404, ErrorCode.NOT_FOUND, "Không tìm thấy bài viết $id")
        toArticle(row, withQuiz = true)
    }

    private fun toArticle(r: ResultRow, withQuiz: Boolean): ArticleDto {
        val quiz = if (!withQuiz) emptyList() else QuizQuestions.selectAll().where { QuizQuestions.itemId eq r[ContentItems.id] }
            .orderBy(QuizQuestions.position).map {
                QuizQuestionDto(it[QuizQuestions.question], listJson.decodeFromString(it[QuizQuestions.options]), it[QuizQuestions.answerIndex])
            }
        return ArticleDto(
            id = r[ContentItems.id], title = r[ContentItems.title], category = r[ContentItems.category].orEmpty(),
            readTime = r[ContentItems.readTime].orEmpty(), body = r[ContentItems.body].orEmpty(),
            publishedAt = r[ContentItems.publishedAt], quiz = quiz
        )
    }
}

/**
 * SEC-01: OTP of 6 digits, valid 5 minutes, 5 wrong attempts per code, 5 codes per identity per hour.
 * No SMS/e-mail gateway in this phase: the code is written to the server log (and is [DEMO_CODE] in demo mode).
 */
class AuthService(
    private val db: Database,
    private val demoOtp: Boolean = true,
    private val now: () -> Long = System::currentTimeMillis
) {
    private data class Challenge(val code: String, val expiresAt: Long, var attempts: Int = 0)

    private val challenges = mutableMapOf<String, Challenge>()
    private val requests = mutableMapOf<String, MutableList<Long>>()
    private val random = SecureRandom()

    @Synchronized
    fun requestOtp(identity: String): String {
        requireIdentity(identity)
        val recent = requests.getOrPut(identity) { mutableListOf() }.apply { removeAll { it < now() - HOUR_MS } }
        if (recent.size >= MAX_CODES_PER_HOUR) throw ApiError(429, ErrorCode.RATE_LIMITED, "Gửi mã quá nhiều lần, thử lại sau")
        recent += now()
        val code = if (demoOtp) DEMO_CODE else "%06d".format(random.nextInt(1_000_000))
        challenges[identity] = Challenge(code, now() + CODE_TTL_MS)
        return code
    }

    @Synchronized
    fun verify(identity: String, code: String): TokenDto {
        val c = challenges[identity]
        when {
            c == null || now() >= c.expiresAt -> throw ApiError(410, ErrorCode.OTP_EXPIRED, "Mã đã hết hạn, hãy gửi lại mã")
            c.attempts >= MAX_ATTEMPTS -> throw ApiError(429, ErrorCode.RATE_LIMITED, "Nhập sai quá nhiều lần, hãy gửi lại mã")
            c.code != code -> { c.attempts++; throw ApiError(400, ErrorCode.OTP_INVALID, "Mã không đúng") }
        }
        challenges.remove(identity)
        return issue(findOrCreateUser(identity, passwordHash = null))
    }

    /** Signs in with a password; an unknown identity is registered on first login. */
    fun login(identity: String, password: String): TokenDto {
        requireIdentity(identity)
        if (password.length < MIN_PASSWORD) {
            throw ApiError(400, ErrorCode.INVALID_PARAMS, "Mật khẩu cần ít nhất 6 ký tự", mapOf("field" to "password"))
        }
        val userId = transaction(db) {
            val row = Users.selectAll().where { (Users.phone eq identity) or (Users.email eq identity) }.singleOrNull()
            when {
                row == null -> null
                row[Users.passwordHash] == null -> {
                    Users.update({ Users.id eq row[Users.id] }) { it[passwordHash] = BCrypt.hashpw(password, BCrypt.gensalt()) }
                    row[Users.id]
                }
                BCrypt.checkpw(password, row[Users.passwordHash]) -> row[Users.id]
                else -> throw ApiError(401, ErrorCode.UNAUTHORIZED, "Sai mật khẩu")
            }
        } ?: findOrCreateUser(identity, BCrypt.hashpw(password, BCrypt.gensalt()))
        return issue(userId)
    }

    /** Refresh tokens rotate: the old session is deleted and a new pair is issued. */
    fun refresh(refreshToken: String): TokenDto {
        val userId = transaction(db) {
            val row = Sessions.selectAll().where { Sessions.refreshTokenHash eq sha256(refreshToken) }.singleOrNull()
            if (row == null || row[Sessions.expiresAt] < now()) throw ApiError(401, ErrorCode.UNAUTHORIZED, "Phiên đăng nhập đã hết hạn")
            Sessions.deleteWhere { id eq row[Sessions.id] }
            row[Sessions.userId]
        }
        return issue(userId)
    }

    fun logout(accessToken: String) = transaction(db) {
        Sessions.deleteWhere { accessTokenHash eq sha256(accessToken) }
    }

    /** Returns the user id for a valid access token, or throws 401. */
    fun authenticate(accessToken: String?): String {
        if (accessToken.isNullOrBlank()) throw ApiError(401, ErrorCode.UNAUTHORIZED, "Chưa đăng nhập")
        return transaction(db) {
            val row = Sessions.selectAll().where { Sessions.accessTokenHash eq sha256(accessToken) }.singleOrNull()
            if (row == null || row[Sessions.accessExpiresAt] < now()) throw ApiError(401, ErrorCode.UNAUTHORIZED, "Token không hợp lệ hoặc đã hết hạn")
            row[Sessions.userId]
        }
    }

    private fun findOrCreateUser(identity: String, passwordHash: String?): String = transaction(db) {
        Users.selectAll().where { (Users.phone eq identity) or (Users.email eq identity) }.singleOrNull()?.get(Users.id)
            ?: ("u_" + UUID.randomUUID().toString().replace("-", "").take(12)).also { newId ->
                val isEmail = '@' in identity
                Users.insert {
                    it[id] = newId
                    it[phone] = if (isEmail) null else identity
                    it[email] = if (isEmail) identity else null
                    it[Users.passwordHash] = passwordHash
                    it[name] = identity.substringBefore('@')
                    it[createdAt] = now()
                }
            }
    }

    private fun issue(userId: String): TokenDto = transaction(db) {
        val access = "at_" + token()
        val refresh = "rt_" + token()
        Sessions.insert {
            it[id] = UUID.randomUUID().toString()
            it[Sessions.userId] = userId
            it[accessTokenHash] = sha256(access)
            it[accessExpiresAt] = now() + ACCESS_TTL_S * 1000
            it[refreshTokenHash] = sha256(refresh)
            it[expiresAt] = now() + REFRESH_TTL_MS
        }
        val user = Users.selectAll().where { Users.id eq userId }.single()
        TokenDto(access, refresh, ACCESS_TTL_S, UserDto(userId, user[Users.name], user[Users.email] ?: user[Users.phone].orEmpty()))
    }

    private fun token(): String = ByteArray(24).also(random::nextBytes).let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }

    private fun requireIdentity(identity: String) {
        val ok = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(identity) || Regex("^\\+?\\d{9,12}$").matches(identity)
        if (!ok) throw ApiError(400, ErrorCode.INVALID_PARAMS, "Số điện thoại hoặc email không hợp lệ", mapOf("field" to "identity"))
    }

    companion object {
        const val DEMO_CODE = "123456"
        const val CODE_TTL_MS = 5 * 60_000L
        const val MAX_ATTEMPTS = 5
        const val MAX_CODES_PER_HOUR = 5
        const val MIN_PASSWORD = 6
        const val ACCESS_TTL_S = 3600L
        const val REFRESH_TTL_MS = 30L * 24 * 3600_000
        private const val HOUR_MS = 3600_000L
    }
}

/** Personal data of a signed-in user: playlists, progress, points and daily missions. */
class MeService(
    private val db: Database,
    private val zone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh"),
    private val now: () -> Long = System::currentTimeMillis
) {
    // ---- playlists (optimistic concurrency through `version`) ----

    fun playlists(userId: String): List<PlaylistDto> = transaction(db) {
        Playlists.selectAll().where { Playlists.userId eq userId }.orderBy(Playlists.updatedAt to SortOrder.DESC).map(::toPlaylist)
    }

    fun createPlaylist(userId: String, body: PlaylistDto): PlaylistDto = transaction(db) {
        validatePlaylist(body)
        val exists = Playlists.selectAll().where { (Playlists.userId eq userId) and (Playlists.name eq body.name.trim()) }.count() > 0
        if (exists) throw ApiError(400, ErrorCode.INVALID_PARAMS, "Đã có playlist tên này", mapOf("field" to "name"))
        val newId = "pl_" + UUID.randomUUID().toString().replace("-", "").take(12)
        Playlists.insert {
            it[id] = newId; it[Playlists.userId] = userId; it[name] = body.name.trim(); it[version] = 1; it[updatedAt] = now()
        }
        writeItems(newId, body.items)
        toPlaylist(Playlists.selectAll().where { Playlists.id eq newId }.single())
    }

    /** The client sends the version it last saw; a different server version means someone else wrote first: 409. */
    fun updatePlaylist(userId: String, id: String, body: PlaylistDto): PlaylistDto = transaction(db) {
        validatePlaylist(body)
        val row = ownedPlaylist(userId, id)
        val serverVersion = row[Playlists.version]
        if (body.version != serverVersion) {
            throw ApiError(409, ErrorCode.VERSION_CONFLICT, "Playlist đã được thay đổi ở thiết bị khác",
                mapOf("serverVersion" to serverVersion.toString()))
        }
        Playlists.update({ Playlists.id eq id }) {
            it[name] = body.name.trim(); it[version] = serverVersion + 1; it[updatedAt] = now()
        }
        PlaylistItems.deleteWhere { playlistId eq id }
        writeItems(id, body.items)
        toPlaylist(Playlists.selectAll().where { Playlists.id eq id }.single())
    }

    fun deletePlaylist(userId: String, id: String) = transaction(db) {
        ownedPlaylist(userId, id)
        PlaylistItems.deleteWhere { playlistId eq id }
        Playlists.deleteWhere { Playlists.id eq id }
    }

    private fun ownedPlaylist(userId: String, id: String): ResultRow =
        Playlists.selectAll().where { (Playlists.id eq id) and (Playlists.userId eq userId) }.singleOrNull()
            ?: throw ApiError(404, ErrorCode.NOT_FOUND, "Không tìm thấy playlist $id")

    private fun validatePlaylist(body: PlaylistDto) {
        if (body.name.isBlank() || body.name.length > 100) throw ApiError(400, ErrorCode.INVALID_PARAMS, "Tên playlist 1-100 ký tự", mapOf("field" to "name"))
        val distinct = body.items.distinct()
        if (distinct.isEmpty()) return
        val known = ContentItems.selectAll().where { ContentItems.id inList distinct }.count()
        if (known.toInt() != distinct.size) throw ApiError(400, ErrorCode.INVALID_PARAMS, "Có nội dung không tồn tại", mapOf("field" to "items"))
    }

    private fun writeItems(id: String, items: List<String>) {
        items.distinct().forEachIndexed { index, itemId ->
            PlaylistItems.insert { it[playlistId] = id; it[PlaylistItems.itemId] = itemId; it[position] = index }
        }
    }

    private fun toPlaylist(r: ResultRow): PlaylistDto {
        val items = PlaylistItems.selectAll().where { PlaylistItems.playlistId eq r[Playlists.id] }
            .orderBy(PlaylistItems.position).map { it[PlaylistItems.itemId] }
        return PlaylistDto(r[Playlists.id], r[Playlists.name], items, r[Playlists.version], Instant.ofEpochMilli(r[Playlists.updatedAt]).toString())
    }

    // ---- progress ----

    fun progress(userId: String, since: Long): List<ProgressDto> = transaction(db) {
        Progress.selectAll().where { (Progress.userId eq userId) and (Progress.updatedAt greater since) }
            .orderBy(Progress.updatedAt).map {
                ProgressDto(it[Progress.itemId], it[Progress.positionMs], it[Progress.version], it[Progress.updatedAt])
            }
    }

    fun saveProgress(userId: String, itemId: String, update: ProgressUpdate): ProgressDto = transaction(db) {
        if (update.positionMs < 0) throw ApiError(400, ErrorCode.INVALID_PARAMS, "positionMs >= 0", mapOf("field" to "positionMs"))
        if (ContentItems.selectAll().where { ContentItems.id eq itemId }.count() == 0L) {
            throw ApiError(404, ErrorCode.NOT_FOUND, "Không tìm thấy nội dung $itemId")
        }
        val row = Progress.selectAll().where { (Progress.userId eq userId) and (Progress.itemId eq itemId) }.singleOrNull()
        val time = now()
        if (row == null) {
            Progress.insert {
                it[Progress.userId] = userId; it[Progress.itemId] = itemId; it[positionMs] = update.positionMs; it[version] = 1; it[updatedAt] = time
            }
            ProgressDto(itemId, update.positionMs, 1, time)
        } else {
            val serverVersion = row[Progress.version]
            if (update.version != null && update.version != serverVersion) {
                throw ApiError(409, ErrorCode.VERSION_CONFLICT, "Tiến độ đã được cập nhật ở thiết bị khác",
                    mapOf("serverVersion" to serverVersion.toString(), "positionMs" to row[Progress.positionMs].toString()))
            }
            Progress.update({ (Progress.userId eq userId) and (Progress.itemId eq itemId) }) {
                it[positionMs] = update.positionMs; it[version] = serverVersion + 1; it[updatedAt] = time
            }
            ProgressDto(itemId, update.positionMs, serverVersion + 1, time)
        }
    }

    // ---- missions and points ----

    fun missionsToday(userId: String): List<MissionDto> = transaction(db) { ensureMissions(userId) }

    private fun today(): String = LocalDate.ofInstant(Instant.ofEpochMilli(now()), zone).toString()

    private fun ensureMissions(userId: String): List<MissionDto> {
        val date = today()
        val existing = DailyMissions.selectAll().where { (DailyMissions.userId eq userId) and (DailyMissions.date eq date) }
        if (existing.count() == 0L) {
            DEFAULT_MISSIONS.forEach { (type, title, target) ->
                DailyMissions.insert {
                    it[id] = "$userId:$date:$type"; it[DailyMissions.userId] = userId; it[DailyMissions.title] = title
                    it[targetType] = type; it[DailyMissions.target] = target; it[progress] = 0; it[completed] = false
                    it[DailyMissions.date] = date; it[version] = 1
                }
            }
        }
        return DailyMissions.selectAll().where { (DailyMissions.userId eq userId) and (DailyMissions.date eq date) }
            .orderBy(DailyMissions.targetType).map {
                MissionDto(it[DailyMissions.id], it[DailyMissions.title], it[DailyMissions.targetType], it[DailyMissions.target],
                    it[DailyMissions.progress], it[DailyMissions.completed], it[DailyMissions.date], it[DailyMissions.version])
            }
    }

    /** Idempotent by `eventId`: a retried event returns the current balance without counting twice. */
    fun postEvent(userId: String, event: EventDto): PointsDto = transaction(db) {
        if (event.eventId.isBlank() || event.eventId.length > 64) throw ApiError(400, ErrorCode.INVALID_PARAMS, "eventId 1-64 ký tự", mapOf("field" to "eventId"))
        if (event.amount !in 0..MAX_EVENT_POINTS) throw ApiError(400, ErrorCode.INVALID_PARAMS, "amount 0..$MAX_EVENT_POINTS", mapOf("field" to "amount"))
        val type = event.missionType?.uppercase()
        if (type != null && DEFAULT_MISSIONS.none { it.first == type }) {
            throw ApiError(400, ErrorCode.INVALID_PARAMS, "missionType phải là LISTEN, READ hoặc PLAY", mapOf("field" to "missionType"))
        }
        val seen = PointEntries.selectAll().where { PointEntries.eventId eq event.eventId }.singleOrNull()
        if (seen == null) {
            PointEntries.insert {
                it[PointEntries.userId] = userId; it[eventId] = event.eventId; it[sourceName] = event.source.take(60)
                it[amount] = event.amount; it[createdAt] = now()
            }
            if (type != null) {
                ensureMissions(userId)
                val missionId = "$userId:${today()}:$type"
                val m = DailyMissions.selectAll().where { DailyMissions.id eq missionId }.single()
                if (!m[DailyMissions.completed]) {
                    val progress = m[DailyMissions.progress] + 1
                    DailyMissions.update({ DailyMissions.id eq missionId }) {
                        it[DailyMissions.progress] = progress
                        it[completed] = progress >= m[DailyMissions.target]
                        it[version] = m[DailyMissions.version] + 1
                    }
                }
            }
        } else if (seen[PointEntries.userId] != userId) {
            throw ApiError(400, ErrorCode.INVALID_PARAMS, "eventId đã được dùng", mapOf("field" to "eventId"))
        }
        pointsOf(userId)
    }

    fun points(userId: String): PointsDto = transaction(db) { pointsOf(userId) }

    private fun pointsOf(userId: String): PointsDto {
        val sum = PointEntries.amount.sum()
        val balance = PointEntries.select(sum).where { PointEntries.userId eq userId }.single()[sum] ?: 0
        val entries = PointEntries.selectAll().where { PointEntries.userId eq userId }
            .orderBy(PointEntries.createdAt to SortOrder.DESC).limit(20)
            .map { PointEntryDto(it[PointEntries.sourceName], it[PointEntries.amount], it[PointEntries.createdAt]) }
        return PointsDto(balance, entries)
    }

    companion object {
        const val MAX_EVENT_POINTS = 100
        val DEFAULT_MISSIONS = listOf(
            // Same three missions as the app (RoomMissionRepository) so both sides agree on progress.
            Triple("LISTEN", "Nghe 1 bài nhạc", 1),
            Triple("PLAY", "Thắng 1 ván game", 1),
            Triple("READ", "Đọc 1 bài tin", 1)
        )
    }
}
