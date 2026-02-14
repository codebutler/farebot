package com.codebutler.farebot.shared.ui.screen

import com.codebutler.farebot.card.CardType

data class HistoryUiState(
    val isLoading: Boolean = true,
    val showAllScans: Boolean = false,
    val allItems: List<HistoryItem> = emptyList(),
    val groupedItems: List<HistoryItem> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
) {
    val items: List<HistoryItem> get() = if (showAllScans) allItems else groupedItems
}

data class HistoryItem(
    val id: String,
    val cardName: String?,
    val serial: String,
    /** Human-friendly date for grouping header, e.g. "Today", "Feb 11". */
    val scannedDate: String?,
    /** Time-only string for the right side, e.g. "2:35 PM". */
    val scannedTime: String?,
    val parseError: String?,
    /** Brand color as 0xRRGGBB (no alpha). Null if unknown. */
    val brandColor: Int? = null,
    val cardType: CardType? = null,
    val keysRequired: Boolean = false,
    val keyBundle: String? = null,
    val preview: Boolean = false,
    val serialOnly: Boolean = false,
    val scanCount: Int = 1,
    val allScanIds: List<String> = emptyList(),
)
