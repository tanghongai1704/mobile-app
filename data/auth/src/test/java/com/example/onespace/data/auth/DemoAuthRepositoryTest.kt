package com.example.onespace.data.auth

import com.example.onespace.domain.auth.model.AuthError
import com.example.onespace.domain.auth.model.AuthException
import com.example.onespace.domain.session.MediaConflictRule
import com.example.onespace.domain.session.Section
import com.example.onespace.domain.session.repository.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DemoAuthRepositoryTest {
    private val prefs = MemoryPreferences()
    private var time = 1_000L
    private val auth = DemoAuthRepository(prefs) { time }

    @Test fun correctCodeCreatesPersistedSessionAndCannotBeReused() = runBlocking {
        auth.requestCode("a@example.com")
        assertTrue(auth.verifyCode("a@example.com", "123456").isSuccess)
        assertEquals("a@example.com", auth.session.first().email)
        assertFalse(auth.session.first().isGuest)
        assertEquals(AuthError.CODE_EXPIRED, error(auth.verifyCode("a@example.com", "123456")))
    }

    @Test fun incorrectCodeAndAnotherEmailNeverAuthenticate() = runBlocking {
        auth.requestCode("a@example.com")
        assertEquals(AuthError.INVALID_CODE, error(auth.verifyCode("b@example.com", "123456")))
        assertEquals(AuthError.INVALID_CODE, error(auth.verifyCode("a@example.com", "000000")))
        assertFalse(auth.session.first().hasAccess)
    }

    @Test fun expiredCodeRequiresResend() = runBlocking {
        auth.requestCode("a@example.com")
        time += 300_000
        assertEquals(AuthError.CODE_EXPIRED, error(auth.verifyCode("a@example.com", "123456")))
        auth.requestCode("a@example.com")
        assertTrue(auth.verifyCode("a@example.com", "123456").isSuccess)
    }

    @Test fun fiveWrongAttemptsBlockTheChallengeUntilResent() = runBlocking {
        auth.requestCode("a@example.com")
        repeat(5) { auth.verifyCode("a@example.com", "000000") }
        assertEquals(AuthError.TOO_MANY_ATTEMPTS, error(auth.verifyCode("a@example.com", "123456")))
        auth.requestCode("a@example.com")
        assertTrue(auth.verifyCode("a@example.com", "123456").isSuccess)
    }

    @Test fun guestRestoresAcrossRepositoryInstancesAndSignOutRemovesAccess() = runBlocking {
        auth.continueAsGuest()
        val restored = DemoAuthRepository(prefs)
        assertTrue(restored.session.first().isGuest)
        assertTrue(restored.session.first().hasAccess)
        restored.signOut()
        assertFalse(auth.session.first().hasAccess)
        assertTrue(prefs.preferences.value.onboarded)
        assertEquals(setOf("Công nghệ"), prefs.preferences.value.selectedTopics)
    }

    private fun error(result: Result<Unit>) = (result.exceptionOrNull() as AuthException).reason

    private class MemoryPreferences : SessionPreferencesRepository {
        override val preferences = MutableStateFlow(
            SessionPreferences(onboarded = true, selectedTopics = setOf("Công nghệ"))
        )
        override suspend fun setAuthSession(email: String?, guest: Boolean) {
            preferences.value = preferences.value.copy(authEmail = email, guestMode = guest)
        }
        override suspend fun setLastSection(section: Section) = Unit
        override suspend fun setLastTrackId(trackId: String?) = Unit
        override suspend fun setConflictRule(rule: MediaConflictRule?) = Unit
        override suspend fun setDataSaver(enabled: Boolean) = Unit
        override suspend fun setThemeMode(mode: ThemeMode) = Unit
        override suspend fun setOnboarded(done: Boolean) = Unit
        override suspend fun setSelectedTopics(topics: Set<String>) = Unit
        override suspend fun setFavoriteTrackIds(ids: Set<String>) = Unit
        override suspend fun setLanguage(language: Language) = Unit
        override suspend fun setSimulateOffline(enabled: Boolean) = Unit
        override suspend fun setNotificationsEnabled(enabled: Boolean) = Unit
        override suspend fun setStreakReminder(enabled: Boolean) = Unit
        override suspend fun setQuietHours(enabled: Boolean) = Unit
    }
}
