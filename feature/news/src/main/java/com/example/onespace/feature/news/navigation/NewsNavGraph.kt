package com.example.onespace.feature.news.navigation

import androidx.compose.runtime.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.*
import androidx.navigation.compose.*
import com.example.onespace.core.designsystem.CatalogContent
import com.example.onespace.core.designsystem.EmptySpec
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Newspaper
import com.example.onespace.domain.session.Section
import com.example.onespace.feature.news.presentation.*

fun NavGraphBuilder.newsGraph(
    getGraphEntry: () -> androidx.navigation.NavBackStackEntry,
    onOpenArticle: (String) -> Unit,
    onListenTts: () -> Unit = {}
) {
    navigation<NewsRoute.Graph>(startDestination = NewsRoute.Feed) {
        composable<NewsRoute.Feed> { entry ->
            val owner = remember(entry) { getGraphEntry() }
            val vm: NewsViewModel = hiltViewModel(owner)
            val state by vm.uiState.collectAsStateWithLifecycle()
            val savedIds by vm.savedIds.collectAsStateWithLifecycle()
            val topics by vm.selectedTopics.collectAsStateWithLifecycle()
            CatalogContent(
                state,
                onRetry = vm::retry,
                empty = EmptySpec(Icons.Filled.Newspaper, "Chưa có tin tức", "Làm mới", vm::retry)
            ) { articles ->
                NewsScreen(articles, savedIds, topics, onOpenArticle)
            }
        }
        composable<NewsRoute.Article> { entry ->
            val id = entry.toRoute<NewsRoute.Article>().id
            val owner = remember(entry) { getGraphEntry() }
            val vm: NewsViewModel = hiltViewModel(owner)
            val state by vm.uiState.collectAsStateWithLifecycle()
            val savedIds by vm.savedIds.collectAsStateWithLifecycle()
            val ttsState by vm.ttsState.collectAsStateWithLifecycle()
            // Leaving the app stops the reading voice, like the video (ADR-12).
            val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
            androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) vm.stopTts()
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }
            val quizAnswers by vm.quizAnswers.collectAsStateWithLifecycle()
            val downloads by vm.downloads.collectAsStateWithLifecycle()
            CatalogContent(state) { articles ->
                val article = articles.find { it.id == id }
                // F-NEWS-12: remember the reading so it can float over other tabs.
                androidx.compose.runtime.LaunchedEffect(id, article?.title) {
                    article?.let { com.example.onespace.core.designsystem.ReadingState.open(it.id, it.title) }
                }
                ArticleScreen(
                    article = article,
                    related = articles,
                    saved = id in savedIds,
                    downloaded = id in downloads,
                    onToggleDownload = {
                        article?.let {
                            vm.toggleDownload(it.id, it.title, it.category + " · " + it.readTime)
                        }
                    },
                    onExtractAudio = {
                        article?.let { vm.addExtractedAudio(it.id, it.title, it.category) }
                    },
                    onReadComplete = vm::onArticleRead,
                    ttsState = ttsState,
                    onListen = {
                        // TTS takes the voice: dock music steps aside while the article reads.
                        onListenTts()
                        article?.let { vm.listen(it) }
                    },
                    onTtsStop = vm::stopTts,
                    onBookmark = { vm.bookmark(id) },
                    quizAnswers = quizAnswers[id] ?: emptyMap(),
                    onQuizAnswer = { qIdx, optIdx -> vm.answerQuiz(id, qIdx, optIdx) },
                    onOpenRelated = onOpenArticle
                )
            }
        }
    }
}
