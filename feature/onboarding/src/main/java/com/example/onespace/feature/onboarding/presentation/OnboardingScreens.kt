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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Splash: one brand beat, then onboarding. Never blocks the 3-second budget. */
@Composable
internal fun SplashScreen(onNext: () -> Unit) {
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(600); onNext() }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.surface
    ))), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("oneSpace", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Text("4 trong 1 · liền mạch", style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Three value/permission/continuity steps; step 3 picks at least 3 topics. */
@Composable
internal fun OnboardingScreen(vm: OnboardingViewModel, onDone: () -> Unit) {
    val pager = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val selected by vm.selected.collectAsState()
    val enough = selected.size >= 3

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            when (page) {
                0 -> InfoStep("Tất cả trong một app",
                    "Video, nhạc, tin tức và minigame cùng một không gian giải trí.", null)
                1 -> MultitaskStep()
                else -> TopicsStep(vm.topics, selected, vm::toggle)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { vm.continueAsGuest(onDone) }) { Text("Bỏ qua") }
            Spacer(Modifier.weight(1f))
        }
        Row(
            Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) { repeat(3) { index -> Box(Modifier.padding(4.dp).size(8.dp).background(
            if (index == pager.currentPage) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)) } }
        Button(
            onClick = { if (pager.currentPage < 2) scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } else onDone() },
            enabled = pager.currentPage < 2 || enough,
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

/** Mock login: email + OTP fields, Google button, and an explicit guest path. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LoginScreen(vm: OnboardingViewModel, onDone: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var showGuest by remember { mutableStateOf(false) }
    var touched by remember { mutableStateOf(false) }
    val signingIn by vm.signingIn.collectAsState()
    val emailError = touched && !vm.isValidEmail(email)

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(32.dp))
        Text("Đăng nhập / Đăng ký", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text("Một tài khoản cho cả bốn phân hệ.", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = email, onValueChange = { email = it; touched = true },
            label = { Text("Email") },
            isError = emailError,
            supportingText = { if (emailError) Text("Email chưa hợp lệ — kiểm tra lại giúp mình nhé.") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = otp, onValueChange = { otp = it },
            label = { Text("Mã OTP (demo: bất kỳ)") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                if (!vm.isValidEmail(email)) touched = true
                else vm.signIn(email, onDone)
            },
            // TC07: the button stays enabled; validation shows inline on submit.
            enabled = !signingIn,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(if (signingIn) "Đang đăng nhập..." else "Tiếp tục với email")
        }
        OutlinedButton(onClick = { vm.signIn(email.ifBlank { "demo.onespace@gmail.com" }, onDone) },
            modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text("Tiếp tục với Google")
        }
        TextButton(onClick = { showGuest = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Dùng thử không cần tài khoản")
        }
    }

    if (showGuest) {
        ModalBottomSheet(onDismissRequest = { showGuest = false }) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Dùng thử không cần tài khoản",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("Đủ 4 phân hệ ở mức cơ bản. Điểm và tiến độ sẽ được giữ khi bạn tạo tài khoản.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { showGuest = false; vm.continueAsGuest(onDone) },
                    modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("Tiếp tục dùng thử")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
