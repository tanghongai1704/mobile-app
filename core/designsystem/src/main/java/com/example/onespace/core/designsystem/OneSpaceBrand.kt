package com.example.onespace.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp

@Composable
fun OneSpaceBrand(modifier: Modifier = Modifier, compact: Boolean = false) {
    if (compact) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BrandMark(Modifier.size(48.dp).padding(12.dp))
            BrandName()
        }
    } else {
        Column(modifier, horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BrandMark(Modifier.size(88.dp).padding(22.dp))
            BrandName()
        }
    }
}

@Composable
private fun BrandName() {
    Text("oneSpace", style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun BrandMark(modifier: Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    Surface(shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer) {
        Canvas(modifier) {
            val radius = size.minDimension * .22f
            drawCircle(primary, radius, Offset(size.width * .25f, size.height * .25f))
            drawCircle(secondary, radius, Offset(size.width * .75f, size.height * .25f))
            drawCircle(tertiary, radius, Offset(size.width * .25f, size.height * .75f))
            drawCircle(primary, radius, Offset(size.width * .75f, size.height * .75f))
        }
    }
}
