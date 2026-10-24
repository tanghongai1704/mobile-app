package com.example.onespace.app.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoute {
    @Serializable data object Root : AppRoute
    @Serializable data object Splash : AppRoute
    @Serializable data object Main : AppRoute
}
