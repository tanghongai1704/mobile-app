package com.example.onespace.domain.auth.usecase

import com.example.onespace.domain.auth.model.AuthError
import com.example.onespace.domain.auth.model.AuthException
import com.example.onespace.domain.auth.repository.AuthRepository

class RequestSignInCode(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String): Result<Unit> {
        val normalized = email.trim().lowercase()
        if (!EMAIL.matches(normalized)) return Result.failure(AuthException(AuthError.INVALID_EMAIL))
        return repository.requestCode(normalized)
    }

    private companion object {
        val EMAIL = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}

class VerifySignInCode(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, code: String): Result<Unit> {
        if (!code.matches(Regex("[0-9]{6}"))) return Result.failure(AuthException(AuthError.INVALID_CODE))
        return repository.verifyCode(email.trim().lowercase(), code)
    }
}

class ContinueAsGuest(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.continueAsGuest()
}

class SignOut(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.signOut()
}
