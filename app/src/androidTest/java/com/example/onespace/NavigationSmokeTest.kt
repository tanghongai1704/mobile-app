package com.example.onespace

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        compose.onNodeWithContentDescription("Expand").performClick()
        compose.onNodeWithText("Float & explore").assertIsDisplayed()
    }

    @Test fun articleBookmarkSurvivesSwitchingTabs() {
        compose.onNodeWithText("News").performClick()
        compose.onNodeWithText("A greener city starts with small ideas").performClick()
        compose.onNodeWithText("Save story").performClick()
        compose.onNodeWithText("Music").performClick()
        compose.onNodeWithText("News").performClick()
        compose.onNodeWithText("Saved ✓").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Saved (1)").performClick()
        compose.onNodeWithText("A greener city starts with small ideas").assertIsDisplayed()
    }

    @Test fun musicMiniPlayerReopensNowPlaying() {
        compose.onNodeWithText("Music").performClick()
        compose.onNodeWithText("Morning light").performClick()
        compose.onNodeWithText("News").performClick()
        compose.onNodeWithText("Morning light").performClick()
        compose.onNodeWithText("ON REPEAT").assertIsDisplayed()
        compose.onNodeWithContentDescription("Pause").assertIsDisplayed()
    }
}
