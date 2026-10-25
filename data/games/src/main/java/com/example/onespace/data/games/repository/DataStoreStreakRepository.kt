package com.example.onespace.data.games.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.onespace.domain.session.model.StreakCalculator
import com.example.onespace.domain.session.model.StreakState
import com.example.onespace.domain.session.repository.StreakRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.streakDataStore by preferencesDataStore(name = "onespace_streak")

class DataStoreStreakRepository(
    private val context: Context
) : StreakRepository {

    private object Keys {
        val CURRENT = intPreferencesKey("current_streak")
        val BEST = intPreferencesKey("best_streak")
        val LAST_ACTIVE = stringPreferencesKey("last_active_date")
    }

    override val streak: Flow<StreakState> = context.streakDataStore.data.map { prefs ->
        StreakState(
            currentStreak = prefs[Keys.CURRENT] ?: 0,
            bestStreak = prefs[Keys.BEST] ?: 0,
            lastActiveDate = prefs[Keys.LAST_ACTIVE]
        )
    }

    override suspend fun onMissionCompleted(today: String, yesterday: String) {
        context.streakDataStore.edit { prefs ->
            val current = StreakState(
                currentStreak = prefs[Keys.CURRENT] ?: 0,
                bestStreak = prefs[Keys.BEST] ?: 0,
                lastActiveDate = prefs[Keys.LAST_ACTIVE]
            )
            val updated = StreakCalculator.onMissionCompleted(current, today, yesterday)
            prefs[Keys.CURRENT] = updated.currentStreak
            prefs[Keys.BEST] = updated.bestStreak
            prefs[Keys.LAST_ACTIVE] = today
        }
    }
}

fun createStreakRepository(context: Context): StreakRepository = DataStoreStreakRepository(context)
