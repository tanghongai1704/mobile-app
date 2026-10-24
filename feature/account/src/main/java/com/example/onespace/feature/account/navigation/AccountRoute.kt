package com.example.onespace.feature.account.navigation

import kotlinx.serialization.Serializable

sealed interface AccountRoute {
    @Serializable data object Graph : AccountRoute
    @Serializable data object Profile : AccountRoute
    @Serializable data object Settings : AccountRoute
}
