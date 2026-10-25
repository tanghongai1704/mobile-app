package com.example.onespace.domain.session.model

enum class ShopCategory { GAME_UNLOCK, PET_ITEM, AVATAR_FRAME }

data class ShopItem(
    val id: String,
    val name: String,
    val description: String,
    val category: ShopCategory,
    val price: Int,
    val emoji: String = "🎁"
)

data class ShopState(
    val items: List<ShopItem> = defaultItems(),
    val ownedItemIds: Set<String> = emptySet()
) {
    fun isOwned(item: ShopItem) = item.id in ownedItemIds
}

fun defaultItems() = listOf(
    ShopItem("game_snake", "Snake", "Mở khoá game rắn săn mồi", ShopCategory.GAME_UNLOCK, 300, "🐍"),
    ShopItem("game_beat", "Beat Tiles", "Mở khoá game nốt nhạc rơi", ShopCategory.GAME_UNLOCK, 500, "🎵"),
    ShopItem("pet_hat", "Mũ thú cưng", "Phụ kiện trang trí thú cưng", ShopCategory.PET_ITEM, 50, "🎩"),
    ShopItem("pet_scarf", "Khăn thú cưng", "Phụ kiện giữ ấm", ShopCategory.PET_ITEM, 100, "🧣"),
    ShopItem("frame_bronze", "Khung Đồng", "Khung avatar mốc 7 ngày streak", ShopCategory.AVATAR_FRAME, 200, "🥉"),
    ShopItem("frame_silver", "Khung Bạc", "Khung avatar mốc 30 ngày streak", ShopCategory.AVATAR_FRAME, 400, "🥈"),
    ShopItem("frame_gold", "Khung Vàng", "Khung avatar mốc 1000 điểm", ShopCategory.AVATAR_FRAME, 800, "🥇")
)