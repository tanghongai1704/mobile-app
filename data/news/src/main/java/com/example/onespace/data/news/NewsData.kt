package com.example.onespace.data.news

import com.example.onespace.domain.news.repository.NewsRepository
import com.example.onespace.data.news.repository.MockNewsRepository

/** Public wiring API; repository implementations remain internal to this module. */
fun createNewsRepository(): NewsRepository = MockNewsRepository()
