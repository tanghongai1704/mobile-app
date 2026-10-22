package com.example.onespace.feature.account.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.onespace.feature.account.presentation.*

object AccountNavigation {
    const val ACCOUNT = "account"
    const val SETTINGS = "settings"
}

fun NavGraphBuilder.accountGraph(nav: NavHostController, onExitAccount: () -> Unit) {
    composable(AccountNavigation.ACCOUNT) {
        val vm: SettingsViewModel = hiltViewModel()
        val prefs by vm.uiState.collectAsStateWithLifecycle()
        AccountScreen(
            authEmail = prefs.authEmail,
            onSettings = { nav.navigate(AccountNavigation.SETTINGS) },
            onExitAccount = {
                if (prefs.authEmail == null) onExitAccount()
                else vm.exitAccount(onExitAccount)
            }
        )
    }
    composable(AccountNavigation.SETTINGS) {
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
