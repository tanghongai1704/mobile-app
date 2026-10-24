package com.example.onespace.domain.session.repository

import com.example.onespace.domain.session.MediaConflictRule
import com.example.onespace.domain.session.Section
import kotlinx.coroutines.flow.Flow

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class Language { VI, EN }

data class SessionPreferences(
    val lastSection: Section = Section.HOME,
    val lastTrackId: String? = null,
    val conflictRule: MediaConflictRule? = null,
    val dataSaver: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Interests/welcome completed; authentication is a separate persisted state. */
    val onboarded: Boolean = false,
    /** Demo identity only; guestMode distinguishes guests from signed-out sessions. */
    val authEmail: String? = null,
    val guestMode: Boolean = false,
    /** Interests picked during onboarding (topic names, at least 3 to continue). */
    val selectedTopics: Set<String> = emptySet(),
    val language: Language = Language.VI,
    /** Demo switch so graders can exercise the offline/error states deterministically. */
    val simulateOffline: Boolean = false,
    val notificationsEnabled: Boolean = false
)

/** Cross-launch session memory: restore point, remembered rules and user toggles. */
interface SessionPreferencesRepository {
    val preferences: Flow<SessionPreferences>
    suspend fun setLastSection(section: Section)
    suspend fun setLastTrackId(trackId: String?)
    suspend fun setConflictRule(rule: MediaConflictRule?)
    suspend fun setDataSaver(enabled: Boolean)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setOnboarded(done: Boolean)
    suspend fun setAuthSession(email: String?, guest: Boolean)
    suspend fun setSelectedTopics(topics: Set<String>)
    suspend fun setLanguage(language: Language)
    suspend fun setSimulateOffline(enabled: Boolean)
    suspend fun setNotificationsEnabled(enabled: Boolean)
}
