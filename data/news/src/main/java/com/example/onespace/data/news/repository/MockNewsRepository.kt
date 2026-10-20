package com.example.onespace.data.news.repository

import com.example.onespace.domain.news.model.Article
import com.example.onespace.domain.news.repository.NewsRepository

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Bundled demo data. Replace this binding, not the UI, when integrating real sources. */
internal class MockNewsRepository : NewsRepository {
    override fun observeCatalog() = flowOf(listOf(
        Article("n1", "A greener city starts with small ideas", "Lifestyle", "4 min read", "Across the city, small community gardens are bringing people together.\n\nThis sample article demonstrates the reading layout, saved articles and navigation. Real editorial content and offline storage will be connected in the next implementation phase.\n\nA shared space can begin with a few plants, a little patience and a conversation with your neighbours."),
        Article("n2", "Technology that makes room for life", "Technology", "6 min read", "Thoughtful technology should make everyday routines simpler.\n\nThis is sample content for the OneSpace news reader. Scroll position is held by the navigation destination while switching tabs."),
        Article("n3", "Your next weekend escape", "Travel", "3 min read", "A short journey can offer a fresh perspective.\n\nDiscover quiet trails, local cafes and new places close to home. This article is mock content bundled with the application.")
    ))
    private val savedIds = MutableStateFlow<Set<String>>(emptySet())
    override fun observeSavedIds() = savedIds.asStateFlow()
    override suspend fun toggleBookmark(id: String) {
        savedIds.update { if (id in it) it - id else it + id }
    }
}
