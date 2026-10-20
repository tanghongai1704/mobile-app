package com.example.onespace.domain.news.repository

import com.example.onespace.domain.news.model.Article
import kotlinx.coroutines.flow.Flow

interface NewsRepository {
    fun observeCatalog(): Flow<List<Article>>
    fun observeSavedIds(): Flow<Set<String>>
    suspend fun toggleBookmark(id: String)
}
