package com.example.onespace.feature.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.example.onespace.domain.session.Section
import com.example.onespace.feature.home.presentation.HomeScreen

object HomeNavigation {
    const val GRAPH = "home"
    const val FEED = "home/feed"
}

fun NavGraphBuilder.homeGraph(onSection: (Section) -> Unit) {
    navigation(startDestination = HomeNavigation.FEED, route = HomeNavigation.GRAPH) {
        composable(HomeNavigation.FEED) { HomeScreen(onSection) }
    }
}
