package com.example.onespace.feature.auth.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onespace.domain.auth.model.AuthError
import com.example.onespace.domain.auth.model.AuthException
import com.example.onespace.domain.auth.usecase.ContinueAsGuest
import com.example.onespace.domain.auth.usecase.RequestSignInCode
import com.example.onespace.domain.auth.usecase.SignInWithPassword
import com.example.onespace.domain.auth.usecase.VerifySignInCode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val codeSentTo: String? = null
)

@HiltViewModel
internal class AuthViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val requestCode: RequestSignInCode,
    private val verifyCode: VerifySignInCode,
    private val signIn: SignInWithPassword,
    private val guest: ContinueAsGuest
) : ViewModel() {
    val email = savedState.getStateFlow("email", "")
    /** F-COM-01: phone login shares the demo OTP path of the email flow. */
    val phone = savedState.getStateFlow("phone", "")
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    fun setEmail(value: String) {
        savedState["email"] = value
        _uiState.update { it.copy(error = null) }
    }

    fun setPhone(value: String) {
        savedState["phone"] = value.filter(Char::isDigit).take(11)
        _uiState.update { it.copy(error = null) }
    }

    /** Stable in-app address so the demo backend can key a phone account. */
    private fun pseudoEmail(digits: String) = digits + "@phone.onespace"

    /** F-COM-01: phone + password signs in directly, no OTP round-trip. */
    fun signInWithPhonePassword(password: String) = submit {
        val digits = phone.value
        if (digits.length !in 9..11) {
            _uiState.update { it.copy(error = "Số điện thoại không hợp lệ") }
            return@submit
        }
        if (password.length < 6) {
            _uiState.update { it.copy(error = "Mật khẩu cần ít nhất 6 ký tự") }
            return@submit
        }
        signIn(pseudoEmail(digits), password).fold(
            onSuccess = { },
            onFailure = ::showError
        )
    }

    fun signInWithEmailPassword(password: String) = submit {
        if (email.value.isBlank()) {
            _uiState.update { it.copy(error = "Email chưa hợp lệ") }
            return@submit
        }
        if (password.length < 6) {
            _uiState.update { it.copy(error = "Mật khẩu cần ít nhất 6 ký tự") }
            return@submit
        }
        signIn(email.value.trim().lowercase(), password).fold(
            onSuccess = { },
            onFailure = ::showError
        )
    }

    fun requestPhoneCode() = submit {
        val digits = phone.value
        if (digits.length !in 9..11) {
            _uiState.update { it.copy(error = "Số điện thoại không hợp lệ") }
            return@submit
        }
        // The code must be verified under the same key it was issued to.
        val pseudo = pseudoEmail(digits)
        requestCode(pseudo).fold(
            onSuccess = { _uiState.update { it.copy(codeSentTo = pseudo) } },
            onFailure = ::showError
        )
    }

    fun requestCode() = submit {
        requestCode(email.value).fold(
            onSuccess = { _uiState.update { it.copy(codeSentTo = email.value.trim().lowercase()) } },
            onFailure = ::showError
        )
    }

    fun verify(email: String, code: String) = submit {
        verifyCode(email, code).fold(
            onSuccess = { },
            onFailure = ::showError
        )
    }

    fun resend(email: String) = submit {
        requestCode(email).onFailure(::showError)
    }

    fun continueAsGuest() = submit {
        guest()
    }

    fun consumeNavigation() = _uiState.update { it.copy(codeSentTo = null) }

    private fun submit(action: suspend () -> Unit) {
        if (_uiState.value.loading) return
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try { action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { showError(error) }
            finally { _uiState.update { it.copy(loading = false) } }
        }
    }

    private fun showError(error: Throwable) {
        val message = when ((error as? AuthException)?.reason) {
            AuthError.INVALID_EMAIL -> "Email chưa hợp lệ — kiểm tra lại giúp mình nhé."
            AuthError.INVALID_CODE -> "Mã xác thực chưa đúng. Nhập 6 chữ số, mã demo là 123456."
            AuthError.CODE_EXPIRED -> "Mã đã hết hạn. Vui lòng gửi lại mã."
            AuthError.TOO_MANY_ATTEMPTS -> "Bạn đã thử quá nhiều lần. Vui lòng gửi lại mã."
            AuthError.WEAK_PASSWORD -> "Mật khẩu cần ít nhất 6 ký tự."
            else -> "Chưa thể đăng nhập. Vui lòng thử lại."
        }
        _uiState.update { it.copy(error = message) }
    }
}
