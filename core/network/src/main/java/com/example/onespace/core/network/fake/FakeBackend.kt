package com.example.onespace.core.network.fake

import com.example.onespace.core.network.api.ErrorBody
import com.example.onespace.core.network.api.ErrorEnvelope
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** One incoming request as the fake server sees it. */
data class FakeRequest(
    val method: String,
    /** Path without the leading slash, e.g. `news/articles/n1`. */
    val path: String,
    val query: Map<String, String>,
    val body: String?,
    val bearerToken: String?
)

data class FakeResponse(val status: Int, val body: String = "")

/**
 * A slice of the backend described in Section 4 (API contract). Data modules contribute routes for
 * their own resources, so the app talks HTTP/JSON through Retrofit exactly as it will with the real
 * server; swapping in the real base URL removes the fake without touching repositories or UI.
 */
interface FakeRoute {
    /** Returns null when the request is not handled by this route. */
    fun handle(request: FakeRequest): FakeResponse?
}

/** JSON helpers shared by fake routes so every response follows the contract. */
object FakeJson {
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun <T> ok(serializer: KSerializer<T>, value: T, status: Int = 200) =
        FakeResponse(status, json.encodeToString(serializer, value))

    fun noContent() = FakeResponse(204)

    fun error(status: Int, code: String, message: String, details: Map<String, String> = emptyMap()) =
        FakeResponse(
            status,
            json.encodeToString(
                ErrorEnvelope.serializer(),
                ErrorEnvelope(
                    ErrorBody(
                        code,
                        message,
                        if (details.isEmpty()) null else JsonObject(details.mapValues { JsonPrimitive(it.value) })
                    )
                )
            )
        )
}
