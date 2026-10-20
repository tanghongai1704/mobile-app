package com.example.onespace.feature.news.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.*
import androidx.navigation.compose.*

import com.example.onespace.feature.news.presentation.*

object NewsNavigation {
    const val GRAPH = "news"
    const val FEED = "news/feed"
    const val ARTICLE = "news/article/{id}"
    fun article(id: String) = "news/article/" + android.net.Uri.encode(id)
}

fun NavGraphBuilder.newsGraph(nav: NavHostController) {
    navigation(startDestination = NewsNavigation.FEED, route = NewsNavigation.GRAPH) {
        composable(NewsNavigation.FEED) { entry ->
            val owner = remember(entry) { nav.getBackStackEntry(NewsNavigation.GRAPH) }
            val vm: NewsViewModel = hiltViewModel(owner)
            val state by vm.uiState.collectAsStateWithLifecycle()
            when {
                state.isLoading -> Text("Loading…")
                state.error != null -> Text(state.error.orEmpty())
                else -> NewsScreen(state.items, state.savedIds) { nav.navigate(NewsNavigation.article(it)) }
            }
        }
        composable(NewsNavigation.ARTICLE, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val owner = remember(entry) { nav.getBackStackEntry(NewsNavigation.GRAPH) }
            val vm: NewsViewModel = hiltViewModel(owner)
            val state by vm.uiState.collectAsStateWithLifecycle()
            val id = entry.arguments?.getString("id")
            when {
                state.isLoading -> Text("Loading…")
                state.error != null -> Text(state.error.orEmpty())
                else -> ArticleScreen(state.items.find { it.id == id }, id in state.savedIds) {
                    if (id != null) vm.bookmark(id)
                }
            }
        }
    }
}
