package com.example.onespace.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * App language, set once at the root from the saved preference ([OneSpaceTheme]). Vietnamese is
 * the baseline; English is the second language. Switching recomposes the whole UI at once, no restart.
 */
val LocalEnglish = staticCompositionLocalOf { false }

/** Picks the Vietnamese or English wording for a UI string. */
@Composable
@ReadOnlyComposable
fun tr(vi: String, en: String): String = if (LocalEnglish.current) en else vi

/**
 * Text that is built outside composition (ViewModels, domain) and shown later: it carries both
 * wordings and is resolved by the UI with [resolve].
 */
data class UiText(val vi: String, val en: String) {
    @Composable
    @ReadOnlyComposable
    fun resolve(): String = tr(vi, en)

    fun resolve(english: Boolean): String = if (english) en else vi
}
