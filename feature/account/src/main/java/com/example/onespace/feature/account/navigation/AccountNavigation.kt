package com.example.onespace.feature.account.navigation

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.onespace.feature.account.presentation.*

object AccountNavigation {
    const val ACCOUNT = "account"
    const val SETTINGS = "settings"
}

fun NavGraphBuilder.accountGraph(nav: NavHostController) {
    composable(AccountNavigation.ACCOUNT) {
        AccountScreen { nav.navigate(AccountNavigation.SETTINGS) }
    }
    composable(AccountNavigation.SETTINGS) {
        var dataSaver by rememberSaveable { mutableStateOf(false) }
        SettingsScreen(dataSaver) { dataSaver = it }
    }
}
