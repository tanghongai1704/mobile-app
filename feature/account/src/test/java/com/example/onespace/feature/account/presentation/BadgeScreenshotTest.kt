package com.example.onespace.feature.account.presentation

import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.onespace.core.designsystem.OneSpaceTheme
import com.example.onespace.domain.games.badge.GameBadges
import com.example.onespace.domain.games.badge.RecordKeys
import com.example.onespace.domain.session.model.PetStage
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

abstract class BadgeShots(private val mode: String) {
    @get:Rule val compose = createComposeRule()

    @Test
    fun badgeGrid() {
        // A player part-way through: some badges earned, others with progress.
        val records = mapOf(
            RecordKeys.SNAKE to 9,
            RecordKeys.GAME_2048_TILE to 512,
            RecordKeys.VIDEO_QUIZ_COMBO to 10,
            RecordKeys.TIC_TAC_TOE_STREAK to 2,
            RecordKeys.BEAT_TILES to 12,
            RecordKeys.GUESS_SONG to 30,
            RecordKeys.CHALLENGES_DONE to 3
        )
        compose.setContent {
            OneSpaceTheme {
                Surface {
                    BadgeGridScreen(
                        totalPoints = 120,
                        currentStreak = 3,
                        petStage = PetStage.BABY,
                        gameBadges = GameBadges.evaluate(records)
                    )
                }
            }
        }
        val dir = File(System.getProperty("screenshot.dir") ?: "build/screenshots", mode).apply { mkdirs() }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage(File(dir, "b1-badges.png").path)
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w392dp-h1500dp-xxhdpi")
class BadgeScreenshotTest : BadgeShots("light")

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w392dp-h1500dp-night-xxhdpi")
class BadgeScreenshotDarkTest : BadgeShots("dark")
