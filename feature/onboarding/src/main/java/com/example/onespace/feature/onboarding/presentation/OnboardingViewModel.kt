package com.example.onespace.feature.onboarding.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onespace.domain.session.repository.SessionPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
internal class OnboardingViewModel @Inject constructor(
    private val preferences: SessionPreferencesRepository,
    private val savedState: SavedStateHandle
) : ViewModel() {
    val topics = listOf("Thiên nhiên", "Đời sống", "Công nghệ", "Du lịch",
        "Nhẹ nhàng", "Trò chơi", "Thể thao", "Ẩm thực")
    val selected = savedState.getStateFlow("topics", arrayListOf<String>())
    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()

    fun toggle(topic: String) {
        val next = selected.value.toMutableSet()
        if (!next.add(topic)) next.remove(topic)
        savedState["topics"] = ArrayList(next)
    }

    fun finish(skip: Boolean = false) {
        if (_saving.value || (!skip && selected.value.size < 3)) return
        _saving.value = true
        viewModelScope.launch {
            try {
                if (selected.value.isNotEmpty()) preferences.setSelectedTopics(selected.value.toSet())
                preferences.setOnboarded(true)
            } finally { _saving.value = false }
        }
    }
}
