package com.example.onespace.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun FeaturePage(eyebrow: String, title: String, subtitle: String, content: LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(eyebrow.uppercase(), color = Mint, style = MaterialTheme.typography.labelMedium)
                Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        content()
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

@Composable
fun ContentCard(title: String, subtitle: String, mark: String, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = Panel)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(58.dp).background(Mint.copy(alpha = .12f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center) {
                Text(mark, color = Mint, style = MaterialTheme.typography.titleLarge)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", color = Mint, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
fun Artwork(label: String, modifier: Modifier = Modifier) {
    Box(modifier.background(Brush.linearGradient(listOf(Color(0xFF355C4D), Color(0xFF172C32))),
        RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("◉", color = Mint, style = MaterialTheme.typography.displayLarge)
            Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun PrototypeNote(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
}

