package com.example.onespace.data.auth

import android.content.Context
import com.example.onespace.core.network.ApiException
import com.example.onespace.core.network.NetworkUnavailableException
import com.example.onespace.core.network.api.ApiErrorCode
import com.example.onespace.core.network.api.AuthApi
import com.example.onespace.core.network.api.LoginRequest
import com.example.onespace.core.network.api.OtpRequest
import com.example.onespace.core.network.api.TokenDto
import com.example.onespace.core.network.api.VerifyRequest
import com.example.onespace.core.network.apiCall
import com.example.onespace.core.network.fake.FakeRoute
import com.example.onespace.domain.auth.model.AuthError
import com.example.onespace.domain.auth.model.AuthException
import com.example.onespace.domain.auth.model.AuthSession
import com.example.onespace.domain.auth.repository.AuthRepository
import com.example.onespace.domain.session.repository.SessionPreferencesRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Talks to `/auth/...` through Retrofit. Tokens go to [SecureTokenStore] (Keystore-encrypted); the
 * signed-in identity and guest flag stay in session preferences so routing can restore offline.
 */
internal class RemoteAuthRepository(
    private val api: AuthApi,
    private val preferences: SessionPreferencesRepository,
    private val tokens: SecureTokenStore
) : AuthRepository {
    override val session = preferences.preferences.map { AuthSession(it.authEmail, it.guestMode) }
    private val mutex = Mutex()

    override suspend fun requestCode(email: String): Result<Unit> = mutex.withLock {
        call { apiCall { api.requestOtp(OtpRequest(email)) } }
    }

    override suspend fun verifyCode(email: String, code: String): Result<Unit> = mutex.withLock {
        call { startSession(apiCall { api.verify(VerifyRequest(email, code)) }) }
    }

    override suspend fun signInWithPassword(identity: String, password: String): Result<Unit> = mutex.withLock {
        call { startSession(apiCall { api.login(LoginRequest(identity, password)) }) }
    }

    override suspend fun continueAsGuest() = mutex.withLock {
        tokens.clear()
        preferences.setAuthSession(null, guest = true)
    }

    override suspend fun signOut() = mutex.withLock {
        // Revoking on the server is best effort; the local session ends either way.
        if (tokens.accessToken() != null) runCatching { apiCall { api.logout() } }
        tokens.clear()
        preferences.setAuthSession(null, guest = false)
    }

    private suspend fun startSession(token: TokenDto) {
        tokens.save(token.accessToken, token.refreshToken)
        preferences.setAuthSession(token.user.identity, guest = false)
    }

    private inline fun call(block: () -> Unit): Result<Unit> = try {
        block()
        Result.success(Unit)
    } catch (e: ApiException) {
        Result.failure(AuthException(e.toAuthError()))
    } catch (e: NetworkUnavailableException) {
        Result.failure(AuthException(AuthError.UNAVAILABLE))
    }

    private fun ApiException.toAuthError() = when (code) {
        ApiErrorCode.OTP_EXPIRED -> AuthError.CODE_EXPIRED
        ApiErrorCode.OTP_INVALID -> AuthError.INVALID_CODE
        ApiErrorCode.RATE_LIMITED -> AuthError.TOO_MANY_ATTEMPTS
        ApiErrorCode.INVALID_PARAMS ->
            if (details["field"] == "password") AuthError.WEAK_PASSWORD else AuthError.INVALID_EMAIL
        else -> AuthError.UNAVAILABLE
    }
}

/** Token storage must exist before OkHttp is built, so it is created separately from the repository. */
fun createSecureTokenStore(context: Context): SecureTokenStore =
    SecureTokenStore(SharedPreferencesBlobStore(context.applicationContext), KeystoreTokenCipher())

fun createAuthRepository(
    api: AuthApi,
    preferences: SessionPreferencesRepository,
    tokens: SecureTokenStore
): AuthRepository = RemoteAuthRepository(api, preferences, tokens)

/** The `/auth/...` slice of the fake backend. */
fun createAuthFakeRoute(): FakeRoute = AuthFakeRoute()
