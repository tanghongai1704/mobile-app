package com.example.onespace.domain.session.usecase

import com.example.onespace.domain.session.model.ShopItem
import com.example.onespace.domain.session.repository.PointRepository
import com.example.onespace.domain.session.repository.ShopRepository

class PurchaseItem(
    private val pointRepository: PointRepository,
    private val shopRepository: ShopRepository
) {
    suspend operator fun invoke(item: ShopItem): PurchaseResult {
        if (shopRepository.isOwned(item.id)) return PurchaseResult.ALREADY_OWNED
        val spent = pointRepository.spend(item.price, "shop_${item.id}")
        if (!spent) return PurchaseResult.NOT_ENOUGH_POINTS
        shopRepository.recordPurchase(item.id)
        return PurchaseResult.SUCCESS
    }
}

class ObserveShop(private val repository: ShopRepository) {
    operator fun invoke() = repository.shopState
}

enum class PurchaseResult { SUCCESS, NOT_ENOUGH_POINTS, ALREADY_OWNED }
