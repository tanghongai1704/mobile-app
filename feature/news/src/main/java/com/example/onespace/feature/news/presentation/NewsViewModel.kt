package com.example.onespace.feature.news.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onespace.core.common.ContentUiState
import com.example.onespace.domain.news.model.TtsState
import com.example.onespace.domain.news.repository.NewsRepository
import com.example.onespace.domain.news.repository.TtsEngine
import com.example.onespace.domain.news.usecase.ListenArticle
import com.example.onespace.domain.news.usecase.PauseTts
import com.example.onespace.domain.news.usecase.StopTts
import com.example.onespace.domain.news.usecase.ObserveSavedArticles
import com.example.onespace.domain.news.usecase.ToggleBookmark
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
internal class NewsViewModel @Inject constructor(
    getNews: NewsRepository,
    observeSaved: ObserveSavedArticles,
    private val toggleBookmarkUseCase: ToggleBookmark,
    preferences: com.example.onespace.domain.session.repository.SessionPreferencesRepository,
    private val listenArticle: ListenArticle,
    private val pointRepository: com.example.onespace.domain.session.repository.PointRepository,
    private val downloadedContent: com.example.onespace.domain.session.repository.DownloadedContentRepository,
    private val extractedAudio: com.example.onespace.domain.music.repository.ExtractedAudioRepository,
    private val pauseTts: PauseTts,
    private val stopTts: StopTts,
    ttsEngine: TtsEngine
) : ViewModel() {
    private val refresh = MutableStateFlow(0)

    val uiState = combine(getNews.observeCatalog(), observeSaved(), refresh) { articles, saved, _ ->
        ContentUiState(items = articles)
    }.catch { emit(ContentUiState(error = "Không tải được tin.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContentUiState(isLoading = true))

    /** F-NEWS-01: onboarding topics put matching categories first. */
    val selectedTopics = preferences.preferences.map { it.selectedTopics }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _savedIds = MutableStateFlow<Set<String>>(emptySet())
    val savedIds: StateFlow<Set<String>> = _savedIds.asStateFlow()

    private val _quizAnswers = MutableStateFlow<Map<String, Map<Int, Int>>>(emptyMap())
    val quizAnswers: StateFlow<Map<String, Map<Int, Int>>> = _quizAnswers.asStateFlow()

    val ttsState: StateFlow<TtsState> = ttsEngine.state

    /** F-MUS-12: downloaded articles, persisted in Room (DB v5). */
    val downloads = downloadedContent.observeByType("news")
        .map { list -> list.map { it.contentId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** F-NEWS-05: convert article to audio, save to Music library. */
    fun addExtractedAudio(articleId: String, title: String, category: String) {
        viewModelScope.launch {
            extractedAudio.add(articleId, title, category, "", "")
        }
    }

    fun toggleDownload(id: String, title: String = "", subtitle: String = "") {
        viewModelScope.launch {
            if (id in downloads.value) downloadedContent.remove(id)
            else downloadedContent.add(id, "news", title, subtitle)
        }
    }

    fun retry() { refresh.value = refresh.value + 1 }
    fun answerQuiz(articleId: String, questionIndex: Int, optionIndex: Int) {
        viewModelScope.launch {
            // Record answer
            val articleAnswers = _quizAnswers.value[articleId]?.toMutableMap() ?: mutableMapOf()
            if (questionIndex !in articleAnswers) {
                articleAnswers[questionIndex] = optionIndex
                _quizAnswers.value = _quizAnswers.value + (articleId to articleAnswers)
                // Check if correct and award points
                val articles = uiState.value.items
                val article = articles.find { it.id == articleId }
                val isCorrect = article?.quiz?.getOrNull(questionIndex)?.correctIndex == optionIndex
                if (isCorrect) pointRepository.add("quiz_answer", 15)
            }
        }
    }

    fun bookmark(id: String) {
        viewModelScope.launch {
            toggleBookmarkUseCase(id)
            // Update local state
            val current = _savedIds.value.toMutableSet()
            if (id in current) current.remove(id) else current.add(id)
            _savedIds.value = current
        }
    }

    fun listen(article: com.example.onespace.domain.news.model.Article) {
        viewModelScope.launch { listenArticle(article) }
    }

    fun stopTts() {
        viewModelScope.launch { stopTts() }
    }
}
