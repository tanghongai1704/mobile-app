package com.example.onespace.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Bundled Google Fonts, including Vietnamese glyphs; no network/font provider required. */
@OptIn(ExperimentalTextApi::class)
private fun variableFamily(resource: Int) = FontFamily(
    Font(resource, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(resource, weight = FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(resource, weight = FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(resource, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700)))
)

private val DisplayFont = variableFamily(R.font.manrope)
private val BodyFont = variableFamily(R.font.roboto)
private val baseline = Typography()

val OneSpaceTypography = Typography(
    displayLarge = baseline.displayLarge.copy(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold),
    displayMedium = baseline.displayMedium.copy(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold),
    displaySmall = baseline.displaySmall.copy(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold),
    headlineLarge = baseline.headlineLarge.copy(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    headlineMedium = baseline.headlineMedium.copy(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold),
    titleLarge = baseline.titleLarge.copy(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold),
    titleMedium = baseline.titleMedium.copy(fontFamily = BodyFont, fontWeight = FontWeight.SemiBold),
    titleSmall = baseline.titleSmall.copy(fontFamily = BodyFont, fontWeight = FontWeight.Medium),
    bodyLarge = baseline.bodyLarge.copy(fontFamily = BodyFont, lineHeight = 26.sp),
    bodyMedium = baseline.bodyMedium.copy(fontFamily = BodyFont, lineHeight = 22.sp),
    bodySmall = baseline.bodySmall.copy(fontFamily = BodyFont, lineHeight = 18.sp),
    labelLarge = baseline.labelLarge.copy(fontFamily = BodyFont, fontWeight = FontWeight.Medium),
    labelMedium = baseline.labelMedium.copy(fontFamily = BodyFont, fontWeight = FontWeight.Medium),
    labelSmall = baseline.labelSmall.copy(fontFamily = BodyFont, fontWeight = FontWeight.Medium)
)
