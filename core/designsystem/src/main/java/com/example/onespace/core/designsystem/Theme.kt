package com.example.onespace.core.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * M3 theming: full light and dark color schemes generated from the OneSpace brand
 * (mint + teal on green-tinted ink), selected by system dark mode. Roles are always
 * paired container/on-container so components keep accessible contrast in both modes.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF41690F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC3F491),
    onPrimaryContainer = Color(0xFF0E2000),
    secondary = Color(0xFF006A68),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF9CF1F0),
    onSecondaryContainer = Color(0xFF002020),
    tertiary = Color(0xFF566516),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD3E58B),
    onTertiaryContainer = Color(0xFF171E00),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF8FAF2),
    onBackground = Color(0xFF191D17),
    surface = Color(0xFFF8FAF2),
    onSurface = Color(0xFF191D17),
    surfaceVariant = Color(0xFFDEE3D0),
    onSurfaceVariant = Color(0xFF43483D),
    outline = Color(0xFF75796D),
    outlineVariant = Color(0xFFC6CAB9),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F4E6),
    surfaceContainer = Color(0xFFEBEEDF),
    surfaceContainerHigh = Color(0xFFE5E8D6),
    surfaceContainerHighest = Color(0xFFDFE2D2),
    inverseSurface = Color(0xFF2E312B),
    inverseOnSurface = Color(0xFFEFF2E4),
    inversePrimary = Color(0xFFA3D458),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFF41690F),
    surfaceDim = Color(0xFFD8DBCC),
    surfaceBright = Color(0xFFF8FAF2)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB8F36E),
    onPrimary = Color(0xFF223500),
    primaryContainer = Color(0xFF334E00),
    onPrimaryContainer = Color(0xFFC3F491),
    secondary = Color(0xFF9BD8D1),
    onSecondary = Color(0xFF003735),
    secondaryContainer = Color(0xFF00504C),
    onSecondaryContainer = Color(0xFFB0F0EA),
    tertiary = Color(0xFFBDE886),
    onTertiary = Color(0xFF2B3900),
    tertiaryContainer = Color(0xFF404D00),
    onTertiaryContainer = Color(0xFFD8EC9D),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF10140E),
    onBackground = Color(0xFFE0E4D8),
    surface = Color(0xFF10140E),
    onSurface = Color(0xFFE0E4D8),
    surfaceVariant = Color(0xFF43483D),
    onSurfaceVariant = Color(0xFFC4CCBA),
    outline = Color(0xFF8E927F),
    outlineVariant = Color(0xFF43483D),
    surfaceContainerLowest = Color(0xFF0B0F09),
    surfaceContainerLow = Color(0xFF181D16),
    surfaceContainer = Color(0xFF1D241C),
    surfaceContainerHigh = Color(0xFF272F25),
    surfaceContainerHighest = Color(0xFF323A31),
    inverseSurface = Color(0xFFE0E4D8),
    inverseOnSurface = Color(0xFF2E312B),
    inversePrimary = Color(0xFF41690F),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFFB8F36E),
    surfaceDim = Color(0xFF10140E),
    surfaceBright = Color(0xFF363B33)
)

@Composable
fun OneSpaceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // M3 dynamic color (Android 12+) is supported but off by default so the mint
    // brand identity stays consistent regardless of the device wallpaper.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
