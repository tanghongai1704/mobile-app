package com.example.onespace.feature.account.presentation

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
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

/** Phone accounts travel under an in-app address; show the number instead. */
internal fun displayIdentity(identity: String?): String =
    if (identity != null && identity.endsWith("@phone.onespace"))
        "0" + identity.removeSuffix("@phone.onespace")
    else identity ?: "Khách"

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
                        val avatarGlyph = AvatarFrameStore.avatar
                            ?: (authEmail?.take(1) ?: "K").uppercase()
                        Text(
                            avatarGlyph,
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (AvatarFrameStore.avatar == null)
                                androidx.compose.ui.graphics.Color(frame.color)
                            else androidx.compose.ui.graphics.Color.Unspecified
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
                        displayIdentity(authEmail),
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
            // F-COM-07: the pet is drawn (2D) and alive: it breathes,
            // blinks and sways while waiting.
            PetArt(pet.stage, Modifier.size(112.dp))
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
            // F-COM-15: each item reads as a mini poster: themed gradient
            // tile, emoji illustration, then name and price.
            state.items.forEach { item ->
                val owned = state.isOwned(item)
                val tint = shopTint(item.id)
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            Modifier.size(56.dp).background(
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(
                                        tint.copy(alpha = 0.9f),
                                        tint.copy(alpha = 0.45f)
                                    )
                                ),
                                androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(item.emoji, style = MaterialTheme.typography.headlineMedium)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold)
                            Text(item.description, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        when {
                            owned -> Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    "Đã có",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                            else -> OutlinedButton(onClick = { onPurchase(item) }) {
                                Text("${item.price} điểm")
                            }
                        }
                    }
                }
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
    onResetConflictRule: () -> Unit,
    onSetConflictRule: (com.example.onespace.domain.session.MediaConflictRule) -> Unit = {}
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
            // F-COM-13/F-COM-14: the row itself is the picker — the tick
            // list always shows the current rule, "Luôn hỏi" included.
            var showConflictPicker by remember { mutableStateOf(false) }
            ListItem(
                headlineContent = { Text("Xung đột âm thanh") },
                supportingContent = { Text(labelOf(prefs.conflictRule)) },
                trailingContent = {
                    TextButton(onClick = { showConflictPicker = true }) {
                        Text(if (prefs.conflictRule == null) "Luôn hỏi" else "Mặc định")
                    }
                },
                modifier = Modifier.clickable { showConflictPicker = true }
            )
            if (showConflictPicker) {
                AlertDialog(
                    onDismissRequest = { showConflictPicker = false },
                    title = { Text("Xung đột âm thanh") },
                    text = {
                        Column {
                            conflictOption(null, "Luôn hỏi", prefs.conflictRule) {
                                onResetConflictRule(); showConflictPicker = false
                            }
                            conflictOption(
                                com.example.onespace.domain.session.MediaConflictRule.MUTE_VIDEO,
                                labelOf(com.example.onespace.domain.session.MediaConflictRule.MUTE_VIDEO),
                                prefs.conflictRule
                            ) { onSetConflictRule(com.example.onespace.domain.session.MediaConflictRule.MUTE_VIDEO); showConflictPicker = false }
                            conflictOption(
                                com.example.onespace.domain.session.MediaConflictRule.PAUSE_VIDEO,
                                labelOf(com.example.onespace.domain.session.MediaConflictRule.PAUSE_VIDEO),
                                prefs.conflictRule
                            ) { onSetConflictRule(com.example.onespace.domain.session.MediaConflictRule.PAUSE_VIDEO); showConflictPicker = false }
                            conflictOption(
                                com.example.onespace.domain.session.MediaConflictRule.KEEP_VIDEO,
                                labelOf(com.example.onespace.domain.session.MediaConflictRule.KEEP_VIDEO),
                                prefs.conflictRule
                            ) { onSetConflictRule(com.example.onespace.domain.session.MediaConflictRule.KEEP_VIDEO); showConflictPicker = false }
                        }
                    },
                    confirmButton = {}
                )
            }
        }
    }
}

/** One radio row of the conflict picker; ticks the current rule. */
@Composable
private fun conflictOption(
    rule: com.example.onespace.domain.session.MediaConflictRule?,
    label: String,
    current: com.example.onespace.domain.session.MediaConflictRule?,
    onPick: (com.example.onespace.domain.session.MediaConflictRule?) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .clickable { onPick(rule) }
            .padding(vertical = 10.dp)
    ) {
        RadioButton(selected = rule == current, onClick = { onPick(rule) })
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun labelOf(rule: com.example.onespace.domain.session.MediaConflictRule?): String = when (rule) {
    com.example.onespace.domain.session.MediaConflictRule.MUTE_VIDEO -> "Ưu tiên nhạc — tắt tiếng video"
    com.example.onespace.domain.session.MediaConflictRule.PAUSE_VIDEO -> "Ưu tiên nhạc — dừng video"
    com.example.onespace.domain.session.MediaConflictRule.KEEP_VIDEO -> "Ưu tiên video"
    null -> "Luôn hỏi mỗi khi hai luồng cùng phát"
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
                        displayIdentity(authEmail),
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
        // F-COM-10: per-section breakdown — what the month looked like
        // in each entertainment area, with the share of total activity.
        item { SectionTitle("Tháng này của bạn ở từng mảng") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                WrappedSection(
                    emoji = "🎬", name = "Video", tint = 0xFFE65100,
                    statLine = "12 video · 3,4 giờ xem",
                    favLine = "Kênh hay nhất: Hành Trình 500 Blues",
                    share = 0.42f
                )
                WrappedSection(
                    emoji = "🎵", name = "Nhạc", tint = 0xFF6A1B9A,
                    statLine = "45 bài đã nghe · 3,2 giờ",
                    favLine = "Nghệ sĩ hay nhất: Mưa Tháng Sáu",
                    share = 0.31f
                )
                WrappedSection(
                    emoji = "📰", name = "Tin tức", tint = 0xFF0277BD,
                    statLine = "28 bài đọc · 96 phút",
                    favLine = "Chuyên mục hay nhất: Công nghệ",
                    share = 0.17f
                )
                WrappedSection(
                    emoji = "🎮", name = "Games", tint = 0xFF2E7D32,
                    statLine = "15 ván thắng · 4 huy hiệu",
                    favLine = "Chơi nhiều nhất: Đoán bài hát",
                    share = 0.10f
                )
            }
        }
        // F-COM-10: the month's little milestones.
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Khoảnh khắc đáng nhớ", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    WrappedMoment(Icons.Outlined.LocalFireDepartment,
                        "Giữ chuỗi ${gamification.streak.currentStreak} ngày liền")
                    WrappedMoment(Icons.Filled.MusicNote, "Trích xuất bài MP3 đầu tiên từ video")
                    WrappedMoment(Icons.Outlined.Star, "Lần đầu thắng game Đoán bài hát")
                }
            }
        }
        item {
            Button(
                onClick = {
                    val text = "Tháng này tôi đạt " + gamification.totalPoints + " điểm, giữ chuỗi " +
                        gamification.streak.currentStreak + " ngày và đủ cả xem - nghe - đọc - chơi trên OneSpace!"
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

/** F-COM-12: reminder hub; every switch persists in DataStore. */
@Composable
internal fun NotificationsScreen(
    notificationsEnabled: Boolean,
    streakReminder: Boolean,
    quietHours: Boolean,
    onToggle: (Boolean) -> Unit,
    onToggleStreakReminder: (Boolean) -> Unit,
    onToggleQuietHours: (Boolean) -> Unit
) {
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
                Switch(checked = streakReminder && notificationsEnabled, onCheckedChange = onToggleStreakReminder)
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
                Switch(checked = quietHours, onCheckedChange = onToggleQuietHours)
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

/** One entertainment area of the wrapped card. */
@Composable
private fun WrappedSection(
    emoji: String, name: String, tint: Long, statLine: String, favLine: String, share: Float
) {
    val tintColor = androidx.compose.ui.graphics.Color(tint)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(44.dp).background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(tintColor.copy(alpha = 0.85f), tintColor.copy(alpha = 0.45f))
                        ),
                        CircleShape
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, style = MaterialTheme.typography.titleLarge)
                }
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(statLine, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(favLine, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text((share * 100).toInt().toString() + "%",
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                    color = tintColor)
            }
            LinearProgressIndicator(
                progress = { share },
                color = tintColor,
                trackColor = tintColor.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
            )
        }
    }
}

/** One milestone line inside the wrapped card. */
@Composable
private fun WrappedMoment(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Per-item accent so the shop tiles feel illustrated, not enumerated. */
private fun shopTint(id: String): androidx.compose.ui.graphics.Color = when {
    id.startsWith("game_") -> androidx.compose.ui.graphics.Color(0xFF2E7D32)
    id.startsWith("pet_") -> androidx.compose.ui.graphics.Color(0xFFAD1457)
    id.endsWith("gold") -> androidx.compose.ui.graphics.Color(0xFFF9A825)
    id.endsWith("silver") -> androidx.compose.ui.graphics.Color(0xFF78909C)
    else -> androidx.compose.ui.graphics.Color(0xFF8D6E63)
}

/** F-COM-07: the pet drawn with 2D shapes, alive with idle animations. */
@Composable
internal fun PetArt(stage: PetStage, modifier: Modifier = Modifier) {
    val idle = rememberInfiniteTransition(label = "pet-idle")
    val scaleY by idle.animateFloat(
        initialValue = 1f, targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            androidx.compose.animation.core.RepeatMode.Reverse
        ), label = "breath"
    )
    val tilt by idle.animateFloat(
        initialValue = -2.5f, targetValue = 3f,
        animationSpec = infiniteRepeatable(
            tween(2300, easing = androidx.compose.animation.core.LinearOutSlowInEasing),
            androidx.compose.animation.core.RepeatMode.Reverse
        ), label = "tilt"
    )
    val eyeOpen by idle.animateFloat(
        initialValue = 1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 3600
                1f at 2850
                0.14f at 2990
                1f at 3140
            }
        ), label = "blink"
    )
    androidx.compose.foundation.Canvas(
        modifier.then(
            Modifier.graphicsLayer {
                this.scaleY = scaleY
                rotationZ = tilt
            }
        )
    ) {
        val r = size.minDimension / 2f
        // ground shadow keeps the pet planted while it moves
        drawCircle(
            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.10f),
            radius = r * 0.46f,
            center = Offset(size.width / 2f, size.height * 0.90f)
        )
        when (stage) {
            PetStage.EGG -> drawEgg(r)
            PetStage.BABY -> drawChick(r, scale = 0.66f, eyeOpen = eyeOpen, teen = false)
            PetStage.TEEN -> drawChick(r, scale = 0.82f, eyeOpen = eyeOpen, teen = true)
            PetStage.ADULT -> drawParrot(r, eyeOpen)
        }
    }
}

/** Egg with soft spots; the idle tilt makes it look like something stirs inside. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEgg(r: Float) {
    val cx = size.width / 2f
    val cy = size.height * 0.56f
    drawOval(
        color = androidx.compose.ui.graphics.Color(0xFFFFF6E3),
        topLeft = Offset(cx - r * 0.58f, cy - r * 0.80f),
        size = androidx.compose.ui.geometry.Size(r * 1.16f, r * 1.6f)
    )
    drawCircle(androidx.compose.ui.graphics.Color(0xFFF3E3B8), r * 0.09f,
        Offset(cx - r * 0.22f, cy - r * 0.34f))
    drawCircle(androidx.compose.ui.graphics.Color(0xFFF3E3B8), r * 0.06f,
        Offset(cx + r * 0.26f, cy + r * 0.12f))
    drawCircle(androidx.compose.ui.graphics.Color(0xFFF3E3B8), r * 0.05f,
        Offset(cx + r * 0.05f, cy - r * 0.6f))
}

/** Round chick for BABY/TEEN: tuft, cheeks appear when older. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawChick(
    r: Float, scale: Float, eyeOpen: Float, teen: Boolean
) {
    val cx = size.width / 2f
    val cy = size.height * 0.56f
    val bodyR = r * 0.52f * scale
    val body = androidx.compose.ui.graphics.Color(0xFFFFD75E)
    val belly = androidx.compose.ui.graphics.Color(0xFFFFF0C2)
    val foot = androidx.compose.ui.graphics.Color(0xFFF09A2D)
    // feet
    drawLine(foot, Offset(cx - bodyR * 0.38f, cy + bodyR * 0.86f),
        Offset(cx - bodyR * 0.38f, cy + bodyR * 1.18f), strokeWidth = r * 0.05f)
    drawLine(foot, Offset(cx + bodyR * 0.38f, cy + bodyR * 0.86f),
        Offset(cx + bodyR * 0.38f, cy + bodyR * 1.18f), strokeWidth = r * 0.05f)
    // tuft feathers on the head
    val tuft = androidx.compose.ui.graphics.Path().apply {
        moveTo(cx - bodyR * 0.05f, cy - bodyR * 0.95f)
        quadraticTo(cx - bodyR * 0.34f, cy - bodyR * (if (teen) 1.45f else 1.25f), cx - bodyR * 0.16f, cy - bodyR * (if (teen) 1.5f else 1.3f))
        quadraticTo(cx + bodyR * 0.02f, cy - bodyR * (if (teen) 1.4f else 1.2f), cx + bodyR * 0.08f, cy - bodyR * 0.92f)
    }
    drawPath(tuft, body)
    // body
    drawCircle(body, bodyR, Offset(cx, cy))
    // wings
    drawArc(belly, 100f, 110f, useCenter = true,
        topLeft = Offset(cx - bodyR * 1.42f, cy - bodyR * 0.5f),
        size = androidx.compose.ui.geometry.Size(bodyR * 1.25f, bodyR * 1.25f))
    drawArc(belly, -30f, 110f, useCenter = true,
        topLeft = Offset(cx + bodyR * 0.17f, cy - bodyR * 0.5f),
        size = androidx.compose.ui.geometry.Size(bodyR * 1.25f, bodyR * 1.25f))
    // cheeks only when older
    if (teen) {
        drawCircle(androidx.compose.ui.graphics.Color(0xFFF8BBD0).copy(alpha = 0.85f), bodyR * 0.13f,
            Offset(cx - bodyR * 0.52f, cy - bodyR * 0.12f))
        drawCircle(androidx.compose.ui.graphics.Color(0xFFF8BBD0).copy(alpha = 0.85f), bodyR * 0.13f,
            Offset(cx + bodyR * 0.52f, cy - bodyR * 0.12f))
    }
    // eyes with blink
    val eyeY = cy - bodyR * 0.22f
    listOf(-1, 1).forEach { side ->
        val ex = cx + side * bodyR * 0.34f
        drawCircle(androidx.compose.ui.graphics.Color.White, bodyR * 0.155f, Offset(ex, eyeY))
        drawCircle(androidx.compose.ui.graphics.Color(0xFF33261A),
            bodyR * 0.10f * eyeOpen.coerceIn(0.14f, 1f), Offset(ex, eyeY + bodyR * 0.01f))
        drawCircle(androidx.compose.ui.graphics.Color.White, bodyR * 0.032f, Offset(ex - bodyR * 0.02f, eyeY - bodyR * 0.028f))
    }
    // beak
    drawPath(androidx.compose.ui.graphics.Path().apply {
        moveTo(cx - bodyR * 0.14f, cy - bodyR * 0.04f)
        lineTo(cx + bodyR * 0.14f, cy - bodyR * 0.04f)
        lineTo(cx, cy + bodyR * 0.14f)
        close()
    }, androidx.compose.ui.graphics.Color(0xFFF57C00))
}

/** ADULT parrot: red head, green body, hooked beak, blue tail feathers. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawParrot(r: Float, eyeOpen: Float) {
    val cx = size.width / 2f
    val cy = size.height * 0.54f
    val bodyR = r * 0.5f
    val green = androidx.compose.ui.graphics.Color(0xFF66BB6A)
    val darkGreen = androidx.compose.ui.graphics.Color(0xFF3E8E41)
    val red = androidx.compose.ui.graphics.Color(0xFFEF5350)
    val tailBlue = androidx.compose.ui.graphics.Color(0xFF42A5F5)
    // tail feathers behind the body
    drawLine(tailBlue, Offset(cx - bodyR * 0.1f, cy + bodyR * 0.55f),
        Offset(cx - bodyR * 0.85f, cy + bodyR * 1.5f), strokeWidth = r * 0.075f)
    drawLine(red, Offset(cx + bodyR * 0.02f, cy + bodyR * 0.55f),
        Offset(cx - bodyR * 0.30f, cy + bodyR * 1.62f), strokeWidth = r * 0.075f)
    drawLine(androidx.compose.ui.graphics.Color(0xFFFFCA28), Offset(cx + bodyR * 0.12f, cy + bodyR * 0.55f),
        Offset(cx + bodyR * 0.42f, cy + bodyR * 1.45f), strokeWidth = r * 0.075f)
    // feet
    val foot = androidx.compose.ui.graphics.Color(0xFF8D6E63)
    drawLine(foot, Offset(cx - bodyR * 0.3f, cy + bodyR * 0.95f),
        Offset(cx - bodyR * 0.3f, cy + bodyR * 1.2f), strokeWidth = r * 0.045f)
    drawLine(foot, Offset(cx + bodyR * 0.3f, cy + bodyR * 0.95f),
        Offset(cx + bodyR * 0.3f, cy + bodyR * 1.2f), strokeWidth = r * 0.045f)
    // body + wing
    drawOval(green, topLeft = Offset(cx - bodyR * 0.78f, cy - bodyR * 0.5f),
        size = androidx.compose.ui.geometry.Size(bodyR * 1.56f, bodyR * 1.55f))
    drawArc(darkGreen, -25f, 130f, useCenter = true,
        topLeft = Offset(cx - bodyR * 1.05f, cy - bodyR * 0.42f),
        size = androidx.compose.ui.geometry.Size(bodyR * 1.15f, bodyR * 1.15f))
    // head
    drawCircle(red, bodyR * 0.56f, Offset(cx, cy - bodyR * 0.72f))
    // cream face patch + eye
    drawCircle(androidx.compose.ui.graphics.Color(0xFFFFF3E0), bodyR * 0.40f, Offset(cx + bodyR * 0.06f, cy - bodyR * 0.70f))
    val ex = cx + bodyR * 0.10f
    val ey = cy - bodyR * 0.74f
    drawCircle(androidx.compose.ui.graphics.Color(0xFF33261A),
        bodyR * 0.085f * eyeOpen.coerceIn(0.14f, 1f), Offset(ex, ey))
    drawCircle(androidx.compose.ui.graphics.Color.White, bodyR * 0.028f, Offset(ex - bodyR * 0.02f, ey - bodyR * 0.025f))
    // hooked beak pointing right
    drawPath(androidx.compose.ui.graphics.Path().apply {
        moveTo(cx + bodyR * 0.28f, cy - bodyR * 0.86f)
        quadraticTo(cx + bodyR * 0.92f, cy - bodyR * 0.80f, cx + bodyR * 0.62f, cy - bodyR * 0.40f)
        quadraticTo(cx + bodyR * 0.50f, cy - bodyR * 0.68f, cx + bodyR * 0.26f, cy - bodyR * 0.74f)
        close()
    }, androidx.compose.ui.graphics.Color(0xFFB0BEC5))
    // crest feather on top
    drawPath(androidx.compose.ui.graphics.Path().apply {
        moveTo(cx - bodyR * 0.05f, cy - bodyR * 1.24f)
        quadraticTo(cx - bodyR * 0.1f, cy - bodyR * 1.5f, cx + bodyR * 0.18f, cy - bodyR * 1.56f)
        quadraticTo(cx + bodyR * 0.06f, cy - bodyR * 1.4f, cx + bodyR * 0.12f, cy - bodyR * 1.22f)
        close()
    }, androidx.compose.ui.graphics.Color(0xFFFFCA28))
}

/** F-COM-09: bottom sheet to change avatar + pick a frame. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AvatarEditorSheet(totalPoints: Int, onDismiss: () -> Unit) {
    val avatars = listOf("😀", "😎", "🦊", "🐼", "🦁", "🐧", "🍀", "⭐")
    val selectedAvatar = AvatarFrameStore.avatar ?: avatars.first()
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
                                .clickable { AvatarFrameStore.avatar = emoji },
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
