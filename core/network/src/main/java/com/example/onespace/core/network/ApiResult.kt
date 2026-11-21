package com.example.onespace.core.network

import com.example.onespace.core.network.api.ErrorEnvelope
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/** A request reached the server and was refused; [code] comes from the shared error envelope. */
class ApiException(
    val httpStatus: Int,
    val code: String,
    override val message: String,
    val details: Map<String, String> = emptyMap()
) : Exception(message)

/** The request never got an answer (offline, DNS, timeout). Callers keep showing cached data. */
class NetworkUnavailableException(cause: Throwable) : IOException("Không có kết nối mạng", cause)

private val errorJson = Json { ignoreUnknownKeys = true }

/**
 * Runs a Retrofit call and normalises failures: HTTP errors become [ApiException] parsed from the
 * `{ "error": { code, message, details } }` envelope, transport failures become
 * [NetworkUnavailableException].
 */
suspend fun <T> apiCall(block: suspend () -> T): T = try {
    block()
} catch (e: HttpException) {
    throw e.toApiException()
} catch (e: ApiException) {
    throw e
} catch (e: IOException) {
    throw if (e is NetworkUnavailableException) e else NetworkUnavailableException(e)
}

fun HttpException.toApiException(): ApiException {
    val raw = response()?.errorBody()?.string().orEmpty()
    val envelope = runCatching { errorJson.decodeFromString(ErrorEnvelope.serializer(), raw) }.getOrNull()
    return ApiException(
        httpStatus = code(),
        code = envelope?.error?.code ?: "HTTP_${code()}",
        message = envelope?.error?.message ?: message(),
        details = envelope?.error?.details?.mapValues { it.value.toString().trim('"') } ?: emptyMap()
    )
}
