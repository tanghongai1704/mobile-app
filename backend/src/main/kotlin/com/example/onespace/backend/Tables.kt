package com.example.onespace.backend

import org.jetbrains.exposed.sql.Table

/** Exposed mappings of the Flyway schema in `db/migration` (the SQL files are the source of truth). */
object Users : Table("users") {
    val id = varchar("id", 40)
    val phone = varchar("phone", 20).nullable()
    val email = varchar("email", 254).nullable()
    val passwordHash = varchar("password_hash", 100).nullable()
    val name = varchar("name", 100)
    val avatar = varchar("avatar", 40).nullable()
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}

object Sessions : Table("sessions") {
    val id = varchar("id", 40)
    val userId = varchar("user_id", 40)
    val accessTokenHash = varchar("access_token_hash", 64)
    val accessExpiresAt = long("access_expires_at")
    val refreshTokenHash = varchar("refresh_token_hash", 64)
    val expiresAt = long("expires_at")
    override val primaryKey = PrimaryKey(id)
}

object ContentItems : Table("content_items") {
    val id = varchar("id", 40)
    val type = varchar("type", 10)
    val title = varchar("title", 200)
    val creator = varchar("creator", 100).nullable()
    val category = varchar("category", 60).nullable()
    val duration = varchar("duration", 10).nullable()
    val readTime = varchar("read_time", 30).nullable()
    val url = varchar("url", 500).nullable()
    val thumbnail = varchar("thumbnail", 500).nullable()
    val body = text("body").nullable()
    val lyrics = text("lyrics").nullable()
    val bpm = integer("bpm").nullable()
    val publishedAt = long("published_at")
    val sortOrder = integer("sort_order")
    val version = integer("version")
    override val primaryKey = PrimaryKey(id)
}

object QuizQuestions : Table("quiz_questions") {
    val id = long("id").autoIncrement()
    val itemId = varchar("item_id", 40)
    val position = integer("position")
    val question = varchar("question", 500)
    val options = text("options")
    val answerIndex = integer("answer_index")
    override val primaryKey = PrimaryKey(id)
}

object Playlists : Table("playlists") {
    val id = varchar("id", 40)
    val userId = varchar("user_id", 40)
    val name = varchar("name", 100)
    val version = integer("version")
    val updatedAt = long("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object PlaylistItems : Table("playlist_items") {
    val playlistId = varchar("playlist_id", 40)
    val itemId = varchar("item_id", 40)
    val position = integer("position")
    override val primaryKey = PrimaryKey(playlistId, itemId)
}

object PointEntries : Table("point_entries") {
    val id = long("id").autoIncrement()
    val userId = varchar("user_id", 40)
    val eventId = varchar("event_id", 64)
    val sourceName = varchar("source", 60)
    val amount = integer("amount")
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}

object DailyMissions : Table("daily_missions") {
    val id = varchar("id", 80)
    val userId = varchar("user_id", 40)
    val title = varchar("title", 100)
    val targetType = varchar("target_type", 10)
    val target = integer("target")
    val progress = integer("progress")
    val completed = bool("completed")
    val date = varchar("date", 10)
    val version = integer("version")
    override val primaryKey = PrimaryKey(id)
}

object Progress : Table("progress") {
    val userId = varchar("user_id", 40)
    val itemId = varchar("item_id", 40)
    val positionMs = long("position_ms")
    val version = integer("version")
    val updatedAt = long("updated_at")
    override val primaryKey = PrimaryKey(userId, itemId)
}
