package com.codebutler.farebot.shared.ui.screen

data class HistoryUiState(
    val isLoading: Boolean = true,
    val items: List<HistoryItem> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
)

data class HistoryItem(
    val id: String,
    val cardName: String?,
    val serial: String,
    val scannedAt: String?,
    val parseError: String?,
)
