package com.example.onespace.domain.session.repository

import com.example.onespace.domain.session.model.PointEntry
import kotlinx.coroutines.flow.Flow

interface PointRepository {
    val totalPoints: Flow<Int>
    val recentEntries: Flow<List<PointEntry>>
    suspend fun add(source: String, amount: Int)
    suspend fun spend(amount: Int, description: String): Boolean
}
