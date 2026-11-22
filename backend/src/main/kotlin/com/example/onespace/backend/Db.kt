package com.example.onespace.backend

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/** Where the server keeps its data. Default: an embedded H2 file in PostgreSQL mode (no install needed). */
data class DbConfig(
    val url: String = "jdbc:h2:file:./data/onespace;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    val user: String = "sa",
    val password: String = ""
) {
    companion object {
        fun fromEnv() = DbConfig(
            url = System.getenv("DB_URL") ?: DbConfig().url,
            user = System.getenv("DB_USER") ?: DbConfig().user,
            password = System.getenv("DB_PASSWORD") ?: DbConfig().password
        )
    }
}

object Db {
    /** Runs the Flyway migrations, connects Exposed and loads the seed catalogue once. */
    fun init(config: DbConfig): Database {
        Flyway.configure()
            .dataSource(config.url, config.user, config.password)
            .locations("classpath:db/migration")
            .load()
            .migrate()
        val db = Database.connect(config.url, user = config.user, password = config.password)
        Seed.loadIfEmpty(db)
        return db
    }
}

@Serializable private data class SeedVideo(val id: String, val title: String, val creator: String, val duration: String, val url: String, val topic: String)
@Serializable private data class SeedTrack(
    val id: String, val title: String, val artist: String, val duration: String, val url: String,
    val bpm: Int? = null, val lyrics: List<String> = emptyList()
)
@Serializable private data class SeedQuiz(val question: String, val options: List<String>, val answerIndex: Int)
@Serializable private data class SeedArticle(
    val id: String, val title: String, val category: String, val readTime: String, val body: String,
    val quiz: List<SeedQuiz> = emptyList()
)

/** The editorial catalogue (videos, music, news) shipped with the server in `resources/seed`. */
object Seed {
    private val json = Json { ignoreUnknownKeys = true }
    private const val BASE_PUBLISHED_AT = 1_793_520_000_000L // 2026-11-01

    private inline fun <reified T> read(name: String): List<T> {
        val text = Seed::class.java.getResource("/seed/$name")!!.readText()
        return json.decodeFromString(text)
    }

    fun loadIfEmpty(db: Database) = transaction(db) {
        if (ContentItems.selectAll().limit(1).count() > 0) return@transaction
        val videos = read<SeedVideo>("videos.json")
        val tracks = read<SeedTrack>("tracks.json")
        val articles = read<SeedArticle>("articles.json")
        val rows = mutableListOf<Map<String, Any?>>()
        videos.forEachIndexed { i, v ->
            rows += mapOf("id" to v.id, "type" to "video", "title" to v.title, "creator" to v.creator, "category" to v.topic,
                "duration" to v.duration, "url" to v.url, "order" to i)
        }
        tracks.forEachIndexed { i, t ->
            rows += mapOf("id" to t.id, "type" to "music", "title" to t.title, "creator" to t.artist, "duration" to t.duration,
                "url" to t.url, "bpm" to t.bpm, "lyrics" to t.lyrics.joinToString("\n").ifEmpty { null }, "order" to i)
        }
        articles.forEachIndexed { i, a ->
            rows += mapOf("id" to a.id, "type" to "news", "title" to a.title, "category" to a.category, "readTime" to a.readTime,
                "body" to a.body.replace("\\n", "\n"), "order" to i)
        }
        ContentItems.batchInsert(rows) { r ->
            this[ContentItems.id] = r["id"] as String
            this[ContentItems.type] = r["type"] as String
            this[ContentItems.title] = r["title"] as String
            this[ContentItems.creator] = r["creator"] as String?
            this[ContentItems.category] = r["category"] as String?
            this[ContentItems.duration] = r["duration"] as String?
            this[ContentItems.readTime] = r["readTime"] as String?
            this[ContentItems.url] = r["url"] as String?
            this[ContentItems.thumbnail] = null
            this[ContentItems.body] = r["body"] as String?
            this[ContentItems.lyrics] = r["lyrics"] as String?
            this[ContentItems.bpm] = r["bpm"] as Int?
            val order = r["order"] as Int
            this[ContentItems.sortOrder] = order
            this[ContentItems.publishedAt] = BASE_PUBLISHED_AT - order * 3_600_000L
            this[ContentItems.version] = 1
        }
        val quiz = articles.flatMap { a -> a.quiz.mapIndexed { p, q -> Triple(a.id, p, q) } }
        QuizQuestions.batchInsert(quiz) { (itemId, position, q) ->
            this[QuizQuestions.itemId] = itemId
            this[QuizQuestions.position] = position
            this[QuizQuestions.question] = q.question
            this[QuizQuestions.options] = json.encodeToString(q.options)
            this[QuizQuestions.answerIndex] = q.answerIndex
        }
    }
}
