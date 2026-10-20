package com.example.onespace.feature.home.presentation

import androidx.compose.runtime.Composable
import androidx.compose.material3.Surface
import androidx.compose.ui.tooling.preview.Preview
import com.example.onespace.core.designsystem.OneSpaceTheme

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomePreview() {
    OneSpaceTheme { Surface { HomeScreen {} } }
}
