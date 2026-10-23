package com.example.onespace.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onespace.domain.session.repository.SessionPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * First-run flow state: picked interests (at least 3 to continue) and the
 * mock auth outcome. Persists everything, then the shell leaves onboarding.
 */
@HiltViewModel
internal class OnboardingViewModel @Inject constructor(
    private val preferences: SessionPreferencesRepository
) : ViewModel() {

    val topics = listOf(
        "Thiên nhiên", "Đời sống", "Công nghệ", "Du lịch",
        "Nhẹ nhàng", "Trò chơi", "Thể thao", "Ẩm thực"
    )

    private val _selected = MutableStateFlow(emptySet<String>())
    val selected: StateFlow<Set<String>> = _selected.asStateFlow()

    private val _signingIn = MutableStateFlow(false)
    val signingIn: StateFlow<Boolean> = _signingIn.asStateFlow()

    fun toggle(topic: String) = _selected.update { if (topic in it) it - topic else it + topic }

    fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

    /** Mock sign-in: shows the persisted "Đang đăng nhập..." state, then finishes. */
    fun signIn(email: String, onFinished: () -> Unit) {
        if (_signingIn.value) return
        viewModelScope.launch {
            _signingIn.value = true
            delay(800)
            preferences.setAuthEmail(email.trim())
            finish()
            _signingIn.value = false
            onFinished()
        }
    }

    fun continueAsGuest(onFinished: () -> Unit) {
        viewModelScope.launch {
            preferences.setAuthEmail(null)
            finish()
            onFinished()
        }
    }

    private suspend fun finish() {
        // Re-entering Login from Profile must not erase interests chosen earlier.
        if (_selected.value.isNotEmpty()) preferences.setSelectedTopics(_selected.value)
        preferences.setOnboarded(true)
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
