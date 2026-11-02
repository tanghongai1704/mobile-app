package com.example.onespace.feature.account.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.onespace.core.designsystem.oneSpaceColors

/**
 * F-COM-09: avatar frames unlock by point milestone. The picked frame and avatar are saved in
 * the session preferences (DataStore), so this object only describes the available frames.
 */
internal enum class FrameTier { BASIC, BRONZE, SILVER, GOLD }

internal data class AvatarFrame(val id: String, val name: String, val tier: FrameTier, val unlockPoints: Int)

/** Frame colour comes from the theme, so it follows light/dark. */
internal val AvatarFrame.color: Color
    @Composable get() = with(MaterialTheme.oneSpaceColors.frames) {
        when (tier) {
            FrameTier.BASIC -> basic
            FrameTier.BRONZE -> bronze
            FrameTier.SILVER -> silver
            FrameTier.GOLD -> gold
        }
    }

internal object AvatarFrameStore {
    val frames = listOf(
        AvatarFrame("basic", "Cơ bản", FrameTier.BASIC, 0),
        AvatarFrame("dong", "Đồng", FrameTier.BRONZE, 50),
        AvatarFrame("bac", "Bạc", FrameTier.SILVER, 100),
        AvatarFrame("vang", "Vàng", FrameTier.GOLD, 200)
    )

    fun unlocked(totalPoints: Int): List<AvatarFrame> = frames.filter { it.unlockPoints <= totalPoints }

    fun frameOf(id: String): AvatarFrame = frames.first { it.id == id }
}
