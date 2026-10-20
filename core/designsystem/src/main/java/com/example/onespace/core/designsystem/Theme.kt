package com.example.onespace.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Mint = Color(0xFFB8F36E)
val Ink = Color(0xFF101311)
val Panel = Color(0xFF1D2420)

@Composable
fun OneSpaceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Mint, onPrimary = Ink, secondary = Color(0xFF9BD8D1),
            background = Ink, surface = Ink, surfaceVariant = Panel,
            onBackground = Color(0xFFF2F5EF), onSurface = Color(0xFFF2F5EF),
            onSurfaceVariant = Color(0xFFABB7AD)
        ),
        content = content
    )
}

