package com.example.onespace.feature.account.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.example.onespace.feature.account.presentation.*

fun NavGraphBuilder.accountGraph(
    getGraphEntry: () -> androidx.navigation.NavBackStackEntry = { throw IllegalStateException() },
    onOpenSettings: () -> Unit,
    onOpenWrapped: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenPlaylists: () -> Unit = {},
    onDownloads: () -> Unit = {},
    onBadges: () -> Unit = {},
    onOpenVideoPlaylist: (String) -> Unit = {},
    onOpenMusicPlaylist: (String) -> Unit = {},
    onOpenVideo: (String) -> Unit = {},
    onOpenMusic: (String) -> Unit = {},
    onOpenArticle: (String) -> Unit = {},
    downloadedContent: com.example.onespace.domain.session.repository.DownloadedContentRepository? = null
) {
    navigation<AccountRoute.Graph>(startDestination = AccountRoute.Profile) {
        composable<AccountRoute.Profile> {
            val vm: SettingsViewModel = hiltViewModel()
            val avm: AccountViewModel = hiltViewModel()
            val prefs by vm.uiState.collectAsStateWithLifecycle()
            val gamification by avm.gamification.collectAsStateWithLifecycle()
            val feedback by avm.feedback.collectAsStateWithLifecycle()
            val context = androidx.compose.ui.platform.LocalContext.current
            androidx.compose.runtime.LaunchedEffect(feedback) {
                feedback?.let {
                    android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
                    avm.consumeFeedback()
                }
            }
            AccountScreen(
                authEmail = prefs.authEmail,
                gamification = gamification,
                onFeedPet = avm::feed,
                onPurchase = { avm.purchase(it) },
                onSettings = onOpenSettings,
                onExitAccount = vm::exitAccount,
                onWrapped = { onOpenWrapped() },
                onNotifications = { onOpenNotifications() },
                onPlaylists = { onOpenPlaylists() },
                onDownloads = { onDownloads() },
                onBadges = { onBadges() }
            )
        }
        composable<AccountRoute.Wrapped> {
            val vm: SettingsViewModel = hiltViewModel()
            val avm: AccountViewModel = hiltViewModel()
            val prefs by vm.uiState.collectAsStateWithLifecycle()
            val gamification by avm.gamification.collectAsStateWithLifecycle()
            WrappedScreen(prefs.authEmail, gamification)
        }
        composable<AccountRoute.Playlists> {
            PlaylistsScreen(
                onOpenVideoPlaylist = onOpenVideoPlaylist,
                onOpenMusicPlaylist = onOpenMusicPlaylist
            )
        }
        composable<AccountRoute.Downloads> {
            if (downloadedContent != null) {
                val videoItems by downloadedContent.observeByType("video").collectAsStateWithLifecycle(initialValue = emptyList())
                val musicItems by downloadedContent.observeByType("music").collectAsStateWithLifecycle(initialValue = emptyList())
                val newsItems by downloadedContent.observeByType("news").collectAsStateWithLifecycle(initialValue = emptyList())
                DownloadedScreen(
                    videoItems = videoItems,
                    musicItems = musicItems,
                    newsItems = newsItems,
                    onOpenVideo = onOpenVideo,
                    onOpenMusic = onOpenMusic,
                    onOpenArticle = onOpenArticle
                )
            }
        }
        composable<AccountRoute.Badges> { entry ->
            val owner = remember(entry) { getGraphEntry() }
            val vm: AccountViewModel = hiltViewModel(owner)
            val gamification by vm.gamification.collectAsStateWithLifecycle()
            BadgeGridScreen(
                totalPoints = gamification.totalPoints,
                currentStreak = gamification.streak.currentStreak,
                petStage = gamification.pet.stage
            )
        }
        composable<AccountRoute.Notifications> {
            val vm: SettingsViewModel = hiltViewModel()
            val prefs by vm.uiState.collectAsStateWithLifecycle()
            NotificationsScreen(
                notificationsEnabled = prefs.notificationsEnabled,
                streakReminder = prefs.streakReminder,
                quietHours = prefs.quietHours,
                onToggle = vm::setNotificationsEnabled,
                onToggleStreakReminder = vm::setStreakReminder,
                onToggleQuietHours = vm::setQuietHours
            )
        }
        composable<AccountRoute.Settings> {
            val vm: SettingsViewModel = hiltViewModel()
            val prefs by vm.uiState.collectAsStateWithLifecycle()
            SettingsScreen(
                prefs = prefs,
                onThemeMode = vm::setThemeMode,
                onDataSaver = vm::setDataSaver,
                onLanguage = vm::setLanguage,
                onNotifications = vm::setNotificationsEnabled,
                onSimulateOffline = vm::setSimulateOffline,
                onResetConflictRule = vm::resetConflictRule,
                onSetConflictRule = vm::setConflictRule
            )
        }
    }
}
