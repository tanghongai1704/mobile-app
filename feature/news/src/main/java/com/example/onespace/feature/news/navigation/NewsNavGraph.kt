package com.example.onespace.feature.news.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.runtime.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.*
import androidx.navigation.compose.*
import com.example.onespace.core.common.ContentUiState
import com.example.onespace.core.designsystem.CatalogContent
import com.example.onespace.core.designsystem.EmptySpec

import com.example.onespace.feature.news.presentation.*

fun NavGraphBuilder.newsGraph(
    getGraphEntry: () -> NavBackStackEntry,
    onOpenArticle: (String) -> Unit
) {
    navigation<NewsRoute.Graph>(startDestination = NewsRoute.Feed) {
        composable<NewsRoute.Feed> { entry ->
            val owner = remember(entry) { getGraphEntry() }
            val vm: NewsViewModel = hiltViewModel(owner)
            val state by vm.uiState.collectAsStateWithLifecycle()
            val offline by vm.offline.collectAsStateWithLifecycle()
            CatalogContent(
                ContentUiState(items = state.items, isLoading = state.isLoading, error = state.error),
                offline = offline,
                onRetry = vm::retry,
                empty = EmptySpec(Icons.Filled.Newspaper, "Chưa có tin tức", "Làm mới", vm::retry)
            ) { articles ->
                NewsScreen(articles, state.savedIds, onOpenArticle)
            }
        }
        composable<NewsRoute.Article> { entry ->
            val owner = remember(entry) { getGraphEntry() }
            val vm: NewsViewModel = hiltViewModel(owner)
            val state by vm.uiState.collectAsStateWithLifecycle()
            val id = entry.toRoute<NewsRoute.Article>().id
            CatalogContent(ContentUiState(items = state.items, isLoading = state.isLoading, error = state.error)) { articles ->
                ArticleScreen(articles.find { it.id == id }, id in state.savedIds) {
                    vm.bookmark(id)
                }
            }
        }
    }
}
