package com.example.onespace.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.onespace.domain.session.MediaConflictRule
import com.example.onespace.domain.session.Section
import com.example.onespace.domain.session.repository.Language
import com.example.onespace.domain.session.repository.SessionPreferences
import com.example.onespace.domain.session.repository.SessionPreferencesRepository
import com.example.onespace.domain.session.repository.ThemeMode
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "onespace_session")

/** DataStore-backed implementation of the domain session preferences contract. */
@Singleton
class SessionPreferencesStore @Inject constructor(@ApplicationContext private val context: Context) :
    SessionPreferencesRepository {

    private object Keys {
        val LAST_SECTION = stringPreferencesKey("last_section")
        val LAST_TRACK = stringPreferencesKey("last_track")
        val CONFLICT_RULE = stringPreferencesKey("conflict_rule")
        val DATA_SAVER = booleanPreferencesKey("data_saver")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val AUTH_EMAIL = stringPreferencesKey("auth_email")
        val TOPICS = stringSetPreferencesKey("selected_topics")
        val LANGUAGE = stringPreferencesKey("language")
        val SIM_OFFLINE = booleanPreferencesKey("simulate_offline")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
    }

    override val preferences: Flow<SessionPreferences> = context.sessionDataStore.data.map {
        SessionPreferences(
            lastSection = it[Keys.LAST_SECTION]?.let(Section::valueOf) ?: Section.HOME,
            lastTrackId = it[Keys.LAST_TRACK],
            conflictRule = it[Keys.CONFLICT_RULE]?.let(MediaConflictRule::valueOf),
            dataSaver = it[Keys.DATA_SAVER] ?: false,
            themeMode = it[Keys.THEME_MODE]?.let(ThemeMode::valueOf) ?: ThemeMode.SYSTEM,
            onboarded = it[Keys.ONBOARDED] ?: false,
            authEmail = it[Keys.AUTH_EMAIL],
            selectedTopics = it[Keys.TOPICS] ?: emptySet(),
            language = it[Keys.LANGUAGE]?.let(Language::valueOf) ?: Language.VI,
            simulateOffline = it[Keys.SIM_OFFLINE] ?: false,
            notificationsEnabled = it[Keys.NOTIFICATIONS] ?: false
        )
    }

    override suspend fun setLastSection(section: Section) = set { it[Keys.LAST_SECTION] = section.name }
    override suspend fun setLastTrackId(trackId: String?) = set {
        if (trackId == null) it.remove(Keys.LAST_TRACK) else it[Keys.LAST_TRACK] = trackId
    }
    override suspend fun setConflictRule(rule: MediaConflictRule?) = set {
        if (rule == null) it.remove(Keys.CONFLICT_RULE) else it[Keys.CONFLICT_RULE] = rule.name
    }
    override suspend fun setDataSaver(enabled: Boolean) = set { it[Keys.DATA_SAVER] = enabled }
    override suspend fun setThemeMode(mode: ThemeMode) = set { it[Keys.THEME_MODE] = mode.name }
    override suspend fun setOnboarded(done: Boolean) = set { it[Keys.ONBOARDED] = done }
    override suspend fun setAuthEmail(email: String?) = set {
        if (email == null) it.remove(Keys.AUTH_EMAIL) else it[Keys.AUTH_EMAIL] = email
    }
    override suspend fun setSelectedTopics(topics: Set<String>) = set { it[Keys.TOPICS] = topics }
    override suspend fun setLanguage(language: Language) = set { it[Keys.LANGUAGE] = language.name }
    override suspend fun setSimulateOffline(enabled: Boolean) = set { it[Keys.SIM_OFFLINE] = enabled }
    override suspend fun setNotificationsEnabled(enabled: Boolean) = set { it[Keys.NOTIFICATIONS] = enabled }

    private suspend fun set(block: (MutablePreferences) -> Unit) {
        context.sessionDataStore.edit(block)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides @Singleton
    fun provideSessionPreferences(store: SessionPreferencesStore): SessionPreferencesRepository = store
}
