package com.example.onespace.feature.news.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onespace.domain.news.model.Article
import com.example.onespace.domain.news.usecase.GetNews
import com.example.onespace.domain.news.usecase.ObserveSavedArticles
import com.example.onespace.domain.news.usecase.ToggleBookmark
import com.example.onespace.domain.session.repository.SessionPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal data class NewsUiState(
    val items: List<Article> = emptyList(),
    val savedIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
internal class NewsViewModel @Inject constructor(
    getNews: GetNews,
    observeSavedArticles: ObserveSavedArticles,
    preferences: SessionPreferencesRepository,
    private val toggleBookmark: ToggleBookmark
) : ViewModel() {
    private val refresh = MutableStateFlow(0)

    val uiState = combine(getNews(), observeSavedArticles(), refresh) { articles, saved, _ ->
        NewsUiState(items = articles, savedIds = saved)
    }.catch { emit(NewsUiState(error = "Không tải được tin.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NewsUiState(isLoading = true))

    /** Demo switch from Settings: feeds render their offline state while on. */
    val offline = preferences.preferences.map { it.simulateOffline }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun retry() { refresh.value = refresh.value + 1 }

    fun bookmark(id: String) {
        viewModelScope.launch { toggleBookmark(id) }
    }
}
