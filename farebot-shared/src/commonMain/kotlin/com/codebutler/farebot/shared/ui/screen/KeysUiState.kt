package com.codebutler.farebot.shared.ui.screen

data class KeysUiState(
    val isLoading: Boolean = true,
    val keys: List<KeyItem> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
)

data class KeyItem(
    val id: String,
    val cardId: String,
    val cardType: String,
)
