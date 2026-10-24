package com.example.onespace.feature.onboarding.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Three value/permission/continuity steps; step 3 picks at least 3 topics. */
@Composable
internal fun OnboardingScreen(
    topics: List<String>, selected: Set<String>, onToggle: (String) -> Unit,
    saving: Boolean, onFinish: () -> Unit, onSkip: () -> Unit
) {
    val pager = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val enough = selected.size >= 3

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            when (page) {
                0 -> InfoStep("Tất cả trong một app",
                    "Video, nhạc, tin tức và minigame cùng một không gian giải trí.", null)
                1 -> MultitaskStep()
                else -> TopicsStep(topics, selected, onToggle)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onSkip() }) { Text("Bỏ qua") }
            Spacer(Modifier.weight(1f))
        }
        Row(
            Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) { repeat(3) { index -> Box(Modifier.padding(4.dp).size(8.dp).background(
            if (index == pager.currentPage) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)) } }
        Button(
            onClick = { if (pager.currentPage < 2) scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } else onFinish() },
            enabled = !saving && (pager.currentPage < 2 || enough),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(when {
                pager.currentPage < 2 -> "Tiếp tục"
                enough -> "Bắt đầu"
                else -> "Chọn ít nhất 3 chủ đề"
            })
        }
    }
}

@Composable
private fun InfoStep(title: String, body: String, icon: ImageVector?) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center)
            Text(body, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Icon(Icons.Filled.PlayCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                Icon(Icons.Filled.MusicNote, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp))
                Icon(Icons.Filled.Newspaper, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp))
                Icon(Icons.Filled.SportsEsports, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            }
        }
    }
}

/** CSS-art style mock of the dock + PIP, retold with the existing card language. */
@Composable
private fun MultitaskStep() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Đa nhiệm liền mạch", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Text("Nhạc vẫn chạy khi bạn đọc tin;\nvideo thu nhỏ thành góc màn hình.",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card { Column(Modifier.padding(12.dp)) {
                Box(Modifier.size(width = 84.dp, height = 48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small))
                Text("Video · PIP", style = MaterialTheme.typography.labelMedium)
            } }
            Card { Column(Modifier.padding(12.dp)) {
                Box(Modifier.size(width = 120.dp, height = 24.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.small))
                Text("Dock nhạc", style = MaterialTheme.typography.labelMedium)
            } }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopicsStep(topics: List<String>, selected: Set<String>, onToggle: (String) -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Chọn sở thích", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text("Ba chủ đề trở lên giúp Home gợi ý đúng nội dung cho bạn.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            topics.forEach { topic ->
                FilterChip(
                    selected = topic in selected,
                    onClick = { onToggle(topic) },
                    label = { Text(topic) }
                )
            }
        }
    }
}
