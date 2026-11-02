package com.example.onespace.data.games.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.onespace.domain.games.challenge.DailyChallenge
import com.example.onespace.domain.games.challenge.DailyChallenges
import com.example.onespace.domain.games.repository.ChallengeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.challengeDataStore by preferencesDataStore(name = "onespace_daily_challenge")

internal class DataStoreChallengeRepository(
    private val context: Context
) : ChallengeRepository {

    private object Keys {
        val DATE = stringPreferencesKey("date")
        val PROGRESS = intPreferencesKey("progress")
        val COMPLETED = booleanPreferencesKey("completed")
        val REWARD = intPreferencesKey("reward")
    }

    private fun Preferences.toChallenge(date: String): DailyChallenge {
        val template = DailyChallenges.templateFor(date)
        if (this[Keys.DATE] != date) return DailyChallenge(date, template)
        return DailyChallenge(
            date = date,
            template = template,
            progress = this[Keys.PROGRESS] ?: 0,
            completed = this[Keys.COMPLETED] ?: false,
            rewardPoints = this[Keys.REWARD] ?: 0
        )
    }

    override fun challenge(date: String): Flow<DailyChallenge> =
        context.challengeDataStore.data.map { it.toChallenge(date) }

    override suspend fun report(date: String, gameId: String, value: Int): DailyChallenge? {
        var completedNow: DailyChallenge? = null
        context.challengeDataStore.edit { prefs ->
            val template = DailyChallenges.templateFor(date)
            if (template.gameId != gameId) return@edit
            if (prefs[Keys.DATE] != date) {
                prefs[Keys.DATE] = date
                prefs[Keys.PROGRESS] = 0
                prefs[Keys.COMPLETED] = false
                prefs[Keys.REWARD] = 0
            }
            if (prefs[Keys.COMPLETED] == true) return@edit
            val progress = DailyChallenges.apply(template, prefs[Keys.PROGRESS] ?: 0, value)
            prefs[Keys.PROGRESS] = progress
            if (progress >= template.target) {
                prefs[Keys.COMPLETED] = true
                completedNow = DailyChallenge(date, template, progress, completed = true)
            }
        }
        return completedNow
    }

    override suspend fun markRewarded(date: String, points: Int) {
        context.challengeDataStore.edit { prefs ->
            if (prefs[Keys.DATE] == date) prefs[Keys.REWARD] = points
        }
    }
}

fun createChallengeRepository(context: Context): ChallengeRepository = DataStoreChallengeRepository(context)
