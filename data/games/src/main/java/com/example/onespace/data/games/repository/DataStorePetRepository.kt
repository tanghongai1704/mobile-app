package com.example.onespace.data.games.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.onespace.domain.session.model.PetStage
import com.example.onespace.domain.session.model.PetState
import com.example.onespace.domain.session.repository.PetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.petDataStore by preferencesDataStore(name = "onespace_pet")

class DataStorePetRepository(
    private val context: Context,
    private val pointRepository: com.example.onespace.domain.session.repository.PointRepository
) : PetRepository {

    private object Keys {
        val MISSIONS_COMPLETED = intPreferencesKey("missions_completed")
        val HAS_HATCHED = booleanPreferencesKey("has_hatched")
        val FEED_DATE = stringPreferencesKey("feed_date")
        val FEED_COUNT = intPreferencesKey("feed_count")
    }

    override val petState: Flow<PetState> = kotlinx.coroutines.flow.combine(
        context.petDataStore.data, pointRepository.totalPoints
    ) { prefs, totalPoints ->
        val missions = prefs[Keys.MISSIONS_COMPLETED] ?: 0
        PetState(
            stage = PetStage.fromPoints(totalPoints),
            totalPoints = totalPoints,
            missionsCompleted = missions,
            hasHatched = prefs[Keys.HAS_HATCHED] ?: (missions >= 3)
        )
    }

    override suspend fun onMissionsCompleted(count: Int) {
        context.petDataStore.edit { prefs ->
            prefs[Keys.MISSIONS_COMPLETED] = count
            if (count >= 3) prefs[Keys.HAS_HATCHED] = true
        }
    }

    override suspend fun canFeedToday(): Boolean {
        val today = LocalDate.now().toString()
        val prefs = context.petDataStore.data.first()
        val feedDate = prefs[Keys.FEED_DATE]
        val feedCount = prefs[Keys.FEED_COUNT] ?: 0
        return feedDate != today || feedCount < 2
    }

    override suspend fun recordFeed() {
        context.petDataStore.edit { prefs ->
            val today = LocalDate.now().toString()
            if (prefs[Keys.FEED_DATE] != today) {
                prefs[Keys.FEED_DATE] = today
                prefs[Keys.FEED_COUNT] = 1
            } else {
                prefs[Keys.FEED_COUNT] = (prefs[Keys.FEED_COUNT] ?: 0) + 1
            }
        }
    }
}

fun createPetRepository(
    context: Context,
    pointRepository: com.example.onespace.domain.session.repository.PointRepository
): PetRepository = DataStorePetRepository(context, pointRepository)
