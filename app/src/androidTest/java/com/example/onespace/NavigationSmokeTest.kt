package com.example.onespace

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import org.junit.Rule
import org.junit.Test

/** Run on an API 26+ device with connectedDebugAndroidTest. */
class NavigationSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun videoCanFloatOverNewsAndExpandAgain() {
        compose.onNodeWithText("Video").performClick()
        compose.onNodeWithText("A little closer to nature").performClick()
        compose.onNodeWithText("Float & explore").performClick()
        compose.onNodeWithText("A fresh perspective.").assertIsDisplayed()
        compose.onNodeWithText("Floating · Demo playing").assertIsDisplayed()
        saveScreenshot("news-floating-video")
        compose.onNodeWithText("Expand").performClick()
        compose.onNodeWithText("Float & explore").assertIsDisplayed()
        saveScreenshot("video")
    }

    @Test fun articleBookmarkSurvivesSwitchingTabs() {
        compose.onNodeWithText("News").performClick()
        compose.onNodeWithText("A greener city starts with small ideas").performClick()
        compose.onNodeWithText("Save story").performClick()
        saveScreenshot("article")
        compose.onNodeWithText("Music").performClick()
        compose.onNodeWithText("News").performClick()
        compose.onNodeWithText("Saved ✓").assertIsDisplayed()
        compose.onNodeWithText("‹ Back").performClick()
        compose.onNodeWithText("Saved (1)").performClick()
        compose.onNodeWithText("A greener city starts with small ideas").assertIsDisplayed()
    }

    @Test fun musicMiniPlayerReopensNowPlaying() {
        compose.onNodeWithText("Music").performClick()
        compose.onNodeWithText("Morning light").performClick()
        compose.onNodeWithText("News").performClick()
        compose.onNodeWithText("♫  Morning light").performClick()
        compose.onNodeWithText("ON REPEAT").assertIsDisplayed()
        compose.onNodeWithText("Pause").assertIsDisplayed()
        saveScreenshot("music")
        compose.onNodeWithText("Next →").performClick()
        compose.onNodeWithText("Somewhere quiet").assertIsDisplayed()
        compose.onNodeWithText("Pause").assertIsDisplayed()
    }

    @Test fun homeAndAllFiveTabsAreVisible() {
        compose.onNodeWithText("One space.\nMore possibilities.").assertIsDisplayed()
        saveScreenshot("home")
        compose.onNodeWithText("Video").performClick()
        compose.onNodeWithText("Worth your time.").assertIsDisplayed()
        compose.onNodeWithText("Music").performClick()
        compose.onNodeWithText("Find your rhythm.").assertIsDisplayed()
        compose.onNodeWithText("News").performClick()
        compose.onNodeWithText("A fresh perspective.").assertIsDisplayed()
        compose.onNodeWithText("Games").performClick()
        compose.onNodeWithText("A small escape.").assertIsDisplayed()
        saveScreenshot("games")
        compose.onNodeWithText("Home").performClick()
        compose.onNodeWithText("One space.\nMore possibilities.").assertIsDisplayed()
    }

    @Test fun gameControlsRemainVisibleAndBackReturnsToCatalog() {
        compose.onNodeWithText("Games").performClick()
        compose.onNodeWithText("2048").performClick()
        compose.onNodeWithText("Pause preview").assertIsDisplayed().performClick()
        compose.onNodeWithText("Resume preview").assertIsDisplayed().performClick()
        saveScreenshot("game-board")
        compose.onNodeWithText("‹ Back").performClick()
        compose.onNodeWithText("A small escape.").assertIsDisplayed()
    }

    @Test fun settingsSurviveActivityRecreation() {
        compose.onNodeWithText("Guest ○").performClick()
        compose.onNodeWithText("Hello, explorer.").assertIsDisplayed()
        compose.onNodeWithText("Preferences").performClick()
        compose.onNodeWithText("Data saver").assertIsDisplayed()
        compose.onNode(isToggleable()).performClick().assertIsOn()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Data saver").assertIsDisplayed()
        compose.onNode(isToggleable()).assertIsOn()
        saveScreenshot("settings")
        compose.onNodeWithText("‹ Back").performClick()
        compose.onNodeWithText("Hello, explorer.").assertIsDisplayed()
    }

    private fun saveScreenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(checkNotNull(instrumentation.targetContext.getExternalFilesDir(null)), "screenshots")
        check(directory.isDirectory || directory.mkdirs())
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            FileOutputStream(File(directory, "$name.png")).use { stream ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
            }
        } finally {
            bitmap.recycle()
        }
    }
}
