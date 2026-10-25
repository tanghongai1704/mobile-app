package com.example.onespace.data.games.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.onespace.domain.session.model.ShopState
import com.example.onespace.domain.session.repository.ShopRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.shopDataStore by preferencesDataStore(name = "onespace_shop")

class DataStoreShopRepository(
    private val context: Context
) : ShopRepository {

    private object Keys {
        val OWNED = stringSetPreferencesKey("owned_items")
    }

    override val shopState: Flow<ShopState> = context.shopDataStore.data.map { prefs ->
        ShopState(ownedItemIds = prefs[Keys.OWNED] ?: emptySet())
    }

    override suspend fun isOwned(itemId: String): Boolean {
        val prefs = context.shopDataStore.data.first()
        return itemId in (prefs[Keys.OWNED] ?: emptySet())
    }

    override suspend fun recordPurchase(itemId: String) {
        context.shopDataStore.edit { prefs ->
            val current = prefs[Keys.OWNED] ?: emptySet()
            prefs[Keys.OWNED] = current + itemId
        }
    }
}

fun createShopRepository(context: Context): ShopRepository = DataStoreShopRepository(context)
