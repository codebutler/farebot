package com.codebutler.farebot.shared.ui.screen

import com.codebutler.farebot.base.ui.FareBotUiTree
import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.transit.Trip

data class CardUiState(
    val isLoading: Boolean = true,
    val cardName: String? = null,
    val serialNumber: String? = null,
    val balances: List<BalanceItem> = emptyList(),
    val transactions: List<TransactionItem> = emptyList(),
    val infoItems: List<InfoItem> = emptyList(),
    val warning: String? = null,
    val emptyStateMessage: String? = null,
    val error: String? = null,
    val hasAdvancedData: Boolean = false,
    val isSample: Boolean = false,
    val scanCount: Int = 1,
    val currentScanLabel: String? = null,
    val scanHistory: List<ScanHistoryEntry> = emptyList(),
    val showScanHistory: Boolean = false,
)

data class ScanHistoryEntry(
    val savedCardId: String,
    val scannedDate: String,
    val scannedTime: String,
    val isCurrent: Boolean,
)

data class BalanceItem(
    val name: String?,
    val balance: String,
)

data class InfoItem(
    val title: String?,
    val value: String?,
    val isHeader: Boolean = false,
)

sealed class TransactionItem {
    data class DateHeader(
        val date: String,
    ) : TransactionItem()

    data class SectionHeader(
        val title: String,
    ) : TransactionItem()

    data class TripItem(
        val route: String?,
        val agency: String?,
        val fare: String?,
        val stations: String?,
        val time: String?,
        val mode: Trip.Mode?,
        val hasLocation: Boolean,
        val tripKey: String?,
        val epochSeconds: Long = 0L,
        val isTransfer: Boolean = false,
        val isRejected: Boolean = false,
    ) : TransactionItem()

    data class RefillItem(
        val agency: String?,
        val amount: String,
        val time: String?,
        val epochSeconds: Long = 0L,
    ) : TransactionItem()

    data class SubscriptionItem(
        val name: String?,
        val agency: String?,
        val validRange: String,
        val remainingTrips: String? = null,
        val state: String? = null,
    ) : TransactionItem()
}

data class CardAdvancedUiState(
    val tabs: List<AdvancedTab> = emptyList(),
)

data class AdvancedTab(
    val title: FormattedString,
    val tree: FareBotUiTree,
)
