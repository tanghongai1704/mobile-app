package com.example.onespace.feature.home.navigation

import kotlinx.serialization.Serializable

/** Public navigation contract; arguments carry IDs only. */
sealed interface HomeRoute {
    @Serializable data object Graph : HomeRoute
    @Serializable data object Feed : HomeRoute
}
