package com.example.onespace.feature.music.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onespace.core.common.ContentUiState
import com.example.onespace.domain.music.model.Track
import com.example.onespace.domain.music.usecase.GetMusic
import com.example.onespace.domain.session.repository.SessionPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
internal class MusicViewModel @Inject constructor(
    getMusic: GetMusic,
    private val preferences: SessionPreferencesRepository,
    extractedAudio: com.example.onespace.domain.music.repository.ExtractedAudioRepository,
    private val downloadedContent: com.example.onespace.domain.session.repository.DownloadedContentRepository
) : ViewModel() {
    private val refresh = MutableStateFlow(0)

    val uiState = combine(getMusic(), refresh) { items, _ -> ContentUiState(items = items) }
        .catch { emit(ContentUiState(error = "Không tải được kho nhạc.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContentUiState(isLoading = true))

    /** F-VID-10/F-MUS-07: the "Từ video của bạn" shelf in the music library. */
    val extracted = extractedAudio.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<com.example.onespace.domain.music.repository.ExtractedAudio>())

    /** Demo switch from Settings: feeds render their offline state while on. */
    val offline = preferences.preferences.map { it.simulateOffline }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun retry() { refresh.value = refresh.value + 1 }

    /** F-MUS-07: favorites drive the "Thư viện" badges and suggestions;
     *  persisted in DataStore so they survive restarts. */
    val favorites: StateFlow<Set<String>> = preferences.preferences
        .map { it.favoriteTrackIds }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            val current = favorites.value
            preferences.setFavoriteTrackIds(if (id in current) current - id else current + id)
        }
    }

    /** F-MUS-12: downloaded tracks, persisted in Room (DB v5). */
    val downloads: kotlinx.coroutines.flow.StateFlow<Set<String>> =
        downloadedContent.observeByType("music")
            .map { list -> list.map { it.contentId }.toSet() }
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), emptySet())

    fun toggleDownload(id: String, title: String = "", subtitle: String = "") {
        viewModelScope.launch {
            if (id in downloads.value) downloadedContent.remove(id)
            else downloadedContent.add(id, "music", title, subtitle)
        }
    }

    /** F-MUS-08: suggest tracks from the artists the listener already loves. */
    fun suggested(tracks: List<Track>): List<Track> {
        val loved = favorites.value
        if (loved.isEmpty()) return emptyList()
        val artists = tracks.filter { it.id in loved }.map { it.artist }.toSet()
        return tracks.filter { it.artist in artists && it.id !in loved }.take(5)
    }

}
