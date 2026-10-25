package com.example.onespace.feature.account.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.Egg
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import com.example.onespace.core.designsystem.*
import com.example.onespace.domain.session.model.PetStage
import com.example.onespace.domain.session.model.PetState
import com.example.onespace.domain.session.model.ShopItem
import com.example.onespace.domain.session.model.ShopState
import com.example.onespace.domain.session.model.StreakState
import com.example.onespace.domain.session.repository.Language
import com.example.onespace.domain.session.repository.SessionPreferences
import com.example.onespace.domain.session.repository.ThemeMode

data class AccountGamificationState(
    val totalPoints: Int = 0,
    val streak: StreakState = StreakState(),
    val pet: PetState = PetState(),
    val shop: ShopState = ShopState()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountScreen(
    authEmail: String?,
    gamification: AccountGamificationState = AccountGamificationState(),
    onFeedPet: () -> Unit = {},
    onPurchase: (ShopItem) -> Unit = {},
    onSettings: () -> Unit,
    onExitAccount: () -> Unit,
    onWrapped: () -> Unit = {},
    onNotifications: () -> Unit = {},
    onPlaylists: () -> Unit = {},
    onDownloads: () -> Unit = {},
    onBadges: () -> Unit = {}
) {
    var showLogout by remember { mutableStateOf(false) }
    var showShop by remember { mutableStateOf(false) }
    var showAvatarEditor by remember { mutableStateOf(false) }

    FeaturePage {
        item {
            // F-COM-03: avatar with initial; F-COM-09: frame ring by milestone.
            val frame = AvatarFrameStore.frameOf(AvatarFrameStore.selected)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // F-COM-09: Edit button on avatar opens frame + avatar picker.
                Box {
                    Box(
                        Modifier.size(72.dp).background(
                            androidx.compose.ui.graphics.Color(frame.color).copy(alpha = 0.25f), CircleShape
                        ).border(
                            BorderStroke(3.dp, androidx.compose.ui.graphics.Color(frame.color)),
                            CircleShape
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            (authEmail?.take(1) ?: "K").uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = androidx.compose.ui.graphics.Color(frame.color)
                        )
                    }
                    Surface(
                        onClick = { showAvatarEditor = true },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.BottomEnd).size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Edit, contentDescription = "Đổi avatar",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        authEmail ?: "Khách",
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (authEmail == null) "Chế độ dùng thử · tiến độ tạm thời"
                        else "Tài khoản OneSpace · khung " + frame.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            // F-COM-08: badge count on profile; tap opens the full grid.
            val earnedCount = listOf(
                true,
                gamification.totalPoints >= 50,
                gamification.totalPoints >= 100,
                gamification.streak.currentStreak >= 3,
                gamification.streak.currentStreak >= 7,
                gamification.pet.stage == PetStage.ADULT
            ).count { it }
            ContentCard(
                "Huy hiệu",
                earnedCount.toString() + "/6 huy hiệu đã đạt",
                Icons.Outlined.Star,
                onBadges
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Điểm", "${gamification.totalPoints}", Icons.Outlined.Star, Modifier.weight(1f))
                StatCard("Chuỗi ngày", "${gamification.streak.currentStreak} ngày",
                    Icons.Outlined.LocalFireDepartment, Modifier.weight(1f))
                StatCard("Thú cưng", gamification.pet.stage.displayName,
                    Icons.Outlined.Egg, Modifier.weight(1f))
            }
        }
        // Pet detail
        item { PetCard(gamification.pet, onFeedPet) }
        // Shop entry
        item {
            ContentCard(
                "Cửa hàng đổi quà",
                "Mở game, đồ pet, khung avatar",
                Icons.Filled.ShoppingBag
            ) { showShop = true }
        }
        item { ContentCard("Đã tải", "Video, nhạc và bài báo đã lưu để xem offline", Icons.Filled.Download, onDownloads) }
        item { ContentCard("Playlist của tôi", "Mọi playlist video và nhạc, quản lý một chỗ", Icons.Filled.LibraryMusic, onPlaylists) }
        item { ContentCard("Tổng kết tháng", "Điểm, chuỗi ngày và huy hiệu của bạn", Icons.Filled.Insights, onWrapped) }
        item { ContentCard("Nhắc nhở", "Nhiệm vụ hằng ngày và giữ chuỗi ngày", Icons.Filled.Notifications, onNotifications) }
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

    if (showAvatarEditor) {
        AvatarEditorSheet(
            totalPoints = gamification.totalPoints,
            onDismiss = { showAvatarEditor = false }
        )
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

    if (showShop) {
        ShopSheet(gamification.shop, gamification.totalPoints, onPurchase) { showShop = false }
    }
}

@Composable
private fun PetCard(pet: PetState, onFeed: () -> Unit) {
    val stageEmoji = when (pet.stage) {
        PetStage.EGG -> "🥚"
        PetStage.BABY -> "🐣"
        PetStage.TEEN -> "🐥"
        PetStage.ADULT -> "🦜"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            // Pet visual: large emoji + gradient circle
            Box(
                Modifier.size(72.dp).background(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                        )
                    ),
                    CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(stageEmoji, style = androidx.compose.ui.text.TextStyle(fontSize = 36.sp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    pet.stage.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    when (pet.stage) {
                        PetStage.EGG -> "Hoàn thành nhiệm vụ để trứng nở"
                        PetStage.BABY -> "Cho ăn để lớn lên"
                        PetStage.TEEN -> "Sắp trưởng thành!"
                        PetStage.ADULT -> "Thú cưng đã trưởng thành"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = onFeed, enabled = pet.canFeed) {
                    Text(if (pet.canFeed) "Cho ăn (−30 điểm)" else "Đã cho ăn hôm nay")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShopSheet(state: ShopState, totalPoints: Int, onPurchase: (ShopItem) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Cửa hàng đổi quà", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "Số dư: $totalPoints điểm",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            state.items.forEach { item ->
                val owned = state.isOwned(item)
                ListItem(
                    leadingContent = {
                        // Emoji visual per item
                        Box(
                            Modifier.size(40.dp).background(
                                if (owned) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(item.emoji, style = MaterialTheme.typography.titleLarge)
                        }
                    },
                    headlineContent = { Text(item.name) },
                    supportingContent = { Text(item.description) },
                    trailingContent = {
                        when {
                            owned -> Text("Đã có", color = MaterialTheme.colorScheme.primary)
                            else -> OutlinedButton(onClick = { onPurchase(item) }) {
                                Text("${item.price} điểm")
                            }
                        }
                    }
                )
            }
            Spacer(Modifier.height(16.dp))
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


/** One achievement cell: colored when earned, grey when locked (F-COM-08). */
@Composable
private fun BadgeCell(name: String, icon: androidx.compose.ui.graphics.vector.ImageVector, earned: Boolean, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.background(
            if (earned) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.shapes.medium
        ).padding(vertical = 12.dp)
    ) {
        Icon(
            icon, contentDescription = null,
            tint = if (earned) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )
        Text(name, style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text(
            if (earned) "Đạt được" else "Chưa mở",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** F-COM-10: monthly recap with a share card. */
@Composable
internal fun WrappedScreen(
    authEmail: String?,
    gamification: AccountGamificationState
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    FeaturePage {
        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("OneSpace của bạn tháng này", style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        (authEmail ?: "Khách"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${gamification.totalPoints}", style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("điểm", style = MaterialTheme.typography.labelMedium)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${gamification.streak.currentStreak}", style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("chuỗi ngày", style = MaterialTheme.typography.labelMedium)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(gamification.pet.stage.displayName, style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("thú cưng", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
        // Per-section visual breakdown
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎬", style = MaterialTheme.typography.headlineLarge)
                    Text("Video", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎵", style = MaterialTheme.typography.headlineLarge)
                    Text("Nhạc", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📰", style = MaterialTheme.typography.headlineLarge)
                    Text("Tin", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎮", style = MaterialTheme.typography.headlineLarge)
                    Text("Game", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Button(
                onClick = {
                    val text = "Tháng này tôi đạt " + gamification.totalPoints + " điểm và giữ chuỗi " +
                        gamification.streak.currentStreak + " ngày trên OneSpace!"
                    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(android.content.Intent.createChooser(send, "Chia sẻ tổng kết"))
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Chia sẻ thẻ tổng kết") }
        }
    }
}

/** F-COM-12: reminder hub with a global switch, per-day reminders stay local. */
@Composable
internal fun NotificationsScreen(
    notificationsEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    var quietHours by remember { mutableStateOf(true) }
    var streakReminder by remember { mutableStateOf(true) }
    FeaturePage {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Nhắc nhiệm vụ hằng ngày", style = MaterialTheme.typography.bodyLarge)
                    Text("9:00 · nhắc nhiệm vụ hôm nay chưa làm",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = notificationsEnabled, onCheckedChange = onToggle)
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Nhắc giữ chuỗi ngày", style = MaterialTheme.typography.bodyLarge)
                    Text("20:00 · chỉ nhắc khi chưa hoạt động hôm nay",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = streakReminder && notificationsEnabled, onCheckedChange = { streakReminder = it })
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Giờ yên tĩnh", style = MaterialTheme.typography.bodyLarge)
                    Text("22:00–7:00 · không hiển thị nhắc nhở",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = quietHours, onCheckedChange = { quietHours = it })
            }
        }
        item { PrototypeNote("Bản demo hiển thị nhắc trong ứng dụng; thông báo đẩy nằm trong bản phát hành.") }
    }
}


/**
 * F-VID-15: the single playlist hub — video and music playlists live
 * together here, not on each tab's feed.
 */
@Composable
internal fun PlaylistsScreen(
    onOpenVideoPlaylist: (String) -> Unit,
    onOpenMusicPlaylist: (String) -> Unit
) {
    val playlists = com.example.onespace.core.designsystem.AppPlaylistStore.playlists
    FeaturePage {
        if (playlists.isEmpty()) {
            item {
                Text(
                    "Chưa có playlist nào. Mở một video hoặc bài hát và chọn \"Playlist\" để tạo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        }
        items(playlists.values.toList(), key = { it.name }) { pl ->
            val subtitle = buildList {
                if (pl.videoIds.isNotEmpty()) add(pl.videoIds.size.toString() + " video")
                if (pl.trackIds.isNotEmpty()) add(pl.trackIds.size.toString() + " bài hát")
            }.joinToString(" · ")
            ContentCard(
                title = pl.name,
                subtitle = if (subtitle.isBlank()) "Trống" else subtitle,
                icon = Icons.Filled.LibraryMusic,
                onClick = {
                    if (pl.videoIds.isNotEmpty()) onOpenVideoPlaylist(pl.name)
                    else onOpenMusicPlaylist(pl.name)
                }
            )
        }
    }
}


/**
 * F-VID-07 / F-MUS-12: the profile-level "Đã tải" hub — one screen, tab
 * chips per content type, cards below.
 */
@Composable
internal fun DownloadedScreen(
    videoItems: List<com.example.onespace.domain.session.repository.DownloadedContent> = emptyList(),
    musicItems: List<com.example.onespace.domain.session.repository.DownloadedContent> = emptyList(),
    newsItems: List<com.example.onespace.domain.session.repository.DownloadedContent> = emptyList(),
    onOpenVideo: (String) -> Unit = {},
    onOpenMusic: (String) -> Unit = {},
    onOpenArticle: (String) -> Unit = {}
) {
    var tab by remember { mutableStateOf("video") }
    val items = when (tab) {
        "music" -> musicItems
        "news" -> newsItems
        else -> videoItems
    }
    val onOpen: (String) -> Unit = when (tab) {
        "music" -> onOpenMusic
        "news" -> onOpenArticle
        else -> onOpenVideo
    }
    FeaturePage {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = tab == "video", onClick = { tab = "video" },
                    label = { Text("Video (" + videoItems.size + ")") })
                FilterChip(selected = tab == "music", onClick = { tab = "music" },
                    label = { Text("Music (" + musicItems.size + ")") })
                FilterChip(selected = tab == "news", onClick = { tab = "news" },
                    label = { Text("Báo (" + newsItems.size + ")") })
            }
        }
        if (items.isEmpty()) {
            item {
                Text(
                    when (tab) {
                        "music" -> "Chưa tải bài hát nào. Bấm nút tải trên một bài trong tab Nhạc."
                        "news" -> "Chưa tải bài báo nào."
                        else -> "Chưa tải video nào. Bấm nút tải trên một video để thêm vào đây."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        } else {
            items(items, key = { tab + "-" + it.contentId }) { entry ->
                val icon = when (tab) {
                    "music" -> Icons.Filled.MusicNote
                    "news" -> Icons.Filled.Newspaper
                    else -> Icons.Filled.PlayCircle
                }
                ContentCard(
                    title = entry.title,
                    subtitle = entry.subtitle,
                    icon = icon,
                    onClick = { onOpen(entry.contentId) }
                )
            }
        }
    }
}


/** F-COM-08: full badge grid — opens from the profile card. */
@Composable
internal fun BadgeGridScreen(totalPoints: Int, currentStreak: Int, petStage: PetStage) {
    val badges = listOf(
        Triple("Người mới", Icons.Outlined.Star, true),
        Triple("50 điểm", Icons.Outlined.Star, totalPoints >= 50),
        Triple("100 điểm", Icons.Outlined.Star, totalPoints >= 100),
        Triple("Chuỗi 3 ngày", Icons.Outlined.LocalFireDepartment, currentStreak >= 3),
        Triple("Chuỗi 7 ngày", Icons.Outlined.LocalFireDepartment, currentStreak >= 7),
        Triple("Thú cưng trưởng thành", Icons.Outlined.Egg, petStage == PetStage.ADULT)
    )
    FeaturePage {
        items(badges.size) { idx ->
            val (name, icon, earned) = badges[idx]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Icon(
                    icon, contentDescription = null,
                    tint = if (earned) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(32.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (earned) "Đã đạt" else "Chưa mở",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (earned) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/** F-COM-09: bottom sheet to change avatar + pick a frame. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AvatarEditorSheet(totalPoints: Int, onDismiss: () -> Unit) {
    val avatars = listOf("😀", "😎", "🦊", "🐼", "🦁", "🐧", "🍀", "⭐")
    var selectedAvatar by remember { mutableStateOf(avatars.first()) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle("Hình đại diện")
            avatars.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { emoji ->
                        val selected = selectedAvatar == emoji
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                )
                                .clickable { selectedAvatar = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
            }
            SectionTitle("Khung avatar")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AvatarFrameStore.frames.forEach { f ->
                    val unlocked = totalPoints >= f.unlockPoints
                    Box(
                        Modifier.size(44.dp).background(
                            if (unlocked) androidx.compose.ui.graphics.Color(f.color).copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        ).border(
                            BorderStroke(
                                if (AvatarFrameStore.selected == f.id) 3.dp else 1.dp,
                                if (unlocked) androidx.compose.ui.graphics.Color(f.color)
                                else MaterialTheme.colorScheme.outlineVariant
                            ),
                            CircleShape
                        ).clickable(enabled = unlocked) { AvatarFrameStore.selected = f.id },
                        contentAlignment = Alignment.Center
                    ) {
                        if (unlocked) Text(f.name.take(1), color = androidx.compose.ui.graphics.Color(f.color),
                            fontWeight = FontWeight.Bold)
                        else Text("🔒", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Xong") }
        }
    }
}
