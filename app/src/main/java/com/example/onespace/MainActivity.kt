package com.example.onespace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.onespace.app.ui.StartupViewModel
import com.example.onespace.app.ui.StartupUiState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.onespace.app.OneSpaceApp
import com.example.onespace.app.ui.MediaViewModel
import com.example.onespace.core.designsystem.OneSpaceTheme
import com.example.onespace.domain.session.repository.ThemeMode
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val startup: StartupViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { startup.uiState.value is StartupUiState.Loading }
        // Default scrims: system bar contrast adapts automatically to light/dark theme.
        enableEdgeToEdge()
        setContent {
            val media: MediaViewModel = hiltViewModel()
            val prefs by media.preferences.collectAsStateWithLifecycle()
            val darkTheme = when (prefs?.themeMode ?: ThemeMode.SYSTEM) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
            OneSpaceTheme(darkTheme = darkTheme) { OneSpaceApp() }
        }
    }
}
