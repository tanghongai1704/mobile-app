package com.example.onespace.data.news

import android.content.Context
import com.example.onespace.core.database.NewsArticleDao
import com.example.onespace.core.network.api.NewsApi
import com.example.onespace.core.network.fake.FakeRoute
import com.example.onespace.data.news.repository.NewsFakeRoute
import com.example.onespace.data.news.repository.OfflineFirstNewsRepository
import com.example.onespace.data.news.repository.SavedArticlesStore
import com.example.onespace.domain.news.repository.NewsRepository

/** Public wiring API; repository implementations remain internal to this module. */
fun createNewsRepository(context: Context, dao: NewsArticleDao, api: NewsApi): NewsRepository =
    OfflineFirstNewsRepository(dao, api, SavedArticlesStore(context.applicationContext))

/** The news slice of the fake backend, registered into OkHttp by the composition root. */
fun createNewsFakeRoute(): FakeRoute = NewsFakeRoute()
