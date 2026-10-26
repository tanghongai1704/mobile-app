package com.example.onespace.data.auth

import com.example.onespace.domain.auth.model.AuthError
import com.example.onespace.domain.auth.model.AuthException
import com.example.onespace.domain.auth.model.AuthSession
import com.example.onespace.domain.auth.repository.AuthRepository
import com.example.onespace.domain.session.repository.SessionPreferencesRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Local demo only: never sends email or represents a verified server identity. */
internal class DemoAuthRepository(
    private val preferences: SessionPreferencesRepository,
    private val now: () -> Long = System::currentTimeMillis
) : AuthRepository {
    override val session = preferences.preferences.map { AuthSession(it.authEmail, it.guestMode) }
    private data class Challenge(val email: String, val expiresAt: Long, var attempts: Int = 0)
    private val mutex = Mutex()
    private var challenge: Challenge? = null

    override suspend fun requestCode(email: String): Result<Unit> = mutex.withLock {
        challenge = Challenge(email, now() + 5 * 60_000)
        Result.success(Unit)
    }

    override suspend fun verifyCode(email: String, code: String): Result<Unit> = mutex.withLock {
        val current = challenge
        val error = when {
            current == null || now() >= current.expiresAt -> AuthError.CODE_EXPIRED
            current.email != email -> AuthError.INVALID_CODE
            current.attempts >= 5 -> AuthError.TOO_MANY_ATTEMPTS
            code != "123456" -> { current.attempts++; AuthError.INVALID_CODE }
            else -> null
        }
        if (error != null) return@withLock Result.failure(AuthException(error))
        preferences.setAuthSession(email, guest = false)
        challenge = null
        Result.success(Unit)
    }

    /** Demo accepts any password of valid length; the identity becomes the account. */
    override suspend fun signInWithPassword(identity: String, password: String): Result<Unit> = mutex.withLock {
        if (password.length < 6) return@withLock Result.failure(AuthException(AuthError.WEAK_PASSWORD))
        preferences.setAuthSession(identity, guest = false)
        challenge = null
        Result.success(Unit)
    }

    override suspend fun continueAsGuest() {
        mutex.withLock {
            preferences.setAuthSession(null, guest = true)
            challenge = null
        }
    }

    override suspend fun signOut() {
        mutex.withLock {
            preferences.setAuthSession(null, guest = false)
            challenge = null
        }
    }
}

fun createAuthRepository(preferences: SessionPreferencesRepository): AuthRepository =
    DemoAuthRepository(preferences)
