package com.example.onespace.core.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * OneSpace "Coral Spark" brand palette.
 * Generated with the Material Theme Builder algorithm (HCT, Vibrant variant) from the seed
 * #E5484D. Accent roles (primary/secondary/tertiary/error) keep the vibrant tones; the neutral
 * palettes are desaturated (chroma 0.6 / 3) so surfaces, cards and bars read as clean
 * off-white / near-black instead of pink. Change the seed and regenerate; do not hand-edit
 * single roles. Every UI colour must come from these schemes or from [OneSpaceExtendedColors]
 * (ExtendedColors.kt); feature modules never declare their own colours.
 */

internal val LightColors = lightColorScheme(
    primary = Color(0xFFAF242E),
    onPrimary = Color(0xFFFFEFEE),
    primaryContainer = Color(0xFFFF7574),
    onPrimaryContainer = Color(0xFF4E0009),
    secondary = Color(0xFF9E3653),
    onSecondary = Color(0xFFFFEFF0),
    secondaryContainer = Color(0xFFFFC2CC),
    onSecondaryContainer = Color(0xFF84213F),
    tertiary = Color(0xFF7243A9),
    onTertiary = Color(0xFFFAEFFF),
    tertiaryContainer = Color(0xFFCA9CFF),
    onTertiaryContainer = Color(0xFF450E7B),
    error = Color(0xFFB02500),
    onError = Color(0xFFFFEFEC),
    errorContainer = Color(0xFFF95630),
    onErrorContainer = Color(0xFF520C00),
    background = Color(0xFFFBF5F4),
    onBackground = Color(0xFF302E2E),
    surface = Color(0xFFFBF5F4),
    onSurface = Color(0xFF302E2E),
    surfaceVariant = Color(0xFFE1DCDB),
    onSurfaceVariant = Color(0xFF5D5B5A),
    outline = Color(0xFF797676),
    outlineVariant = Color(0xFFB0ACAB),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5EFEF),
    surfaceContainer = Color(0xFFECE7E6),
    surfaceContainerHigh = Color(0xFFE7E1E0),
    surfaceContainerHighest = Color(0xFFE1DCDB),
    inverseSurface = Color(0xFF0F0E0E),
    inverseOnSurface = Color(0xFFA09C9B),
    inversePrimary = Color(0xFFFC5C5F),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFFAF242E),
    surfaceDim = Color(0xFFD8D3D2),
    surfaceBright = Color(0xFFFBF5F4)
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFFFF8D8A),
    onPrimary = Color(0xFF64000E),
    primaryContainer = Color(0xFFFF7574),
    onPrimaryContainer = Color(0xFF4E0009),
    secondary = Color(0xFFFC7E9C),
    onSecondary = Color(0xFF560021),
    secondaryContainer = Color(0xFF83203F),
    onSecondaryContainer = Color(0xFFFFC0CB),
    tertiary = Color(0xFFCEA3FF),
    onTertiary = Color(0xFF49157F),
    tertiaryContainer = Color(0xFFC492FE),
    onTertiaryContainer = Color(0xFF3E0174),
    error = Color(0xFFFF7351),
    onError = Color(0xFF450900),
    errorContainer = Color(0xFFB92902),
    onErrorContainer = Color(0xFFFFD2C8),
    background = Color(0xFF0F0E0E),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF0F0E0E),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF272525),
    onSurfaceVariant = Color(0xFFAEAAA9),
    outline = Color(0xFF787574),
    outlineVariant = Color(0xFF4A4747),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF141313),
    surfaceContainer = Color(0xFF1B1919),
    surfaceContainerHigh = Color(0xFF211F1F),
    surfaceContainerHighest = Color(0xFF272525),
    inverseSurface = Color(0xFFFEF8F7),
    inverseOnSurface = Color(0xFF575454),
    inversePrimary = Color(0xFFB42731),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFFFF8D8A),
    surfaceDim = Color(0xFF0F0E0E),
    surfaceBright = Color(0xFF2E2C2B)
)
