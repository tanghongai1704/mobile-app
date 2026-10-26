package com.example.onespace.domain.auth.repository

import com.example.onespace.domain.auth.model.AuthSession
import kotlinx.coroutines.flow.Flow

/** Authentication boundary. Implementations decide how codes and sessions are stored. */
interface AuthRepository {
    val session: Flow<AuthSession>
    suspend fun requestCode(email: String): Result<Unit>
    suspend fun verifyCode(email: String, code: String): Result<Unit>
    suspend fun signInWithPassword(identity: String, password: String): Result<Unit>
    suspend fun continueAsGuest()
    suspend fun signOut()
}
