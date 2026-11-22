package com.example.onespace.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route

/** Supplies the current access token; implemented by the auth data module, bound in the app. */
fun interface AccessTokenProvider {
    fun accessToken(): String?
}

/** The token pair, so an expired access token can be renewed without asking the user to sign in again. */
interface SessionTokens : AccessTokenProvider {
    fun refreshToken(): String?
    fun update(accessToken: String, refreshToken: String)
    fun clear()
}

/** Adds `Authorization: Bearer <token>` to `/me/...` and `/auth/logout` when a session exists. */
class AuthHeaderInterceptor(private val tokens: AccessTokenProvider) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = if (needsAuth(request)) tokens.accessToken() else null
        return chain.proceed(
            if (token == null) request
            else request.newBuilder().header("Authorization", "Bearer $token").build()
        )
    }
}

internal fun needsAuth(request: Request): Boolean {
    val path = request.url.encodedPath
    return path.contains("/me/") || path.endsWith("/auth/logout")
}

@Serializable private data class RefreshBody(val refreshToken: String)
@Serializable private data class RefreshAnswer(val accessToken: String, val refreshToken: String)

/**
 * On a 401 from `/me/...`, trades the refresh token for a new pair once (`POST auth/refresh`, the
 * server rotates it) and replays the request. If refresh fails the tokens are cleared: the user
 * then signs in again, the local data stays.
 */
class TokenRefreshAuthenticator(
    private val baseUrl: String,
    private val tokens: SessionTokens,
    private val client: () -> OkHttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val request = response.request
        if (!needsAuth(request) || response.priorResponse != null) return null
        synchronized(this) {
            val sent = request.header("Authorization")?.removePrefix("Bearer ")
            val current = tokens.accessToken()
            // Another call already refreshed while this one waited.
            if (current != null && current != sent) return request.withToken(current)
            val refresh = tokens.refreshToken() ?: return null
            val call = Request.Builder()
                .url(baseUrl.trimEnd('/') + "/auth/refresh")
                .post(json.encodeToString(RefreshBody.serializer(), RefreshBody(refresh)).toRequestBody(JSON))
                .build()
            val answer = runCatching {
                client().newCall(call).execute().use { r ->
                    if (!r.isSuccessful) null else json.decodeFromString(RefreshAnswer.serializer(), r.body!!.string())
                }
            }.getOrNull()
            if (answer == null) {
                tokens.clear()
                return null
            }
            tokens.update(answer.accessToken, answer.refreshToken)
            return request.withToken(answer.accessToken)
        }
    }

    private fun Request.withToken(token: String) = newBuilder().header("Authorization", "Bearer $token").build()

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
