package com.example.onespace.feature.news.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onespace.domain.news.model.Article
import com.example.onespace.domain.news.usecase.GetNews
import com.example.onespace.domain.news.usecase.ObserveSavedArticles
import com.example.onespace.domain.news.usecase.ToggleBookmark
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
    private val toggleBookmark: ToggleBookmark
) : ViewModel() {
    val uiState = combine(getNews(), observeSavedArticles()) { articles, saved ->
        NewsUiState(items = articles, savedIds = saved)
    }.catch { emit(NewsUiState(error = "Unable to load stories")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NewsUiState(isLoading = true))

    fun bookmark(id: String) {
        viewModelScope.launch { toggleBookmark(id) }
    }
}
