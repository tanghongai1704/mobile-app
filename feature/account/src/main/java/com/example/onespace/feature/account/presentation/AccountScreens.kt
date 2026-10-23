package com.example.onespace.feature.account.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Egg
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.onespace.core.designsystem.*
import com.example.onespace.domain.session.repository.Language
import com.example.onespace.domain.session.repository.SessionPreferences
import com.example.onespace.domain.session.repository.ThemeMode

/**
 * Profile: identity, gamification summary and the way back to Settings/Login.
 * Points, streak and pet start honest at zero (gamification ships next phase).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountScreen(
    authEmail: String?,
    onSettings: () -> Unit,
    onExitAccount: () -> Unit
) {
    var showLogout by remember { mutableStateOf(false) }
    FeaturePage {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(64.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Person, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        authEmail ?: "Khách",
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (authEmail == null) "Chế độ dùng thử · tiến độ tạm thời"
                        else "Tài khoản OneSpace",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Điểm", "0", Icons.Outlined.Star, Modifier.weight(1f))
                StatCard("Chuỗi ngày", "0 ngày", Icons.Outlined.LocalFireDepartment, Modifier.weight(1f))
                StatCard("Thú cưng", "Trứng", Icons.Outlined.Egg, Modifier.weight(1f))
            }
        }
        item { ContentCard("Tùy chỉnh", "Giao diện, dữ liệu, âm thanh", Icons.Filled.Settings, onSettings) }
        item {
            when {
                authEmail == null -> Button(onClick = onExitAccount, modifier = Modifier.fillMaxWidth()) {
                    Text("Đăng nhập để giữ tiến độ")
                }
                else -> Button(onClick = { showLogout = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Đăng xuất")
                }
            }
        }
    }

    if (showLogout) {
        ModalBottomSheet(onDismissRequest = { showLogout = false }) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Bạn có muốn đăng xuất?", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold)
                Text("Nội dung đã lưu vẫn giữ nguyên trên máy này.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { showLogout = false }, modifier = Modifier.weight(1f)) {
                        Text("Hủy")
                    }
                    Button(onClick = { showLogout = false; onExitAccount() }, modifier = Modifier.weight(1f)) {
                        Text("Đăng xuất")
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(title, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    prefs: SessionPreferences,
    onThemeMode: (ThemeMode) -> Unit,
    onDataSaver: (Boolean) -> Unit,
    onLanguage: (Language) -> Unit,
    onNotifications: (Boolean) -> Unit,
    onSimulateOffline: (Boolean) -> Unit,
    onResetConflictRule: () -> Unit
) {
    FeaturePage {
        item {
            ListItem(
                headlineContent = { Text("Giao diện") },
                supportingContent = { Text("Áp dụng cho toàn bộ ứng dụng") }
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                SegmentedButton(
                    selected = prefs.themeMode == ThemeMode.SYSTEM,
                    onClick = { onThemeMode(ThemeMode.SYSTEM) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                    label = { Text("Hệ thống") }
                )
                SegmentedButton(
                    selected = prefs.themeMode == ThemeMode.LIGHT,
                    onClick = { onThemeMode(ThemeMode.LIGHT) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                    label = { Text("Sáng") }
                )
                SegmentedButton(
                    selected = prefs.themeMode == ThemeMode.DARK,
                    onClick = { onThemeMode(ThemeMode.DARK) },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                    label = { Text("Tối") }
                )
            }
        }
        item {
            ListItem(
                headlineContent = { Text("Ngôn ngữ") },
                supportingContent = { Text("Bản dịch tiếng Anh đến ở giai đoạn sau") }
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                SegmentedButton(
                    selected = prefs.language == Language.VI,
                    onClick = { onLanguage(Language.VI) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    label = { Text("Tiếng Việt") }
                )
                SegmentedButton(
                    selected = prefs.language == Language.EN,
                    onClick = { onLanguage(Language.EN) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    label = { Text("English") }
                )
            }
        }
        item {
            ListItem(
                modifier = Modifier.toggleable(
                    value = prefs.dataSaver, role = Role.Switch, onValueChange = onDataSaver),
                headlineContent = { Text("Tiết kiệm dữ liệu") },
                supportingContent = { Text("Giới hạn video ở mức 720p khi bật") },
                trailingContent = { Switch(prefs.dataSaver, onDataSaver) }
            )
        }
        item {
            ListItem(
                modifier = Modifier.toggleable(
                    value = prefs.notificationsEnabled, role = Role.Switch, onValueChange = onNotifications),
                headlineContent = { Text("Nhắc nhiệm vụ") },
                supportingContent = { Text("Quyền thông báo chỉ được xin khi bạn bật nhắc lần đầu") },
                trailingContent = { Switch(prefs.notificationsEnabled, onNotifications) }
            )
        }
        item {
            ListItem(
                modifier = Modifier.toggleable(
                    value = prefs.simulateOffline, role = Role.Switch, onValueChange = onSimulateOffline),
                headlineContent = { Text("Mô phỏng mất mạng (demo)") },
                supportingContent = { Text("Bật để xem các trạng thái offline/retry của feed") },
                trailingContent = { Switch(prefs.simulateOffline, onSimulateOffline) }
            )
        }
        item {
            ListItem(
                headlineContent = { Text("Quy tắc video + nhạc") },
                supportingContent = {
                    Text(if (prefs.conflictRule == null)
                        "Chưa có quy tắc đã nhớ — sẽ hỏi ở lần va chạm đầu tiên"
                    else "Đã nhớ: " + labelOf(prefs.conflictRule))
                },
                trailingContent = {
                    TextButton(onClick = onResetConflictRule, enabled = prefs.conflictRule != null) {
                        Text("Hỏi lại")
                    }
                }
            )
        }
    }
}

private fun labelOf(rule: com.example.onespace.domain.session.MediaConflictRule?): String = when (rule) {
    com.example.onespace.domain.session.MediaConflictRule.MUTE_VIDEO -> "tắt tiếng video khi mở nhạc"
    com.example.onespace.domain.session.MediaConflictRule.PAUSE_VIDEO -> "dừng video khi mở nhạc"
    com.example.onespace.domain.session.MediaConflictRule.KEEP_VIDEO -> "chỉ giữ video"
    null -> ""
}
