package com.example.onespace.core.media

import android.content.Context
import com.example.onespace.domain.session.port.OfflineMediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores downloaded media in the app's private storage (`files/offline/`), so no storage
 * permission is needed and the files go away with the app. Writes to a temporary file first, so a
 * cut connection never leaves a half file that the player would try to use.
 */
@Singleton
class FileOfflineMediaStore @Inject constructor(
    @ApplicationContext private val context: Context
) : OfflineMediaStore {

    private val dir: File get() = File(context.filesDir, "offline").apply { mkdirs() }

    private fun fileOf(contentId: String) = File(dir, contentId.replace(Regex("[^A-Za-z0-9_-]"), "_"))

    override suspend fun save(contentId: String, url: String): Boolean = withContext(Dispatchers.IO) {
        val target = fileOf(contentId)
        if (target.exists() && target.length() > 0) return@withContext true
        val temp = File(dir, target.name + ".part")
        runCatching {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
            }
            try {
                if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
                connection.inputStream.use { input -> temp.outputStream().use { input.copyTo(it) } }
            } finally {
                connection.disconnect()
            }
            temp.renameTo(target) && target.length() > 0
        }.getOrElse {
            temp.delete()
            false
        }
    }

    override suspend fun delete(contentId: String) {
        withContext(Dispatchers.IO) { fileOf(contentId).delete() }
    }

    override fun localUri(contentId: String): String? =
        fileOf(contentId).takeIf { it.exists() && it.length() > 0 }?.toURI()?.toString()
}
