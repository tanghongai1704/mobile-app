package com.example.onespace.data.games.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.onespace.domain.games.repository.GameRecordsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.recordsDataStore by preferencesDataStore(name = "onespace_game_records")

private const val KEY_PREFIX = "best_"

internal class DataStoreGameRecordsRepository(
    private val context: Context
) : GameRecordsRepository {

    private fun key(gameId: String) = intPreferencesKey(KEY_PREFIX + gameId)

    override fun best(gameId: String): Flow<Int> =
        context.recordsDataStore.data.map { it[key(gameId)] ?: 0 }

    override fun allBests(): Flow<Map<String, Int>> =
        context.recordsDataStore.data.map { prefs -> prefs.toBests() }

    override suspend fun submit(gameId: String, score: Int): Boolean {
        var improved = false
        context.recordsDataStore.edit { prefs ->
            if (score > (prefs[key(gameId)] ?: 0)) {
                prefs[key(gameId)] = score
                improved = true
            }
        }
        return improved
    }

    override suspend fun increment(key: String): Int {
        var value = 0
        context.recordsDataStore.edit { prefs ->
            value = (prefs[key(key)] ?: 0) + 1
            prefs[key(key)] = value
        }
        return value
    }

    private fun Preferences.toBests(): Map<String, Int> =
        asMap().entries
            .filter { it.key.name.startsWith(KEY_PREFIX) && it.value is Int }
            .associate { it.key.name.removePrefix(KEY_PREFIX) to it.value as Int }
}

fun createGameRecordsRepository(context: Context): GameRecordsRepository =
    DataStoreGameRecordsRepository(context)
