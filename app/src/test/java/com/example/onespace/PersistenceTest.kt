package com.example.onespace

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.onespace.core.datastore.LocalJsonStore
import com.example.onespace.core.datastore.SessionPreferencesStore
import com.example.onespace.core.designsystem.AppPlaylistStore
import com.example.onespace.data.news.createNewsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** State that used to live only in memory now has to survive a restart. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class PersistenceTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun newsBookmarksSurviveANewRepositoryInstance() = runBlocking {
        val first = createNewsRepository(context)
        first.toggleBookmark("a1")
        first.toggleBookmark("a2")
        first.toggleBookmark("a1") // toggled off again
        val second = createNewsRepository(context)
        assertEquals(setOf("a2"), second.observeSavedIds().first())
    }

    @Test fun playlistsRoundTripThroughJson() {
        AppPlaylistStore.restore("{}")
        AppPlaylistStore.create("Chill", videoIds = listOf("v1"), trackIds = listOf("m1"))
        AppPlaylistStore.addTrack("Chill", "m2")
        AppPlaylistStore.toggleOrder("Chill")
        AppPlaylistStore.toggleRepeat("Chill")
        val saved = AppPlaylistStore.toJson()

        AppPlaylistStore.restore("{}")
        assertTrue(AppPlaylistStore.playlists.isEmpty())
        AppPlaylistStore.restore(saved)

        val chill = AppPlaylistStore.playlists.getValue("Chill")
        assertEquals(listOf("v1"), chill.videoIds)
        assertEquals(listOf("m1", "m2"), chill.trackIds)
        assertEquals("shuffle", AppPlaylistStore.settingsOf("Chill").order)
        assertTrue(AppPlaylistStore.settingsOf("Chill").repeat)
    }

    @Test fun brokenPlaylistDataIsIgnored() {
        AppPlaylistStore.restore("{}")
        AppPlaylistStore.create("Keep")
        AppPlaylistStore.restore("this is not json")
        assertTrue("Keep" in AppPlaylistStore.playlists)
    }

    @Test fun playlistChangesAreReportedForSaving() {
        val seen = mutableListOf<String>()
        AppPlaylistStore.restore("{}")
        AppPlaylistStore.onChanged = { seen += it }
        AppPlaylistStore.create("Road trip")
        AppPlaylistStore.remove("Road trip")
        AppPlaylistStore.onChanged = null
        assertEquals(2, seen.size)
        assertTrue(seen.first().contains("Road trip"))
        assertFalse(seen.last().contains("Road trip"))
    }

    @Test fun avatarAndFrameAreSavedInPreferences() = runBlocking {
        val prefs = SessionPreferencesStore(context)
        assertNull(prefs.preferences.first().avatarEmoji)
        assertEquals("basic", prefs.preferences.first().avatarFrameId)
        prefs.setAvatar("🦊")
        prefs.setAvatarFrame("bac")
        val now = prefs.preferences.first()
        assertEquals("🦊", now.avatarEmoji)
        assertEquals("bac", now.avatarFrameId)
    }

    @Test fun localJsonStoreKeepsWhatWasWritten() = runBlocking {
        val store = LocalJsonStore(context)
        assertNull(store.read("missing-key"))
        store.write("k", "{\"a\":1}")
        assertEquals("{\"a\":1}", LocalJsonStore(context).read("k"))
    }
}
