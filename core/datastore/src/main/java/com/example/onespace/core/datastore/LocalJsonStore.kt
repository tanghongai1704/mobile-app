package com.example.onespace.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.localJsonDataStore by preferencesDataStore(name = "onespace_local_json")

/** Small string store for app-level state that has no repository of its own (e.g. playlists). */
class LocalJsonStore(context: Context) {
    private val appContext = context.applicationContext

    suspend fun read(key: String): String? =
        appContext.localJsonDataStore.data.first()[stringPreferencesKey(key)]

    suspend fun write(key: String, value: String) {
        appContext.localJsonDataStore.edit { it[stringPreferencesKey(key)] = value }
    }
}
