package com.example.onespace.feature.account.navigation

import kotlinx.serialization.Serializable

sealed interface AccountRoute {
    @Serializable data object Graph : AccountRoute
    @Serializable data object Profile : AccountRoute
    @Serializable data object Settings : AccountRoute
    @Serializable data object Wrapped : AccountRoute
    @Serializable data object Notifications : AccountRoute
    @Serializable data object Playlists : AccountRoute
    @Serializable data object Downloads : AccountRoute
    @Serializable data object Badges : AccountRoute
}
