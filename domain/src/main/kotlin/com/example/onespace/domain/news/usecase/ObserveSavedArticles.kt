package com.example.onespace.domain.news.usecase

import com.example.onespace.domain.news.repository.NewsRepository

class ObserveSavedArticles(private val repository: NewsRepository) {
    operator fun invoke() = repository.observeSavedIds()
}
