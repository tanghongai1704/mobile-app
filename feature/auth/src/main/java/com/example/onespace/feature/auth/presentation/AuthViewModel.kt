package com.example.onespace.feature.auth.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onespace.domain.auth.model.AuthError
import com.example.onespace.domain.auth.model.AuthException
import com.example.onespace.domain.auth.usecase.ContinueAsGuest
import com.example.onespace.domain.auth.usecase.RequestSignInCode
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
    private val guest: ContinueAsGuest
) : ViewModel() {
    val email = savedState.getStateFlow("email", "")
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    fun setEmail(value: String) {
        savedState["email"] = value
        _uiState.update { it.copy(error = null) }
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
            else -> "Chưa thể đăng nhập. Vui lòng thử lại."
        }
        _uiState.update { it.copy(error = message) }
    }
}
