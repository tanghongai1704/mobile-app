package com.example.onespace.feature.account.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * SEC-04: the notification permission is asked in context, the first time the user turns
 * reminders on (Android 13+). If it is refused the switch stays off and the rest of the app works.
 */
@Composable
internal fun rememberNotificationToggle(onToggle: (Boolean) -> Unit): (Boolean) -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onToggle(granted)
    }
    return { enable ->
        val needsPermission = enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else onToggle(enable)
    }
}
