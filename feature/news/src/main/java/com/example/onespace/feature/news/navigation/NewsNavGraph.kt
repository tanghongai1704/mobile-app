package com.example.onespace.feature.news.navigation

import com.example.onespace.core.designsystem.tr
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
    onListenTts: () -> Unit = {},
    session: com.example.onespace.core.session.SessionCoordinator? = null
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
                empty = EmptySpec(Icons.Filled.Newspaper, tr("Chưa có tin tức", "No news yet"), tr("Làm mới", "Refresh"), vm::retry)
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
            // The nav entry also stops on tab switches, but F-NEWS-12 keeps the
            // voice running while the reading floats over other tabs — so the
            // observer follows the activity, not this entry.
            val activity = androidx.compose.ui.platform.LocalContext.current.findActivity()
            androidx.compose.runtime.DisposableEffect(activity) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) vm.stopTts()
                }
                val lifecycle = (activity as? androidx.lifecycle.LifecycleOwner)?.lifecycle
                lifecycle?.addObserver(observer)
                onDispose { lifecycle?.removeObserver(observer) }
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
                    onExtractAudio = { done ->
                        article?.let { vm.addExtractedAudio(it.id, it.title, it.category, it.body, done) }
                    },
                    onReadComplete = vm::onArticleRead,
                    ttsState = ttsState,
                    onListen = {
                        // TTS takes the voice: dock music steps aside while the article reads.
                        onListenTts()
                        article?.let { vm.listen(it) }
                    },
                    onTtsStop = vm::stopTts,
                    onSeekTts = vm::seekTts,
                    onSetTtsVolume = vm::setTtsVolume,
                    onBookmark = { vm.bookmark(id) },
                    quizAnswers = quizAnswers[id] ?: emptyMap(),
                    onQuizAnswer = { qIdx, optIdx -> vm.answerQuiz(id, qIdx, optIdx) },
                    onOpenRelated = onOpenArticle,
                    musicPlaying = session?.music?.collectAsStateWithLifecycle()?.value?.trackId != null,
                    onPlayMusic = { session?.playMusic("m1", "Nắng sớm") }
                )
            }
        }
    }
}

/** The activity behind this context — its lifecycle, unlike a nav entry's, survives tab switches. */
private tailrec fun android.content.Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}
