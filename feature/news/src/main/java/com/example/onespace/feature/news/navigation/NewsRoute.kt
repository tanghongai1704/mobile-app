package com.example.onespace.feature.news.navigation

import kotlinx.serialization.Serializable

/** Public navigation contract; arguments carry IDs only. */
sealed interface NewsRoute {
    @Serializable data object Graph : NewsRoute
    @Serializable data object Feed : NewsRoute
    @Serializable data class Article(val id: String) : NewsRoute
}
