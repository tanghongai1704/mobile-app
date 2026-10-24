package com.example.onespace.feature.onboarding.navigation

import kotlinx.serialization.Serializable

sealed interface OnboardingRoute {
    @Serializable data object Graph : OnboardingRoute
    @Serializable data object Interests : OnboardingRoute
}
