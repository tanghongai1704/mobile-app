package com.example.onespace.feature.games.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.onespace.core.designsystem.OneSpaceTheme
import com.example.onespace.domain.games.challenge.DailyChallenge
import com.example.onespace.domain.games.challenge.DailyChallenges
import com.example.onespace.domain.games.usecase.ChallengeCompletion
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

abstract class ChallengeShots(private val mode: String) {
    @get:Rule val compose = createComposeRule()

    private fun shot(name: String) {
        val dir = File(System.getProperty("screenshot.dir") ?: "build/screenshots", mode).apply { mkdirs() }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage(File(dir, "$name.png").path)
    }

    @Test
    fun challengeStates() {
        val date = "2026-11-01"
        val twoK = DailyChallenges.pool.first { it.gameId == "g1" }
        val ttt = DailyChallenges.pool.first { it.gameId == "g2" }
        val done = DailyChallenge(date, twoK, 320, completed = true, rewardPoints = 65)
        compose.setContent {
            OneSpaceTheme {
                Surface {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        DailyChallengeCard(DailyChallenge(date, twoK, progress = 150), streakDays = 3, onPlay = {})
                        DailyChallengeCard(DailyChallenge(date, ttt, progress = 1), streakDays = 0, onPlay = {})
                        DailyChallengeCard(done, streakDays = 3, onPlay = {})
                        ChallengeCompletedBanner(ChallengeCompletion(done, points = 65, streakBonus = 15, petTreat = true))
                        BadgeUnlockedBanner(
                            com.example.onespace.domain.games.badge.GameBadges.all.filter { it.id == "snake_15" || it.id == "tile_512" }
                        )
                    }
                }
            }
        }
        shot("c1-challenge-states")
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w392dp-h1300dp-xxhdpi")
class ChallengeScreenshotTest : ChallengeShots("light")

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w392dp-h1300dp-night-xxhdpi")
class ChallengeScreenshotDarkTest : ChallengeShots("dark")
