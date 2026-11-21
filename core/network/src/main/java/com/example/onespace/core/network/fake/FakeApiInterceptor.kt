package com.example.onespace.core.network.fake

import com.example.onespace.core.network.api.ApiErrorCode
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.io.IOException

/**
 * Terminal OkHttp interceptor that answers requests in-process instead of opening a socket.
 * It still honours the device's real connectivity: with no network the call fails with an
 * [IOException], so offline behaviour (cache kept, ErrorState + "Thử lại") is the real one.
 */
class FakeApiInterceptor(
    private val routes: Set<FakeRoute>,
    private val isOnline: () -> Boolean = { true },
    private val latencyMs: Long = 250L,
    /** Version prefix of the base URL (`https://host/v1/`); routes see paths relative to it. */
    private val basePath: String = "v1/"
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!isOnline()) throw IOException("Device is offline")
        if (latencyMs > 0) Thread.sleep(latencyMs)

        val url = request.url
        val body = request.body?.let { b -> Buffer().also { b.writeTo(it) }.readUtf8() }
        val fake = FakeRequest(
            method = request.method,
            path = url.encodedPath.trimStart('/').removePrefix(basePath),
            query = url.queryParameterNames.associateWith { url.queryParameter(it).orEmpty() },
            body = body,
            bearerToken = request.header("Authorization")?.removePrefix("Bearer ")?.trim()
        )
        val answer = runCatching { routes.firstNotNullOfOrNull { it.handle(fake) } }
            .getOrElse { FakeJson.error(500, ApiErrorCode.SERVER_ERROR, "Lỗi máy chủ") }
            ?: FakeJson.error(404, ApiErrorCode.NOT_FOUND, "Không tìm thấy ${fake.path}")

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(answer.status)
            .message(if (answer.status < 400) "OK" else "Error")
            .body(answer.body.toResponseBody(JSON))
            .build()
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
