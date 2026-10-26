package com.example.onespace.feature.account.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * F-COM-09: avatar frames unlock by point milestone; the pick lives in a
 * process-level store until it moves to DataStore.
 */
internal data class AvatarFrame(val id: String, val name: String, val color: Long, val unlockPoints: Int)

internal object AvatarFrameStore {
    val frames = listOf(
        AvatarFrame("basic", "Cơ bản", 0xFF9E9E9E, 0),
        AvatarFrame("dong", "Đồng", 0xFFCD7F32, 50),
        AvatarFrame("bac", "Bạc", 0xFF90A4AE, 100),
        AvatarFrame("vang", "Vàng", 0xFFFFC107, 200)
    )

    var selected by mutableStateOf("basic")

    /** F-COM-09: the picked avatar emoji; null keeps the initial-letter avatar. */
    var avatar by mutableStateOf<String?>(null)

    fun unlocked(totalPoints: Int): List<AvatarFrame> = frames.filter { it.unlockPoints <= totalPoints }

    fun frameOf(id: String): AvatarFrame = frames.first { it.id == id }
}
