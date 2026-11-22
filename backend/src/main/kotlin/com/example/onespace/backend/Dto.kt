package com.example.onespace.backend

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// Wire format shared with the app (core/network ApiDtos). Field names are the API contract.

@Serializable data class PageDto<T>(val items: List<T>, val page: Int, val hasMore: Boolean)

@Serializable data class VideoDto(
    val id: String, val title: String, val creator: String, val duration: Int, val url: String,
    val thumbnail: String? = null, val topic: String = "", val durationLabel: String = ""
)

@Serializable data class TrackDto(
    val id: String, val title: String, val artist: String, val album: String? = null, val duration: Int,
    val url: String, val durationLabel: String = "", val lyrics: List<String> = emptyList(), val bpm: Int? = null
)

@Serializable data class QuizQuestionDto(val question: String, val options: List<String>, val answerIndex: Int)

@Serializable data class ArticleDto(
    val id: String, val title: String, val category: String, val readTime: String, val body: String,
    val publishedAt: Long = 0, val quiz: List<QuizQuestionDto> = emptyList()
)

@Serializable data class OtpRequest(val identity: String)
@Serializable data class VerifyRequest(val identity: String, val code: String)
@Serializable data class LoginRequest(val identity: String, val password: String)
@Serializable data class RefreshRequest(val refreshToken: String)
@Serializable data class UserDto(val id: String, val name: String, val identity: String)
@Serializable data class TokenDto(val accessToken: String, val refreshToken: String, val expiresIn: Long, val user: UserDto)

@Serializable data class PlaylistDto(
    val id: String? = null, val name: String, val items: List<String>, val version: Int = 0, val updatedAt: String? = null
)

@Serializable data class ProgressDto(val itemId: String, val positionMs: Long, val version: Int = 0, val updatedAt: Long = 0)
@Serializable data class ProgressUpdate(val positionMs: Long, val version: Int? = null)

@Serializable data class MissionDto(
    val id: String, val title: String, val targetType: String, val target: Int, val progress: Int,
    val completed: Boolean, val date: String, val version: Int
)

/** A scored action. [eventId] is generated on the device so retries are counted once. */
@Serializable data class EventDto(val eventId: String, val source: String, val amount: Int, val missionType: String? = null)

@Serializable data class PointEntryDto(val source: String, val amount: Int, val createdAt: Long)
@Serializable data class PointsDto(val balance: Int, val entries: List<PointEntryDto> = emptyList())

@Serializable data class ErrorBody(val code: String, val message: String, val details: JsonObject? = null)
@Serializable data class ErrorEnvelope(val error: ErrorBody)

object ErrorCode {
    const val INVALID_PARAMS = "INVALID_PARAMS"
    const val UNAUTHORIZED = "UNAUTHORIZED"
    const val OTP_INVALID = "OTP_INVALID"
    const val OTP_EXPIRED = "OTP_EXPIRED"
    const val NOT_FOUND = "NOT_FOUND"
    const val VERSION_CONFLICT = "VERSION_CONFLICT"
    const val RATE_LIMITED = "RATE_LIMITED"
    const val SERVER_ERROR = "SERVER_ERROR"
}

/** Thrown by services; StatusPages turns it into the shared error envelope. */
class ApiError(val status: Int, val code: String, override val message: String, val details: Map<String, String> = emptyMap()) :
    RuntimeException(message)
