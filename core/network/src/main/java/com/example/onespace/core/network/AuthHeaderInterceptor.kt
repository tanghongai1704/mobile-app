package com.example.onespace.core.network

import okhttp3.Interceptor
import okhttp3.Response

/** Supplies the current access token; implemented by the auth data module, bound in the app. */
fun interface AccessTokenProvider {
    fun accessToken(): String?
}

/** Adds `Authorization: Bearer <token>` to `/me/...` and `/auth/logout` when a session exists. */
class AuthHeaderInterceptor(private val tokens: AccessTokenProvider) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val needsAuth = path.startsWith("/me/") || path == "/auth/logout"
        val token = if (needsAuth) tokens.accessToken() else null
        return chain.proceed(
            if (token == null) request
            else request.newBuilder().header("Authorization", "Bearer $token").build()
        )
    }
}
