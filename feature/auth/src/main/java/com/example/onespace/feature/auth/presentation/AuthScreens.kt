package com.example.onespace.feature.auth.presentation

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.onespace.core.designsystem.OneSpaceBrand

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LoginScreen(
    email: String,
    phone: String = "",
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit = {},
    onRequestCode: () -> Unit,
    onRequestPhoneCode: () -> Unit = {},
    onGuest: () -> Unit
) {
    var showGuest by rememberSaveable { mutableStateOf(false) }
    var password by rememberSaveable { mutableStateOf("") }
    var showGoogle by rememberSaveable { mutableStateOf(false) }
    // F-COM-01: phone-first login, email still available on the second tab.
    var phoneMode by rememberSaveable { mutableStateOf(true) }
    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        OneSpaceBrand(compact = true)
        Text("Không gian của bạn.", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text("Đăng nhập / Đăng ký", style = MaterialTheme.typography.titleLarge)
        Text("Một tài khoản. Bốn cách tận hưởng mỗi ngày.",
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = phoneMode,
                        onClick = { phoneMode = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        label = { Text("Số điện thoại") }
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
                        value = phone, onValueChange = onPhoneChange, enabled = !state.loading,
                        label = { Text("Số điện thoại") }, singleLine = true,
                        prefix = { Text("+84 ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = state.error != null,
                        supportingText = { state.error?.let { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = onRequestPhoneCode, enabled = !state.loading,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        if (state.loading) LoadingIndicator() else Text("Gửi mã OTP")
                    }
                } else {
                    OutlinedTextField(
                        value = email, onValueChange = onEmailChange, enabled = !state.loading,
                        label = { Text("Email") }, singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Email, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = state.error != null,
                        supportingText = { state.error?.let { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        enabled = !state.loading,
                        label = { Text("Mật khẩu") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = onRequestCode, enabled = !state.loading && email.isNotBlank() && password.length >= 6,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        if (state.loading) LoadingIndicator() else Text("Đăng nhập")
                    }
                }
                // F-COM-01: Google is offered, the demo build only explains it.
                OutlinedButton(onClick = { showGoogle = true }, enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text("Tiếp tục với Google")
                }
            }
        }
        TextButton(onClick = { showGuest = true }, enabled = !state.loading) {
            Text("Dùng thử không cần tài khoản")
        }
        Spacer(Modifier.height(12.dp))
    }

    if (showGoogle) ModalBottomSheet(onDismissRequest = { showGoogle = false }) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Đăng nhập bằng Google", style = MaterialTheme.typography.headlineSmall)
            Text("Bản demo của môn học chưa kết nối Google Sign-In. Trong bản phát hành, bước này mở cửa sổ chọn tài khoản Google của bạn.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { showGoogle = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("Đã hiểu")
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showGuest) ModalBottomSheet(onDismissRequest = { showGuest = false }) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Dùng thử không cần tài khoản", style = MaterialTheme.typography.headlineSmall)
            Text("Khám phá video, âm nhạc, tin tức và trò chơi. Phiên dùng thử được lưu trên thiết bị này.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { showGuest = false; onGuest() },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Tiếp tục dùng thử") }
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
    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        IconButton(onClick = onBack, enabled = !state.loading) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Đổi email")
        }
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.extraLarge) {
            Icon(Icons.Outlined.MarkEmailRead, null, Modifier.padding(24.dp).size(40.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Text("Kiểm tra mã xác thực", style = MaterialTheme.typography.headlineLarge)
        Text(email, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text("Nhập 6 chữ số để tiếp tục. Mã có hiệu lực trong 5 phút.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = code, onValueChange = { code = it.filter(Char::isDigit).take(6) },
            label = { Text("Mã OTP") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            enabled = !state.loading, isError = state.error != null,
            supportingText = { state.error?.let { Text(it) } },
            textStyle = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { onVerify(code) }, enabled = !state.loading,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            if (state.loading) LoadingIndicator() else Text("Xác nhận & tiếp tục")
        }
        TextButton(onClick = { onResend(); resent = true }, enabled = !state.loading) { Text("Gửi lại mã") }
        if (resent && state.error == null) Text("Đã tạo lại mã demo.", color = MaterialTheme.colorScheme.primary)
        Text("Chế độ demo · Nhập 123456. Không gửi email thật.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LoadingIndicator() {
    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
        color = MaterialTheme.colorScheme.onPrimary)
}
