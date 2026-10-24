package com.example.onespace.feature.onboarding.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.example.onespace.feature.onboarding.presentation.OnboardingScreen
import com.example.onespace.feature.onboarding.presentation.OnboardingViewModel

fun NavGraphBuilder.onboardingGraph() {
    navigation<OnboardingRoute.Graph>(startDestination = OnboardingRoute.Interests) {
        composable<OnboardingRoute.Interests> {
            val vm: OnboardingViewModel = hiltViewModel()
            val selected by vm.selected.collectAsStateWithLifecycle()
            val saving by vm.saving.collectAsStateWithLifecycle()
            OnboardingScreen(vm.topics, selected.toSet(), vm::toggle, saving,
                onFinish = { vm.finish() },
                onSkip = { vm.finish(skip = true) })
        }
    }
}
