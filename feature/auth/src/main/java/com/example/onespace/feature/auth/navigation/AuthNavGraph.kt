package com.example.onespace.feature.auth.navigation

import androidx.compose.runtime.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.*
import androidx.navigation.compose.*
import com.example.onespace.feature.auth.presentation.*

/** Login and verification share a graph-scoped ViewModel; screens receive only state/actions. */
fun NavGraphBuilder.authGraph(
    getGraphEntry: () -> NavBackStackEntry,
    onVerifyEmail: (String) -> Unit,
    onBack: () -> Unit
) {
    navigation<AuthRoute.Graph>(startDestination = AuthRoute.Login) {
        composable<AuthRoute.Login> { entry ->
            val owner = remember(entry) { getGraphEntry() }
            val vm: AuthViewModel = hiltViewModel(owner)
            val state by vm.uiState.collectAsStateWithLifecycle()
            val email by vm.email.collectAsStateWithLifecycle()
            LaunchedEffect(state.codeSentTo) {
                state.codeSentTo?.let {
                    vm.consumeNavigation()
                    onVerifyEmail(it)
                }
            }
            LoginScreen(email, state, vm::setEmail, vm::requestCode, vm::continueAsGuest)
        }
        composable<AuthRoute.VerifyCode> { entry ->
            val email = entry.toRoute<AuthRoute.VerifyCode>().email
            val owner = remember(entry) { getGraphEntry() }
            val vm: AuthViewModel = hiltViewModel(owner)
            val state by vm.uiState.collectAsStateWithLifecycle()
            VerifyCodeScreen(email, state,
                onVerify = { vm.verify(email, it) },
                onResend = { vm.resend(email) },
                onBack = onBack)
        }
    }
}
