package com.example.onespace.feature.onboarding.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.example.onespace.feature.onboarding.presentation.LoginScreen
import com.example.onespace.feature.onboarding.presentation.OnboardingScreen
import com.example.onespace.feature.onboarding.presentation.OnboardingViewModel
import com.example.onespace.feature.onboarding.presentation.SplashScreen

object OnboardingNavigation {
    const val GRAPH = "onboarding"
    const val SPLASH = "onboarding/splash"
    const val STEPS = "onboarding/steps"
    const val LOGIN = "onboarding/login"
}

/** First-run flow: Splash → steps → Login/Guest. [onDone] leads into the main UI. */
fun NavGraphBuilder.onboardingGraph(nav: NavHostController, onDone: () -> Unit) {
    navigation(startDestination = OnboardingNavigation.SPLASH, route = OnboardingNavigation.GRAPH) {
        composable(OnboardingNavigation.SPLASH) {
            SplashScreen(onNext = {
                nav.navigate(OnboardingNavigation.STEPS) {
                    popUpTo(OnboardingNavigation.SPLASH) { inclusive = true }
                }
            })
        }
        composable(OnboardingNavigation.STEPS) { entry ->
            val owner = remember(entry) { nav.getBackStackEntry(OnboardingNavigation.GRAPH) }
            val vm: OnboardingViewModel = hiltViewModel(owner)
            OnboardingScreen(vm, onDone = {
                nav.navigate(OnboardingNavigation.LOGIN) { launchSingleTop = true }
            })
        }
        composable(OnboardingNavigation.LOGIN) { entry ->
            val owner = remember(entry) { nav.getBackStackEntry(OnboardingNavigation.GRAPH) }
            val vm: OnboardingViewModel = hiltViewModel(owner)
            LoginScreen(vm, onDone = onDone)
        }
    }
}
