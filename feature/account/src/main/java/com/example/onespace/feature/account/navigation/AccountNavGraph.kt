package com.example.onespace.feature.account.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.example.onespace.feature.account.presentation.*

fun NavGraphBuilder.accountGraph(onOpenSettings: () -> Unit) {
    navigation<AccountRoute.Graph>(startDestination = AccountRoute.Profile) {
        composable<AccountRoute.Profile> {
            val vm: SettingsViewModel = hiltViewModel()
            val prefs by vm.uiState.collectAsStateWithLifecycle()
            AccountScreen(
                authEmail = prefs.authEmail,
                onSettings = onOpenSettings,
                onExitAccount = vm::exitAccount
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
                onResetConflictRule = vm::resetConflictRule
            )
        }
    }
}
