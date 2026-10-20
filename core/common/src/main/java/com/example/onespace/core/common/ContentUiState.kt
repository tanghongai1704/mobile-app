package com.example.onespace.core.common

/** Immutable screen state; error/loading branches are ready for asynchronous repositories. */
data class ContentUiState<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
