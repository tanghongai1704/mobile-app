package com.example.onespace.data.news

import android.content.Context
import com.example.onespace.data.news.repository.MockNewsRepository
import com.example.onespace.data.news.repository.SavedArticlesStore
import com.example.onespace.domain.news.repository.NewsRepository

/** Public wiring API; repository implementations remain internal to this module. */
fun createNewsRepository(context: Context): NewsRepository =
    MockNewsRepository(SavedArticlesStore(context.applicationContext))
