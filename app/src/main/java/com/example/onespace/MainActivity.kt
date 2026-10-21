package com.example.onespace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.onespace.app.OneSpaceApp
import com.example.onespace.core.designsystem.OneSpaceTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Default scrims: system bar contrast adapts automatically to light/dark theme.
        enableEdgeToEdge()
        setContent { OneSpaceTheme { OneSpaceApp() } }
    }
}
