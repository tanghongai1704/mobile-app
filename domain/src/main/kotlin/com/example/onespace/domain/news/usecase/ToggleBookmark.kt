package com.example.onespace.domain.news.usecase

import com.example.onespace.domain.news.repository.NewsRepository

class ToggleBookmark(private val repository: NewsRepository) {
    suspend operator fun invoke(articleId: String) = repository.toggleBookmark(articleId)
}
