package com.example.onespace.domain.session.repository

import com.example.onespace.domain.session.model.ShopState
import kotlinx.coroutines.flow.Flow

interface ShopRepository {
    val shopState: Flow<ShopState>
    suspend fun isOwned(itemId: String): Boolean
    suspend fun recordPurchase(itemId: String)
}
