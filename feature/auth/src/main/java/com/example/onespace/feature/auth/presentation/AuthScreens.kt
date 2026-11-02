package com.example.onespace.feature.auth.presentation

import com.example.onespace.core.designsystem.t
import com.example.onespace.core.designsystem.tr
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.onespace.core.designsystem.AppCard
import com.example.onespace.core.designsystem.OneSpaceBrand

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LoginScreen(
    email: String,
    phone: String = "",
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit = {},
    onRequestCode: () -> Unit = {},
    onRequestPhoneCode: () -> Unit = {},
    onPhonePasswordSignIn: (String) -> Unit = {},
    onEmailPasswordSignIn: (String) -> Unit = {},
    onGuest: () -> Unit
) {
    var showGuest by rememberSaveable { mutableStateOf(false) }
    var password by rememberSaveable { mutableStateOf("") }
    var showGoogle by rememberSaveable { mutableStateOf(false) }
    // F-COM-01: phone-first login, email still available on the second tab.
    var phoneMode by rememberSaveable { mutableStateOf(true) }
    // Password is the primary path; OTP stays as the alternative.
    var otpMode by rememberSaveable { mutableStateOf(false) }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        OneSpaceBrand(compact = true)
        Text(tr("Không gian của bạn.", "Your space."), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text(tr("Đăng nhập / Đăng ký", "Sign in / Sign up"), style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(tr("Một tài khoản. Bốn cách tận hưởng mỗi ngày.", "One account. Four ways to enjoy every day."),
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        AppCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = phoneMode,
                        onClick = { phoneMode = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        label = { Text(tr("Số điện thoại", "Phone number")) }
                    )
                    SegmentedButton(
                        selected = !phoneMode,
                        onClick = { phoneMode = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        label = { Text("Email") }
                    )
                }
                if (phoneMode) {
                    OutlinedTextField(
                        shape = MaterialTheme.shapes.medium, colors = com.example.onespace.core.designsystem.AppFieldDefaults.colors(),
                        value = phone, onValueChange = onPhoneChange, enabled = !state.loading,
                        label = { Text(tr("Số điện thoại", "Phone number")) }, singleLine = true,
                        prefix = { Text("+84 ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = state.error != null,
                        supportingText = { state.error?.let { Text(it.t()) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        shape = MaterialTheme.shapes.medium, colors = com.example.onespace.core.designsystem.AppFieldDefaults.colors(),
                        value = email, onValueChange = onEmailChange, enabled = !state.loading,
                        label = { Text("Email") }, singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Email, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = state.error != null,
                        supportingText = { state.error?.let { Text(it.t()) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (otpMode) {
                    Button(
                        onClick = if (phoneMode) onRequestPhoneCode else onRequestCode,
                        enabled = !state.loading,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    ) {
                        if (state.loading) LoadingIndicator() else Text(tr("Gửi mã OTP", "Send OTP code"))
                    }
                } else {
                    OutlinedTextField(
                        shape = MaterialTheme.shapes.medium, colors = com.example.onespace.core.designsystem.AppFieldDefaults.colors(),
                        value = password, onValueChange = { password = it },
                        enabled = !state.loading,
                        label = { Text(tr("Mật khẩu", "Password")) }, singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (showPassword) tr("Ẩn mật khẩu", "Hide password") else tr("Hiện mật khẩu", "Show password")
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = state.error != null,
                        supportingText = { state.error?.let { Text(it.t()) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (phoneMode) onPhonePasswordSignIn(password)
                            else onEmailPasswordSignIn(password)
                        },
                        enabled = !state.loading && (if (phoneMode) phone.length in 9..11 else email.isNotBlank())
                            && password.length >= 6,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    ) {
                        if (state.loading) LoadingIndicator() else Text(tr("Đăng nhập", "Sign in"))
                    }
                }
                TextButton(
                    onClick = { otpMode = !otpMode },
                    enabled = !state.loading,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(if (otpMode) tr("Dùng mật khẩu", "Use password") else tr("Dùng mã OTP", "Use OTP code"))
                }
                // F-COM-01: Google is offered, the demo build only explains it.
                OutlinedButton(onClick = { showGoogle = true }, enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text(tr("Tiếp tục với Google", "Continue with Google"))
                }
            }
        }
        TextButton(onClick = { showGuest = true }, enabled = !state.loading) {
            Text(tr("Dùng thử không cần tài khoản", "Try without an account"))
        }
        Spacer(Modifier.height(12.dp))
    }

    if (showGoogle) ModalBottomSheet(onDismissRequest = { showGoogle = false }) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(tr("Đăng nhập bằng Google", "Sign in with Google"), style = MaterialTheme.typography.headlineSmall)
            Text(tr("Bản demo của môn học chưa kết nối Google Sign-In. Trong bản phát hành, bước này mở cửa sổ chọn tài khoản Google của bạn.", "The course demo is not connected to Google Sign-In yet. In the release, this step opens your Google account picker."),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { showGoogle = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(tr("Đã hiểu", "Got it"))
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showGuest) ModalBottomSheet(onDismissRequest = { showGuest = false }) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(tr("Dùng thử không cần tài khoản", "Try without an account"), style = MaterialTheme.typography.headlineSmall)
            Text(tr("Khám phá video, âm nhạc, tin tức và trò chơi. Phiên dùng thử được lưu trên thiết bị này.", "Explore videos, music, news and games. The trial session is saved on this device."),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { showGuest = false; onGuest() },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(tr("Tiếp tục dùng thử", "Continue trial")) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
internal fun VerifyCodeScreen(
    email: String,
    state: AuthUiState,
    onVerify: (String) -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit
) {
    var code by rememberSaveable(email) { mutableStateOf("") }
    var resent by rememberSaveable { mutableStateOf(false) }
    // Phone accounts travel under an in-app address; show the number instead.
    val display = if (email.endsWith("@phone.onespace"))
        "0" + email.removeSuffix("@phone.onespace") else email
    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        IconButton(onClick = onBack, enabled = !state.loading) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Quay lại", "Back"))
        }
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.extraLarge) {
            Icon(Icons.Outlined.MarkEmailRead, null, Modifier.padding(24.dp).size(40.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Text(tr("Kiểm tra mã xác thực", "Check your code"), style = MaterialTheme.typography.headlineLarge)
        Text(display, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(tr("Nhập 6 chữ số để tiếp tục. Mã có hiệu lực trong 5 phút.", "Enter the 6 digits to continue. The code is valid for 5 minutes."),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            shape = MaterialTheme.shapes.medium, colors = com.example.onespace.core.designsystem.AppFieldDefaults.colors(),
            value = code, onValueChange = { code = it.filter(Char::isDigit).take(6) },
            label = { Text(tr("Mã OTP", "OTP code")) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            enabled = !state.loading, isError = state.error != null,
            supportingText = { state.error?.let { Text(it.t()) } },
            textStyle = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { onVerify(code) }, enabled = !state.loading,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            if (state.loading) LoadingIndicator() else Text(tr("Xác nhận & tiếp tục", "Confirm & continue"))
        }
        TextButton(onClick = { onResend(); resent = true }, enabled = !state.loading) { Text(tr("Gửi lại mã", "Resend code")) }
        if (resent && state.error == null) Text(tr("Đã tạo lại mã.", "A new code was created."), color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun LoadingIndicator() {
    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
        color = MaterialTheme.colorScheme.onPrimary)
}
