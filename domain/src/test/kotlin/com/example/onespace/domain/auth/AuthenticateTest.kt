package com.example.onespace.domain.auth

import com.example.onespace.domain.auth.model.AuthSession
import com.example.onespace.domain.auth.repository.AuthRepository
import com.example.onespace.domain.auth.usecase.RequestSignInCode
import com.example.onespace.domain.auth.usecase.VerifySignInCode
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AuthenticateTest {
    private val repository = RecordingAuth()

    @Test fun invalidEmailNeverRequestsAChallenge() = runBlocking {
        assertTrue(RequestSignInCode(repository)("invalid").isFailure)
        assertNull(repository.email)
    }

    @Test fun emailIsNormalizedBeforeRequestAndVerification() = runBlocking {
        assertTrue(RequestSignInCode(repository)(" A@Example.COM ").isSuccess)
        assertEquals("a@example.com", repository.email)
        assertTrue(VerifySignInCode(repository)(" A@Example.COM ", "123456").isSuccess)
        assertEquals("a@example.com", repository.email)
    }

    @Test fun malformedCodesNeverReachTheRepository() = runBlocking {
        for (code in listOf("", "12345", "1234567", "abcdef")) {
            assertTrue(VerifySignInCode(repository)("a@example.com", code).isFailure)
        }
        assertNull(repository.email)
    }

    private class RecordingAuth : AuthRepository {
        var email: String? = null
        override val session = flowOf(AuthSession())
        override suspend fun requestCode(email: String): Result<Unit> {
            this.email = email
            return Result.success(Unit)
        }
        override suspend fun verifyCode(email: String, code: String): Result<Unit> {
            this.email = email
            return Result.success(Unit)
        }
        override suspend fun signInWithPassword(identity: String, password: String): Result<Unit> {
            this.email = identity
            return Result.success(Unit)
        }
        override suspend fun continueAsGuest() = Unit
        override suspend fun signOut() = Unit
    }
}
