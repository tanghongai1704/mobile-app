package com.example.onespace.feature.account.presentation

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.example.onespace.core.designsystem.*

@Composable
internal fun AccountScreen(onSettings: () -> Unit) {
    FeaturePage("Your space", "Hello, explorer.", "Guest mode · Discover at your own pace.") {
        item { ContentCard("Guest profile", "No account required for this prototype", "○", {}) }
        item { ContentCard("Preferences", "Playback and data usage", "⚙", onSettings) }
        item { PrototypeNote("Sign-in and cloud sync are planned for a later phase.") }
    }
}

@Composable
internal fun SettingsScreen(dataSaver: Boolean, onDataSaver: (Boolean) -> Unit) {
    FeaturePage("Preferences", "Make it yours.", "A simple starting point for your space.") {
        item {
            ListItem(
                headlineContent = { Text("Data saver") },
                supportingContent = { Text("Demo preference · Streaming is not connected") },
                trailingContent = { Switch(dataSaver, onDataSaver) }
            )
        }
        item { PrototypeNote("Preview build · Android 8.0+ · Preferences currently survive UI recreation, not a fresh installation.") }
    }
}
