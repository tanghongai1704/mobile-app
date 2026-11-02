package com.example.onespace.data.news.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.savedArticlesDataStore by preferencesDataStore(name = "onespace_saved_articles")

/** Bookmarked article ids, persisted so the reading list survives restarts. */
internal class SavedArticlesStore(private val context: Context) {
    private val key = stringSetPreferencesKey("saved_ids")

    val ids: Flow<Set<String>> = context.savedArticlesDataStore.data.map { it[key] ?: emptySet() }

    suspend fun toggle(id: String) {
        context.savedArticlesDataStore.edit { prefs ->
            val current = prefs[key] ?: emptySet()
            prefs[key] = if (id in current) current - id else current + id
        }
    }
}
