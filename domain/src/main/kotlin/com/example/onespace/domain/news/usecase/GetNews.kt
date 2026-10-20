package com.example.onespace.domain.news.usecase

import com.example.onespace.domain.news.repository.NewsRepository

class GetNews(private val repository: NewsRepository) {
    operator fun invoke() = repository.observeCatalog()
}
