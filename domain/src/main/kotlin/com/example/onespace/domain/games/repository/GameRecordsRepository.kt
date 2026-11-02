package com.example.onespace.domain.games.repository

import kotlinx.coroutines.flow.Flow

/** Personal bests per game id (e.g. "g1" for 2048). Higher is better. */
interface GameRecordsRepository {
    fun best(gameId: String): Flow<Int>
    fun allBests(): Flow<Map<String, Int>>

    /** Stores [score] when it beats the record. Returns true when a new record was set. */
    suspend fun submit(gameId: String, score: Int): Boolean

    /** Adds one to a counter record (e.g. completed challenges) and returns the new value. */
    suspend fun increment(key: String): Int
}
