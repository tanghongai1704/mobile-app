package com.example.onespace.domain.auth.model

data class AuthSession(val email: String? = null, val isGuest: Boolean = false) {
    val hasAccess: Boolean get() = email != null || isGuest
}

enum class AuthError { INVALID_EMAIL, INVALID_CODE, CODE_EXPIRED, TOO_MANY_ATTEMPTS, UNAVAILABLE, WEAK_PASSWORD }

class AuthException(val reason: AuthError) : Exception(reason.name)
