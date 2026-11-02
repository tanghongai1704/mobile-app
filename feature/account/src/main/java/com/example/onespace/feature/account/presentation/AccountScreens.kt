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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.SportsEsports
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
    val shop: ShopState = ShopState(),
    val gameBadges: List<com.example.onespace.domain.games.badge.BadgeStatus> = emptyList()
)

/** Phone accounts travel under an in-app address; show the number instead. */
@Composable
internal fun displayIdentity(identity: String?): String =
    if (identity != null && identity.endsWith("@phone.onespace"))
        "0" + identity.removeSuffix("@phone.onespace")
    else identity ?: tr("Khách", "Guest")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountScreen(
    authEmail: String?,
    avatarEmoji: String? = null,
    avatarFrameId: String = "basic",
    onAvatarChange: (String) -> Unit = {},
    onAvatarFrameChange: (String) -> Unit = {},
    gamification: AccountGamificationState = AccountGamificationState(),
    continueWatching: List<com.example.onespace.domain.video.repository.ContinueEntry> = emptyList(),
    onFeedPet: () -> Unit = {},
    onPurchase: (ShopItem) -> Unit = {},
    onSettings: () -> Unit,
    onExitAccount: () -> Unit,
    onWrapped: () -> Unit = {},
    onNotifications: () -> Unit = {},
    onPlaylists: () -> Unit = {},
    onDownloads: () -> Unit = {},
    onBadges: () -> Unit = {},
    onOpenVideo: (id: String, title: String) -> Unit = { _, _ -> }
) {
    var showLogout by remember { mutableStateOf(false) }
    var showShop by remember { mutableStateOf(false) }
    var showAvatarEditor by remember { mutableStateOf(false) }

    FeaturePage {
        item {
            // F-COM-03: avatar with initial; F-COM-09: frame ring by milestone.
            val frame = AvatarFrameStore.frameOf(avatarFrameId)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // F-COM-09: Edit button on avatar opens frame + avatar picker.
                Box {
                    Box(
                        Modifier.size(72.dp).background(
                            frame.color.copy(alpha = 0.25f), CircleShape
                        ).border(
                            BorderStroke(3.dp, frame.color),
                            CircleShape
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        val avatarGlyph = avatarEmoji
                            ?: (authEmail?.take(1) ?: "K").uppercase()
                        Text(
                            avatarGlyph,
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (avatarEmoji == null)
                                frame.color
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
                            Icon(Icons.Filled.Edit, contentDescription = tr("Đổi avatar", "Change avatar"),
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
                        if (authEmail == null) tr("Chế độ dùng thử", "Trial mode")
                        else tr("Tài khoản OneSpace · khung ", "OneSpace account · frame ") + frame.name.t(),
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
            ).count { it } + gamification.gameBadges.count { it.earned }
            val badgeTotal = 6 + gamification.gameBadges.size
            ContentCard(
                tr("Huy hiệu", "Badges"),
                earnedCount.toString() + "/" + badgeTotal + tr(" huy hiệu đã đạt", " badges earned"),
                Icons.Outlined.Star,
                onBadges
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(tr("Điểm", "Points"), "${gamification.totalPoints}", Icons.Outlined.Star, Modifier.weight(1f))
                StatCard(tr("Chuỗi ngày", "Day streak"), tr("${gamification.streak.currentStreak} ngày", "${gamification.streak.currentStreak} ${if (gamification.streak.currentStreak == 1) "day" else "days"}"),
                    Icons.Outlined.LocalFireDepartment, Modifier.weight(1f))
                StatCard(tr("Thú cưng", "Pet"), gamification.pet.stage.displayName,
                    Icons.Outlined.Egg, Modifier.weight(1f))
            }
        }
        // Pet detail
        item { PetCard(gamification.pet, onFeedPet) }
        // Shop entry
        item {
            ContentCard(
                tr("Cửa hàng đổi quà", "Rewards shop"),
                tr("Mở game, đồ pet, khung avatar", "Unlock games, pet items, avatar frames"),
                Icons.Filled.ShoppingBag
            ) { showShop = true }
        }
        // "Continue" is gone: a video closed mid-way lands in Watch later on Home.
        item { ContentCard(tr("Đã tải", "Downloaded"), tr("Video, nhạc và bài báo đã lưu để xem offline", "Videos, music and articles saved for offline"), Icons.Filled.Download, onDownloads) }
        item { ContentCard(tr("Playlist của tôi", "My playlists"), tr("Mọi playlist video và nhạc, quản lý một chỗ", "All video and music playlists in one place"), Icons.Filled.LibraryMusic, onPlaylists) }
        item { ContentCard(tr("Tổng kết tháng", "Monthly recap"), tr("Điểm, chuỗi ngày và huy hiệu của bạn", "Your points, streak and badges"), Icons.Filled.Insights, onWrapped) }
        item { ContentCard(tr("Nhắc nhở", "Reminders"), tr("Nhiệm vụ hằng ngày và giữ chuỗi ngày", "Daily missions and keeping your streak"), Icons.Filled.Notifications, onNotifications) }
        item { ContentCard("Tùy chỉnh", tr("Giao diện, dữ liệu, âm thanh", "Appearance, data, audio"), Icons.Filled.Settings, onSettings) }
        item {
            when {
                authEmail == null -> Button(onClick = onExitAccount, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Đăng nhập để giữ tiến độ", "Sign in to keep your progress"))
                }
                else -> Button(onClick = { showLogout = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Đăng xuất", "Sign out"))
                }
            }
        }
    }

    if (showAvatarEditor) {
        AvatarEditorSheet(
            totalPoints = gamification.totalPoints,
            selectedAvatar = avatarEmoji,
            selectedFrameId = avatarFrameId,
            onPickAvatar = onAvatarChange,
            onPickFrame = onAvatarFrameChange,
            onDismiss = { showAvatarEditor = false }
        )
    }
    if (showLogout) {
        ModalBottomSheet(onDismissRequest = { showLogout = false }) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(tr("Bạn có muốn đăng xuất?", "Do you want to sign out?"), style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold)
                Text(tr("Nội dung đã lưu vẫn giữ nguyên trên máy này.", "Saved content stays on this device."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { showLogout = false }, modifier = Modifier.weight(1f)) {
                        Text(tr("Hủy", "Cancel"))
                    }
                    Button(onClick = { showLogout = false; onExitAccount() }, modifier = Modifier.weight(1f)) {
                        Text(tr("Đăng xuất", "Sign out"))
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
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
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
                        PetStage.EGG -> tr("Hoàn thành nhiệm vụ để trứng nở", "Complete missions to hatch the egg")
                        PetStage.BABY -> tr("Cho ăn để lớn lên", "Feed it to help it grow")
                        PetStage.TEEN -> tr("Sắp trưởng thành!", "Almost grown up!")
                        PetStage.ADULT -> tr("Thú cưng đã trưởng thành", "Your pet is all grown up")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                if (pet.treats > 0) {
                    Text(
                        tr("Thức ăn thưởng thử thách: ${pet.treats}", "Challenge treats: ${pet.treats}"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                Button(onClick = onFeed, enabled = pet.canFeed) {
                    Text(
                        when {
                            pet.treats > 0 -> tr("Cho ăn miễn phí", "Feed for free")
                            pet.canFeed -> tr("Cho ăn (−30 điểm)", "Feed (−30 points)")
                            else -> tr("Đã cho ăn hôm nay", "Already fed today")
                        }
                    )
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
            Text(tr("Cửa hàng đổi quà", "Rewards shop"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                tr("Số dư: $totalPoints điểm", "Balance: $totalPoints points"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // F-COM-15: each item reads as a mini poster: themed gradient
            // tile, emoji illustration, then name and price.
            state.items.forEach { item ->
                val owned = state.isOwned(item)
                val tint = shopTint(item.id)
                AppCard(Modifier.fillMaxWidth()) {
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
                                MaterialTheme.shapes.medium
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(item.emoji, style = MaterialTheme.typography.headlineMedium)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(item.name.t(), style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold)
                            Text(item.description.t(), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        when {
                            owned -> Surface(
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    tr("Đã có", "Owned"),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                            else -> OutlinedButton(onClick = { onPurchase(item) }) {
                                Text(tr("${item.price} điểm", "${item.price} points"))
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
    AppCard(modifier) {
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
    onSetConflictRule: (com.example.onespace.domain.session.MediaConflictRule) -> Unit = {},
    onReplayOnboarding: () -> Unit = {}
) {
    FeaturePage {
        item {
            ListItem(
                headlineContent = { Text(tr("Giao diện", "Appearance")) },
                supportingContent = { Text(tr("Áp dụng cho toàn bộ ứng dụng", "Applies to the whole app")) }
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                SegmentedButton(
                    selected = prefs.themeMode == ThemeMode.SYSTEM,
                    onClick = { onThemeMode(ThemeMode.SYSTEM) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                    label = { Text(tr("Hệ thống", "System")) }
                )
                SegmentedButton(
                    selected = prefs.themeMode == ThemeMode.LIGHT,
                    onClick = { onThemeMode(ThemeMode.LIGHT) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                    label = { Text(tr("Sáng", "Light")) }
                )
                SegmentedButton(
                    selected = prefs.themeMode == ThemeMode.DARK,
                    onClick = { onThemeMode(ThemeMode.DARK) },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                    label = { Text(tr("Tối", "Dark")) }
                )
            }
        }
        item {
            ListItem(
                headlineContent = { Text(tr("Ngôn ngữ", "Language")) },
                supportingContent = { Text(tr("Đổi ngay, không cần khởi động lại", "Applies immediately, no restart needed")) }
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
                headlineContent = { Text(tr("Tiết kiệm dữ liệu", "Data saver")) },
                supportingContent = { Text(tr("Giới hạn video ở mức 720p khi bật", "Limits video to 720p when on")) },
                trailingContent = { Switch(prefs.dataSaver, onDataSaver) }
            )
        }
        item {
            ListItem(
                modifier = Modifier.toggleable(
                    value = prefs.notificationsEnabled, role = Role.Switch, onValueChange = onNotifications),
                headlineContent = { Text(tr("Nhắc nhiệm vụ", "Mission reminders")) },
                supportingContent = { Text(tr("Quyền thông báo chỉ được xin khi bạn bật nhắc lần đầu", "Notification permission is only requested the first time you turn reminders on")) },
                trailingContent = { Switch(prefs.notificationsEnabled, onNotifications) }
            )
        }
        item {
            ListItem(
                modifier = Modifier.toggleable(
                    value = prefs.simulateOffline, role = Role.Switch, onValueChange = onSimulateOffline),
                headlineContent = { Text(tr("Mô phỏng mất mạng (demo)", "Simulate going offline (demo)")) },
                supportingContent = { Text(tr("Bật để xem các trạng thái offline/retry của feed", "Turn on to see the feed offline and retry states")) },
                trailingContent = { Switch(prefs.simulateOffline, onSimulateOffline) }
            )
        }
        item {
            // F-COM-13/F-COM-14: the row itself is the picker — the tick
            // list always shows the current rule, "Luôn hỏi" included.
            var showConflictPicker by remember { mutableStateOf(false) }
            ListItem(
                headlineContent = { Text(tr("Xung đột âm thanh", "Audio conflicts")) },
                supportingContent = { Text(labelOf(prefs.conflictRule)) },
                trailingContent = {
                    TextButton(onClick = { showConflictPicker = true }) {
                        Text(if (prefs.conflictRule == null) tr("Luôn hỏi", "Always ask") else tr("Mặc định", "Default"))
                    }
                },
                modifier = Modifier.clickable { showConflictPicker = true }
            )
            if (showConflictPicker) {
                AlertDialog(
                    onDismissRequest = { showConflictPicker = false },
                    title = { Text(tr("Xung đột âm thanh", "Audio conflicts")) },
                    text = {
                        Column {
                            conflictOption(null, tr("Luôn hỏi", "Always ask"), prefs.conflictRule) {
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
        item {
            // Replay the first-launch flow: the persisted flag flips and the
            // root navigator returns to the onboarding graph.
            var showReplayConfirm by remember { mutableStateOf(false) }
            ListItem(
                headlineContent = { Text(tr("Làm lại hướng dẫn", "Replay the intro")) },
                supportingContent = { Text(tr("Xem lại phần giới thiệu và chọn lại chủ đề yêu thích", "Watch the intro again and pick your favourite topics again")) },
                modifier = Modifier.clickable { showReplayConfirm = true }
            )
            if (showReplayConfirm) {
                AlertDialog(
                    onDismissRequest = { showReplayConfirm = false },
                    title = { Text(tr("Làm lại hướng dẫn?", "Replay the intro?")) },
                    text = { Text(tr("Điểm, nhiệm vụ và dữ liệu đã lưu không đổi; bạn sẽ chọn lại chủ đề ở cuối phần hướng dẫn.", "Your points, missions and saved data stay as they are; you will pick topics again at the end of the intro.")) },
                    confirmButton = {
                        TextButton(onClick = { onReplayOnboarding(); showReplayConfirm = false }) {
                            Text(tr("Bắt đầu", "Start"))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showReplayConfirm = false }) { Text(tr("Để sau", "Not now")) }
                    }
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
            .clip(MaterialTheme.shapes.small)
            .clickable { onPick(rule) }
            .padding(vertical = 10.dp)
    ) {
        RadioButton(selected = rule == current, onClick = { onPick(rule) })
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun labelOf(rule: com.example.onespace.domain.session.MediaConflictRule?): String = when (rule) {
    com.example.onespace.domain.session.MediaConflictRule.MUTE_VIDEO -> tr("Ưu tiên nhạc — tắt tiếng video", "Music first, mute the video")
    com.example.onespace.domain.session.MediaConflictRule.PAUSE_VIDEO -> tr("Ưu tiên nhạc — dừng video", "Music first, pause the video")
    com.example.onespace.domain.session.MediaConflictRule.KEEP_VIDEO -> tr("Ưu tiên video", "Video first")
    null -> tr("Luôn hỏi mỗi khi hai luồng cùng phát", "Ask every time both play at once")
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
            if (earned) tr("Đạt được", "Earned") else tr("Chưa mở", "Locked"),
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
                    Text(tr("OneSpace của bạn tháng này", "Your OneSpace this month"), style = MaterialTheme.typography.headlineSmall,
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
                            Text(tr("điểm", "points"), style = MaterialTheme.typography.labelMedium)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${gamification.streak.currentStreak}", style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(tr("chuỗi ngày", "day streak"), style = MaterialTheme.typography.labelMedium)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(gamification.pet.stage.displayName.t(), style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(tr("thú cưng", "pet"), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
        // F-COM-10: per-section breakdown — what the month looked like
        // in each entertainment area, with the share of total activity.
        item { SectionTitle(tr("Tháng này của bạn ở từng mảng", "Your month, area by area")) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                WrappedSection(
                    emoji = "🎬", name = "Video", tint = MaterialTheme.oneSpaceColors.accents.video,
                    statLine = tr("12 video · 3,4 giờ xem", "12 videos · 3.4 hours watched"),
                    favLine = tr("Kênh hay nhất: Hành Trình 500 Blues", "Top channel: Hành Trình 500 Blues"),
                    share = 0.42f
                )
                WrappedSection(
                    emoji = "🎵", name = "Nhạc", tint = MaterialTheme.oneSpaceColors.accents.music,
                    statLine = tr("45 bài đã nghe · 3,2 giờ", "45 songs played · 3.2 hours"),
                    favLine = tr("Nghệ sĩ hay nhất: Mưa Tháng Sáu", "Top artist: Mưa Tháng Sáu"),
                    share = 0.31f
                )
                WrappedSection(
                    emoji = "📰", name = "Tin tức", tint = MaterialTheme.oneSpaceColors.accents.news,
                    statLine = tr("28 bài đọc · 96 phút", "28 articles read · 96 minutes"),
                    favLine = tr("Chuyên mục hay nhất: Công nghệ", "Top section: Công nghệ"),
                    share = 0.17f
                )
                WrappedSection(
                    emoji = "🎮", name = "Games", tint = MaterialTheme.oneSpaceColors.accents.games,
                    statLine = tr("15 ván thắng · 4 huy hiệu", "15 wins · 4 badges"),
                    favLine = tr("Chơi nhiều nhất: Đoán bài hát", "Most played: Guess the Song"),
                    share = 0.10f
                )
            }
        }
        // F-COM-10: the month's little milestones.
        item {
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(tr("Khoảnh khắc đáng nhớ", "Highlights"), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    WrappedMoment(Icons.Outlined.LocalFireDepartment,
                        tr("Giữ chuỗi ${gamification.streak.currentStreak} ngày liền", "Kept a ${gamification.streak.currentStreak}-day streak"))
                    WrappedMoment(Icons.Filled.MusicNote, tr("Trích xuất bài MP3 đầu tiên từ video", "Extracted your first MP3 from a video"))
                    WrappedMoment(Icons.Outlined.Star, tr("Lần đầu thắng game Đoán bài hát", "First win in Guess the Song"))
                }
            }
        }
        item {
            val shareText = tr("Tháng này tôi đạt ", "This month I earned ") + gamification.totalPoints + tr(" điểm, giữ chuỗi ", " points, kept a ") +
                gamification.streak.currentStreak + tr(" ngày và đủ cả xem - nghe - đọc - chơi trên OneSpace!", "-day streak and watched, listened, read and played on OneSpace!")
            val shareTitle = tr("Chia sẻ tổng kết", "Share recap")
            Button(
                onClick = {
                    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(android.content.Intent.createChooser(send, shareTitle))
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(tr("Chia sẻ thẻ tổng kết", "Share recap card")) }
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
                    Text(tr("Nhắc nhiệm vụ hằng ngày", "Daily mission reminder"), style = MaterialTheme.typography.bodyLarge)
                    Text(tr("9:00 · nhắc nhiệm vụ hôm nay chưa làm", "9:00 · reminds you of today's missions"),
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
                    Text(tr("Nhắc giữ chuỗi ngày", "Streak reminder"), style = MaterialTheme.typography.bodyLarge)
                    Text(tr("20:00 · chỉ nhắc khi chưa hoạt động hôm nay", "20:00 · only if you haven't been active today"),
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
                    Text(tr("Giờ yên tĩnh", "Quiet hours"), style = MaterialTheme.typography.bodyLarge)
                    Text(tr("22:00–7:00 · không hiển thị nhắc nhở", "22:00–7:00 · no reminders shown"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = quietHours, onCheckedChange = onToggleQuietHours)
            }
        }
        item { PrototypeNote(tr("Bản demo hiển thị nhắc trong ứng dụng; thông báo đẩy nằm trong bản phát hành.", "The demo shows reminders inside the app; push notifications ship with the release.")) }
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
                EmptyHint(
                    Icons.Filled.LibraryMusic,
                    tr("Chưa có playlist nào. Mở một video hoặc bài hát và chọn \"Playlist\" để tạo.", "No playlists yet. Open a video or song and choose \"Playlist\" to create one.")
                )
            }
        }
        items(playlists.values.toList(), key = { it.name }) { pl ->
            val subtitle = buildList {
                if (pl.videoIds.isNotEmpty()) add(pl.videoIds.size.toString() + " video")
                if (pl.trackIds.isNotEmpty()) add(pl.trackIds.size.toString() + tr(" bài hát", " songs"))
            }.joinToString(" · ")
            ContentCard(
                title = pl.name,
                subtitle = if (subtitle.isBlank()) tr("Trống", "Empty") else subtitle,
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
                    label = { Text(tr("Báo (", "Articles (") + newsItems.size + ")") })
            }
        }
        if (items.isEmpty()) {
            item {
                EmptyHint(
                    when (tab) {
                        "music" -> Icons.Filled.MusicNote
                        "news" -> Icons.Filled.Newspaper
                        else -> Icons.Filled.PlayCircle
                    },
                    when (tab) {
                        "music" -> tr("Chưa tải bài hát nào. Bấm nút tải trên một bài trong tab Nhạc.", "No songs downloaded yet. Tap the download button on a song in the Music tab.")
                        "news" -> tr("Chưa tải bài báo nào.", "No articles downloaded yet.")
                        else -> tr("Chưa tải video nào. Bấm nút tải trên một video để thêm vào đây.", "No videos downloaded yet. Tap the download button on a video to add it here.")
                    }
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
internal fun BadgeGridScreen(
    totalPoints: Int,
    currentStreak: Int,
    petStage: PetStage,
    gameBadges: List<com.example.onespace.domain.games.badge.BadgeStatus> = emptyList()
) {
    val badges = listOf(
        Triple(tr("Người mới", "Newcomer"), Icons.Outlined.Star, true),
        Triple(tr("50 điểm", "50 points"), Icons.Outlined.Star, totalPoints >= 50),
        Triple(tr("100 điểm", "100 points"), Icons.Outlined.Star, totalPoints >= 100),
        Triple(tr("Chuỗi 3 ngày", "3-day streak"), Icons.Outlined.LocalFireDepartment, currentStreak >= 3),
        Triple(tr("Chuỗi 7 ngày", "7-day streak"), Icons.Outlined.LocalFireDepartment, currentStreak >= 7),
        Triple(tr("Thú cưng trưởng thành", "Grown-up pet"), Icons.Outlined.Egg, petStage == PetStage.ADULT)
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
                        if (earned) tr("Đã đạt", "Earned") else tr("Chưa mở", "Locked"),
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
        if (gameBadges.isNotEmpty()) {
            item { SectionTitle(tr("Huy hiệu game", "Game badges")) }
            items(gameBadges.size) { idx -> GameBadgeRow(gameBadges[idx]) }
        }
    }
}

/** A game badge with its goal and how far along the player is. */
@Composable
internal fun GameBadgeRow(status: com.example.onespace.domain.games.badge.BadgeStatus) {
    val earned = status.earned
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Icon(
            gameBadgeIcon(status.badge.id), contentDescription = null,
            tint = if (earned) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(32.dp)
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(status.badge.name.t(), style = MaterialTheme.typography.bodyLarge)
            Text(
                status.badge.description.t(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!earned) {
                LinearProgressIndicator(
                    progress = { status.fraction },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                )
                Text(
                    "${status.progress}/${status.badge.goal}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (earned) {
            Icon(Icons.Filled.CheckCircle, contentDescription = tr("Đã đạt", "Earned"), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun gameBadgeIcon(id: String): androidx.compose.ui.graphics.vector.ImageVector = when {
    id.startsWith("snake") -> Icons.Filled.Pets
    id.startsWith("tile") -> Icons.Filled.GridView
    id.startsWith("quiz") -> Icons.Filled.Quiz
    id.startsWith("ttt") -> Icons.Filled.Apps
    id.startsWith("beat") -> Icons.Filled.MusicNote
    id.startsWith("guess") -> Icons.Filled.Headphones
    id.startsWith("challenge") -> Icons.Filled.EmojiEvents
    else -> Icons.Filled.SportsEsports
}

/** One entertainment area of the wrapped card. */
@Composable
private fun WrappedSection(
    emoji: String, name: String, tint: androidx.compose.ui.graphics.Color, statLine: String, favLine: String, share: Float
) {
    val tintColor = tint
    AppCard(Modifier.fillMaxWidth()) {
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
@Composable
private fun shopTint(id: String): androidx.compose.ui.graphics.Color = with(MaterialTheme.oneSpaceColors) {
    when {
        id.startsWith("game_") -> accents.games
        id.startsWith("pet_") -> accents.pet
        id.endsWith("gold") -> frames.gold
        id.endsWith("silver") -> frames.silver
        else -> frames.bronze
    }
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
    val pet = MaterialTheme.oneSpaceColors.pet
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
            pet.groundShadow,
            radius = r * 0.46f,
            center = Offset(size.width / 2f, size.height * 0.90f)
        )
        when (stage) {
            PetStage.EGG -> drawEgg(r, pet)
            PetStage.BABY -> drawChick(r, pet, scale = 0.66f, eyeOpen = eyeOpen, teen = false)
            PetStage.TEEN -> drawChick(r, pet, scale = 0.82f, eyeOpen = eyeOpen, teen = true)
            PetStage.ADULT -> drawParrot(r, pet, eyeOpen)
        }
    }
}

/** Egg with soft spots; the idle tilt makes it look like something stirs inside. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEgg(r: Float, pet: PetPaletteColors) {
    val cx = size.width / 2f
    val cy = size.height * 0.56f
    drawOval(
        color = pet.eggShell,
        topLeft = Offset(cx - r * 0.58f, cy - r * 0.80f),
        size = androidx.compose.ui.geometry.Size(r * 1.16f, r * 1.6f)
    )
    drawCircle(pet.eggSpot, r * 0.09f,
        Offset(cx - r * 0.22f, cy - r * 0.34f))
    drawCircle(pet.eggSpot, r * 0.06f,
        Offset(cx + r * 0.26f, cy + r * 0.12f))
    drawCircle(pet.eggSpot, r * 0.05f,
        Offset(cx + r * 0.05f, cy - r * 0.6f))
}

/** Round chick for BABY/TEEN: tuft, cheeks appear when older. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawChick(
    r: Float, pet: PetPaletteColors, scale: Float, eyeOpen: Float, teen: Boolean
) {
    val cx = size.width / 2f
    val cy = size.height * 0.56f
    val bodyR = r * 0.52f * scale
    val body = pet.chickBody
    val belly = pet.chickBelly
    val foot = pet.chickFoot
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
        drawCircle(pet.cheek, bodyR * 0.13f,
            Offset(cx - bodyR * 0.52f, cy - bodyR * 0.12f))
        drawCircle(pet.cheek, bodyR * 0.13f,
            Offset(cx + bodyR * 0.52f, cy - bodyR * 0.12f))
    }
    // eyes with blink
    val eyeY = cy - bodyR * 0.22f
    listOf(-1, 1).forEach { side ->
        val ex = cx + side * bodyR * 0.34f
        drawCircle(pet.eyeWhite, bodyR * 0.155f, Offset(ex, eyeY))
        drawCircle(pet.eyePupil,
            bodyR * 0.10f * eyeOpen.coerceIn(0.14f, 1f), Offset(ex, eyeY + bodyR * 0.01f))
        drawCircle(pet.eyeWhite, bodyR * 0.032f, Offset(ex - bodyR * 0.02f, eyeY - bodyR * 0.028f))
    }
    // beak
    drawPath(androidx.compose.ui.graphics.Path().apply {
        moveTo(cx - bodyR * 0.14f, cy - bodyR * 0.04f)
        lineTo(cx + bodyR * 0.14f, cy - bodyR * 0.04f)
        lineTo(cx, cy + bodyR * 0.14f)
        close()
    }, pet.chickBeak)
}

/** ADULT parrot: red head, green body, hooked beak, blue tail feathers. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawParrot(r: Float, pet: PetPaletteColors, eyeOpen: Float) {
    val cx = size.width / 2f
    val cy = size.height * 0.54f
    val bodyR = r * 0.5f
    val green = pet.parrotBody
    val darkGreen = pet.parrotWing
    val red = pet.parrotHead
    val tailBlue = pet.parrotTailBlue
    // tail feathers behind the body
    drawLine(tailBlue, Offset(cx - bodyR * 0.1f, cy + bodyR * 0.55f),
        Offset(cx - bodyR * 0.85f, cy + bodyR * 1.5f), strokeWidth = r * 0.075f)
    drawLine(red, Offset(cx + bodyR * 0.02f, cy + bodyR * 0.55f),
        Offset(cx - bodyR * 0.30f, cy + bodyR * 1.62f), strokeWidth = r * 0.075f)
    drawLine(pet.parrotTailYellow, Offset(cx + bodyR * 0.12f, cy + bodyR * 0.55f),
        Offset(cx + bodyR * 0.42f, cy + bodyR * 1.45f), strokeWidth = r * 0.075f)
    // feet
    val foot = pet.parrotFoot
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
    drawCircle(pet.parrotFace, bodyR * 0.40f, Offset(cx + bodyR * 0.06f, cy - bodyR * 0.70f))
    val ex = cx + bodyR * 0.10f
    val ey = cy - bodyR * 0.74f
    drawCircle(pet.eyePupil,
        bodyR * 0.085f * eyeOpen.coerceIn(0.14f, 1f), Offset(ex, ey))
    drawCircle(pet.eyeWhite, bodyR * 0.028f, Offset(ex - bodyR * 0.02f, ey - bodyR * 0.025f))
    // hooked beak pointing right
    drawPath(androidx.compose.ui.graphics.Path().apply {
        moveTo(cx + bodyR * 0.28f, cy - bodyR * 0.86f)
        quadraticTo(cx + bodyR * 0.92f, cy - bodyR * 0.80f, cx + bodyR * 0.62f, cy - bodyR * 0.40f)
        quadraticTo(cx + bodyR * 0.50f, cy - bodyR * 0.68f, cx + bodyR * 0.26f, cy - bodyR * 0.74f)
        close()
    }, pet.parrotBeak)
    // crest feather on top
    drawPath(androidx.compose.ui.graphics.Path().apply {
        moveTo(cx - bodyR * 0.05f, cy - bodyR * 1.24f)
        quadraticTo(cx - bodyR * 0.1f, cy - bodyR * 1.5f, cx + bodyR * 0.18f, cy - bodyR * 1.56f)
        quadraticTo(cx + bodyR * 0.06f, cy - bodyR * 1.4f, cx + bodyR * 0.12f, cy - bodyR * 1.22f)
        close()
    }, pet.parrotCrest)
}

/** F-COM-09: bottom sheet to change avatar + pick a frame. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AvatarEditorSheet(
    totalPoints: Int,
    selectedAvatar: String?,
    selectedFrameId: String,
    onPickAvatar: (String) -> Unit,
    onPickFrame: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val avatars = listOf("😀", "😎", "🦊", "🐼", "🦁", "🐧", "🍀", "⭐")
    val currentAvatar = selectedAvatar ?: avatars.first()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle(tr("Hình đại diện", "Avatar"))
            avatars.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { emoji ->
                        val selected = currentAvatar == emoji
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                )
                                .clickable { onPickAvatar(emoji) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
            }
            SectionTitle(tr("Khung avatar", "Avatar frame"))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AvatarFrameStore.frames.forEach { f ->
                    val unlocked = totalPoints >= f.unlockPoints
                    Box(
                        Modifier.size(44.dp).background(
                            if (unlocked) f.color.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        ).border(
                            BorderStroke(
                                if (selectedFrameId == f.id) 3.dp else 1.dp,
                                if (unlocked) f.color
                                else MaterialTheme.colorScheme.outlineVariant
                            ),
                            CircleShape
                        ).clickable(enabled = unlocked) { onPickFrame(f.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (unlocked) Text(f.name.t().take(1), color = f.color,
                            fontWeight = FontWeight.Bold)
                        else Text("🔒", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(tr("Xong", "Done")) }
        }
    }
}
