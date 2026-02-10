package com.codebutler.farebot.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codebutler.farebot.base.ui.HeaderListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.DateFormatStyle
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.base.util.formatDate
import com.codebutler.farebot.base.util.formatTime
import com.codebutler.farebot.card.RawCard
import com.codebutler.farebot.card.serialize.CardSerializer
import com.codebutler.farebot.shared.core.NavDataHolder
import com.codebutler.farebot.shared.platform.Analytics
import com.codebutler.farebot.shared.transit.TransitFactoryRegistry
import com.codebutler.farebot.shared.ui.screen.BalanceItem
import com.codebutler.farebot.shared.ui.screen.CardUiState
import com.codebutler.farebot.shared.ui.screen.InfoItem
import com.codebutler.farebot.shared.ui.screen.TransactionItem
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.UnknownTransitInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Instant

class CardViewModel(
    private val transitFactoryRegistry: TransitFactoryRegistry,
    private val navDataHolder: NavDataHolder,
    private val stringResource: StringResource,
    private val analytics: Analytics,
    private val cardSerializer: CardSerializer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CardUiState())
    val uiState: StateFlow<CardUiState> = _uiState.asStateFlow()

    // Store parsed data for advanced screen navigation
    private var parsedCardKey: String? = null
    private var currentRawCard: RawCard<*>? = null

    fun loadCard(cardKey: String) {
        loadCardInternal(cardKey, isSample = false, sampleTitle = null)
    }

    fun loadSampleCard(cardKey: String, sampleTitle: String) {
        loadCardInternal(cardKey, isSample = true, sampleTitle = sampleTitle)
    }

    private fun loadCardInternal(cardKey: String, isSample: Boolean, sampleTitle: String?) {
        val rawCard = navDataHolder.get<RawCard<*>>(cardKey) ?: return
        currentRawCard = rawCard

        viewModelScope.launch {
            try {
                val card = rawCard.parse()
                val transitInfo = transitFactoryRegistry.parseTransitInfo(card)

                if (transitInfo != null) {
                    if (!isSample) {
                        analytics.logEvent("view_card", mapOf(
                            "card_name" to transitInfo.cardName,
                        ))
                    }
                    val transactions = createTransactionItems(transitInfo)
                    val balances = createBalanceItems(transitInfo)
                    val infoItems = createInfoItems(transitInfo)

                    // Store card + transitInfo for advanced screen
                    parsedCardKey = navDataHolder.put(Pair(card, transitInfo))

                    _uiState.value = CardUiState(
                        isLoading = false,
                        cardName = sampleTitle ?: transitInfo.cardName,
                        serialNumber = transitInfo.serialNumber,
                        balances = balances,
                        transactions = transactions,
                        infoItems = infoItems,
                        warning = transitInfo.warning,
                        hasAdvancedData = true,
                        isSample = isSample,
                    )
                } else {
                    val tagIdHex = card.tagId.joinToString("") {
                        (it.toInt() and 0xFF).toString(16).padStart(2, '0')
                    }.uppercase()
                    val unknownInfo = UnknownTransitInfo(
                        cardTypeName = card.cardType.toString(),
                        tagIdHex = tagIdHex
                    )
                    parsedCardKey = navDataHolder.put(Pair(card, unknownInfo))
                    _uiState.value = CardUiState(
                        isLoading = false,
                        cardName = sampleTitle ?: unknownInfo.cardName,
                        serialNumber = unknownInfo.serialNumber,
                        balances = createBalanceItems(unknownInfo),
                        hasAdvancedData = true,
                        isSample = isSample,
                    )
                }
            } catch (ex: Exception) {
                _uiState.value = CardUiState(
                    isLoading = false,
                    error = ex.message ?: "Unknown error",
                )
            }
        }
    }

    fun getAdvancedCardKey(): String? = parsedCardKey

    fun exportCard(): String? {
        val rawCard = currentRawCard ?: return null
        return cardSerializer.serialize(rawCard)
    }

    fun getTripKey(tripItem: TransactionItem.TripItem): String? {
        return tripItem.tripKey
    }

    private fun createBalanceItems(transitInfo: TransitInfo): List<BalanceItem> {
        val balances = transitInfo.balances ?: return emptyList()
        return balances.map { tb ->
            BalanceItem(
                name = tb.name,
                balance = tb.balance.formatCurrencyString(isBalance = true),
            )
        }
    }

    private fun createInfoItems(transitInfo: TransitInfo): List<InfoItem> {
        val items = transitInfo.info ?: return emptyList()
        return items.map { item ->
            InfoItem(
                title = item.text1,
                value = item.text2,
                isHeader = item is HeaderListItem,
            )
        }
    }

    private fun createTransactionItems(transitInfo: TransitInfo): List<TransactionItem> {
        val subscriptions = transitInfo.subscriptions?.map { sub ->
            TransactionItem.SubscriptionItem(
                name = sub.subscriptionName,
                agency = sub.shortAgencyName,
                validRange = formatSubscriptionRange(sub),
                remainingTrips = sub.remainingTripCount?.let { "$it trips remaining" },
                state = formatSubscriptionState(sub),
            )
        } ?: emptyList()

        val trips = transitInfo.trips?.map { trip ->
            val hasLocation = trip.startStation?.hasLocation() == true ||
                trip.endStation?.hasLocation() == true
            val tripKey = if (hasLocation) navDataHolder.put(trip) else null
            val ts = trip.startTimestamp?.epochSeconds ?: 0L
            val stationsStr = buildStationsString(trip)
            TransactionItem.TripItem(
                route = trip.routeName,
                agency = trip.agencyName,
                fare = trip.fare?.formatCurrencyString() ?: trip.fareString,
                stations = stationsStr,
                time = formatTimestamp(ts),
                mode = trip.mode,
                hasLocation = hasLocation,
                tripKey = tripKey,
                epochSeconds = ts,
                isTransfer = trip.isTransfer,
                isRejected = trip.isRejected,
            )
        } ?: emptyList()

        // Sort trips by time descending
        val sortedTimedItems = trips.sortedByDescending { item ->
            item.epochSeconds
        }

        // Group by calendar day and insert date headers
        val withDateHeaders = mutableListOf<TransactionItem>()
        var lastDateStr: String? = null
        for (item in sortedTimedItems) {
            val epochSec = item.epochSeconds
            if (epochSec > 0L) {
                val dateStr = try {
                    formatDate(Instant.fromEpochSeconds(epochSec), DateFormatStyle.LONG)
                } catch (_: Exception) {
                    null
                }
                if (dateStr != null && dateStr != lastDateStr) {
                    withDateHeaders.add(TransactionItem.DateHeader(dateStr))
                    lastDateStr = dateStr
                }
            }
            withDateHeaders.add(item)
        }

        // Prepend subscriptions with header if any
        val result = mutableListOf<TransactionItem>()
        if (subscriptions.isNotEmpty()) {
            result.add(TransactionItem.SectionHeader("Subscriptions"))
            result.addAll(subscriptions)
        }
        result.addAll(withDateHeaders)
        return result
    }

    private fun formatTimestamp(epochSeconds: Long): String? {
        if (epochSeconds == 0L) return null
        return try {
            formatTime(Instant.fromEpochSeconds(epochSeconds), DateFormatStyle.SHORT)
        } catch (_: Exception) {
            null
        }
    }

    private fun buildStationsString(trip: Trip): String? {
        val start = trip.startStation?.stationName
        val end = trip.endStation?.stationName
        return when {
            start != null && end != null -> "$start \u2192 $end"
            start != null -> start
            end != null -> end
            else -> null
        }
    }

    private fun formatSubscriptionRange(sub: Subscription): String {
        return try {
            val from = sub.validFrom?.let { formatDate(it, DateFormatStyle.SHORT) }
            val to = sub.validTo?.let { formatDate(it, DateFormatStyle.SHORT) }
            "${from ?: "?"} - ${to ?: "?"}"
        } catch (_: Exception) {
            "${sub.validFrom ?: "?"} - ${sub.validTo ?: "?"}"
        }
    }

    private fun formatSubscriptionState(sub: Subscription): String? {
        return when (sub.subscriptionState) {
            Subscription.SubscriptionState.INACTIVE -> "Inactive"
            Subscription.SubscriptionState.UNUSED -> "Unused"
            Subscription.SubscriptionState.STARTED -> "Active"
            Subscription.SubscriptionState.USED -> "Used"
            Subscription.SubscriptionState.EXPIRED -> "Expired"
            else -> null
        }
    }
}
