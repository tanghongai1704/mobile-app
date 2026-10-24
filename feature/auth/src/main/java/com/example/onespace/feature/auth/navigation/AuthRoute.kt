package com.example.onespace.feature.auth.navigation

import kotlinx.serialization.Serializable

sealed interface AuthRoute {
    @Serializable data object Graph : AuthRoute
    @Serializable data object Login : AuthRoute
    @Serializable data class VerifyCode(val email: String) : AuthRoute
}
