package com.example.onespace.data.auth

import com.example.onespace.core.network.api.ApiErrorCode
import com.example.onespace.core.network.api.LoginRequest
import com.example.onespace.core.network.api.OtpRequest
import com.example.onespace.core.network.api.RefreshRequest
import com.example.onespace.core.network.api.TokenDto
import com.example.onespace.core.network.api.UserDto
import com.example.onespace.core.network.api.VerifyRequest
import com.example.onespace.core.network.fake.FakeJson
import com.example.onespace.core.network.fake.FakeRequest
import com.example.onespace.core.network.fake.FakeResponse
import com.example.onespace.core.network.fake.FakeRoute
import java.util.UUID

/**
 * Server side of `/auth/...` for the fake backend, following SEC-01: a 6-digit code valid for
 * 5 minutes, at most 5 wrong attempts per code. No SMS is sent; the demo code is [DEMO_CODE].
 */
internal class AuthFakeRoute(private val now: () -> Long = System::currentTimeMillis) : FakeRoute {
    private data class Challenge(val identity: String, val expiresAt: Long, var attempts: Int = 0)

    private val json = FakeJson.json
    private var challenge: Challenge? = null
    private val refreshTokens = mutableMapOf<String, String>() // refresh token -> identity
    private val accessTokens = mutableMapOf<String, String>() // access token -> identity

    @Synchronized
    override fun handle(request: FakeRequest): FakeResponse? {
        if (request.method != "POST" || !request.path.startsWith("auth/")) return null
        return when (request.path) {
            "auth/otp" -> {
                val body = json.decodeFromString(OtpRequest.serializer(), request.body.orEmpty())
                if (!body.identity.looksValid()) return invalidIdentity()
                challenge = Challenge(body.identity, now() + CODE_TTL_MS)
                FakeJson.noContent()
            }
            "auth/verify" -> {
                val body = json.decodeFromString(VerifyRequest.serializer(), request.body.orEmpty())
                val current = challenge
                when {
                    current == null || now() >= current.expiresAt ->
                        FakeJson.error(410, ApiErrorCode.OTP_EXPIRED, "Mã đã hết hạn, hãy gửi lại mã")
                    current.attempts >= MAX_ATTEMPTS ->
                        FakeJson.error(429, ApiErrorCode.RATE_LIMITED, "Nhập sai quá nhiều lần, hãy gửi lại mã")
                    current.identity != body.identity || body.code != DEMO_CODE -> {
                        current.attempts++
                        FakeJson.error(400, ApiErrorCode.OTP_INVALID, "Mã không đúng")
                    }
                    else -> {
                        challenge = null
                        issueTokens(body.identity)
                    }
                }
            }
            "auth/login" -> {
                val body = json.decodeFromString(LoginRequest.serializer(), request.body.orEmpty())
                when {
                    !body.identity.looksValid() -> invalidIdentity()
                    body.password.length < MIN_PASSWORD ->
                        FakeJson.error(400, ApiErrorCode.INVALID_PARAMS, "Mật khẩu cần ít nhất 6 ký tự", mapOf("field" to "password"))
                    else -> issueTokens(body.identity)
                }
            }
            "auth/refresh" -> {
                val body = json.decodeFromString(RefreshRequest.serializer(), request.body.orEmpty())
                val identity = refreshTokens.remove(body.refreshToken)
                    ?: return FakeJson.error(401, ApiErrorCode.UNAUTHORIZED, "Phiên đăng nhập đã hết hạn")
                issueTokens(identity)
            }
            "auth/logout" -> {
                val identity = request.bearerToken?.let(accessTokens::remove)
                    ?: return FakeJson.error(401, ApiErrorCode.UNAUTHORIZED, "Chưa đăng nhập")
                refreshTokens.values.removeAll { it == identity }
                FakeJson.noContent()
            }
            else -> FakeJson.error(404, ApiErrorCode.NOT_FOUND, "Không tìm thấy ${request.path}")
        }
    }

    private fun issueTokens(identity: String): FakeResponse {
        val refresh = UUID.randomUUID().toString()
        val access = "at_" + UUID.randomUUID().toString().replace("-", "")
        refreshTokens[refresh] = identity
        accessTokens[access] = identity
        return FakeJson.ok(
            TokenDto.serializer(),
            TokenDto(
                accessToken = access,
                refreshToken = refresh,
                expiresIn = 3600,
                user = UserDto(id = "u_" + identity.hashCode().toUInt().toString(16), name = identity.substringBefore('@'), identity = identity)
            )
        )
    }

    private fun invalidIdentity() =
        FakeJson.error(400, ApiErrorCode.INVALID_PARAMS, "Số điện thoại hoặc email không hợp lệ", mapOf("field" to "identity"))

    private fun String.looksValid(): Boolean =
        Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(this) || Regex("^\\+?\\d{9,12}$").matches(this)

    companion object {
        const val DEMO_CODE = "123456"
        const val CODE_TTL_MS = 5 * 60_000L
        const val MAX_ATTEMPTS = 5
        const val MIN_PASSWORD = 6
    }
}
