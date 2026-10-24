package com.example.onespace.feature.video.navigation

import kotlinx.serialization.Serializable

/** Public navigation contract; arguments carry IDs only. */
sealed interface VideoRoute {
    @Serializable data object Graph : VideoRoute
    @Serializable data object Feed : VideoRoute
    @Serializable data class Detail(val id: String) : VideoRoute
}
