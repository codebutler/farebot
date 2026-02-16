package com.codebutler.farebot.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codebutler.farebot.base.util.formatHumanDate
import com.codebutler.farebot.base.util.formatTimeShort
import com.codebutler.farebot.base.util.hex
import com.codebutler.farebot.card.RawCard
import com.codebutler.farebot.card.serialize.CardSerializer
import com.codebutler.farebot.persist.CardPersister
import com.codebutler.farebot.persist.db.model.SavedCard
import com.codebutler.farebot.shared.core.NavDataHolder
import com.codebutler.farebot.shared.serialize.CardExporter
import com.codebutler.farebot.shared.serialize.CardImporter
import com.codebutler.farebot.shared.serialize.ExportFormat
import com.codebutler.farebot.shared.serialize.ImportResult
import com.codebutler.farebot.shared.transit.TransitFactoryRegistry
import com.codebutler.farebot.shared.ui.screen.HistoryItem
import com.codebutler.farebot.shared.ui.screen.HistoryUiState
import dev.zacsweers.metro.Inject
import farebot.app.generated.resources.Res
import farebot.app.generated.resources.date_today
import farebot.app.generated.resources.date_yesterday
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.getString

@Inject
class HistoryViewModel(
    private val cardPersister: CardPersister,
    private val cardSerializer: CardSerializer,
    private val transitFactoryRegistry: TransitFactoryRegistry,
    private val navDataHolder: NavDataHolder,
    private val json: Json,
    private val versionCode: Int = 1,
    private val versionName: String = "1.0.0",
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _navigateToCard = MutableSharedFlow<String>()
    val navigateToCard: SharedFlow<String> = _navigateToCard.asSharedFlow()

    // Map item IDs to raw cards for navigation
    private val rawCardMap = mutableMapOf<String, RawCard<*>>()

    // Map item IDs to saved cards for deletion
    private val savedCardMap = mutableMapOf<String, SavedCard>()

    // Export/import helpers
    private val cardExporter by lazy {
        CardExporter(cardSerializer, json, versionCode, versionName)
    }
    private val cardImporter by lazy {
        CardImporter(cardSerializer, json)
    }

    fun loadCards() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val todayLabel = getString(Res.string.date_today)
                val yesterdayLabel = getString(Res.string.date_yesterday)
                val savedCards = cardPersister.getCards()

                // Build flat list of all items (one per scan)
                val allItems =
                    savedCards.map { savedCard ->
                        val rawCard = cardSerializer.deserialize(savedCard.data)
                        val id = savedCard.id.toString()
                        rawCardMap[id] = rawCard
                        savedCardMap[id] = savedCard

                        var cardName: String? = null
                        var serial = savedCard.serial
                        var parseError: String? = null
                        var brandColor: Int? = null
                        var keysRequired = false
                        var keyBundle: String? = null
                        var preview = false
                        var serialOnly = false
                        try {
                            val card = rawCard.parse()
                            val cardInfo = transitFactoryRegistry.findCardInfo(card)
                            val identity = transitFactoryRegistry.parseTransitIdentity(card)
                            cardName = identity?.name?.resolveAsync()
                            brandColor = cardInfo?.brandColor
                            keysRequired = cardInfo?.keysRequired ?: false
                            keyBundle = cardInfo?.keyBundle
                            preview = cardInfo?.preview ?: false
                            serialOnly = cardInfo?.serialOnly ?: false
                            if (identity?.serialNumber != null) {
                                serial = identity.serialNumber!!
                            }
                        } catch (ex: Exception) {
                            parseError = ex.message
                        }

                        val scannedDate =
                            try {
                                formatHumanDate(savedCard.scannedAt, todayLabel, yesterdayLabel)
                            } catch (_: Exception) {
                                null
                            }
                        val scannedTime =
                            try {
                                formatTimeShort(savedCard.scannedAt)
                            } catch (_: Exception) {
                                null
                            }

                        HistoryItem(
                            id = id,
                            cardName = cardName,
                            serial = serial,
                            scannedDate = scannedDate,
                            scannedTime = scannedTime,
                            parseError = parseError,
                            brandColor = brandColor,
                            cardType = savedCard.type,
                            keysRequired = keysRequired,
                            keyBundle = keyBundle,
                            preview = preview,
                            serialOnly = serialOnly,
                            scanCount = 1,
                            allScanIds = listOf(id),
                        )
                    }

                // Build deduplicated list (one per unique card, most recent scan first)
                // Group by identity: cardType:serial
                val groupedItems =
                    allItems
                        .groupBy { item ->
                            val savedCard = savedCardMap[item.id]
                            if (savedCard != null) "${savedCard.type.name}:${savedCard.serial}" else item.id
                        }.map { (_, scans) ->
                            // scans are already sorted newest-first (from getCards() order)
                            val newest = scans.first()
                            newest.copy(
                                scanCount = scans.size,
                                allScanIds = scans.map { it.id },
                            )
                        }

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        allItems = allItems,
                        groupedItems = groupedItems,
                    )
            } catch (e: Throwable) {
                _uiState.value =
                    _uiState.value.copy(isLoading = false, allItems = emptyList(), groupedItems = emptyList())
            }
        }
    }

    fun toggleShowAllScans() {
        _uiState.value = _uiState.value.copy(showAllScans = !_uiState.value.showAllScans)
    }

    fun toggleSelection(itemId: String) {
        val current = _uiState.value
        val newSelected =
            if (current.selectedIds.contains(itemId)) {
                current.selectedIds - itemId
            } else {
                current.selectedIds + itemId
            }
        _uiState.value =
            current.copy(
                selectedIds = newSelected,
                isSelectionMode = newSelected.isNotEmpty(),
            )
    }

    fun clearSelection() {
        _uiState.value =
            _uiState.value.copy(
                selectedIds = emptySet(),
                isSelectionMode = false,
            )
    }

    fun deleteSelected() {
        val selectedIds = _uiState.value.selectedIds.toList()
        viewModelScope.launch {
            for (id in selectedIds) {
                // Find the item to get all scan IDs (for grouped view, deletes all scans)
                val item = _uiState.value.items.find { it.id == id }
                val idsToDelete = item?.allScanIds ?: listOf(id)
                for (scanId in idsToDelete) {
                    val savedCard = savedCardMap[scanId] ?: continue
                    cardPersister.deleteCard(savedCard)
                    rawCardMap.remove(scanId)
                    savedCardMap.remove(scanId)
                }
            }
            clearSelection()
            loadCards()
        }
    }

    fun getCardNavKey(itemId: String): String? {
        val rawCard = rawCardMap[itemId] ?: return null
        return navDataHolder.put(rawCard)
    }

    fun getCardScanIds(itemId: String): List<String> {
        val item = _uiState.value.items.find { it.id == itemId }
        return item?.allScanIds ?: listOf(itemId)
    }

    /**
     * Exports all cards to JSON format with metadata.
     * This is the default export format, compatible with Metrodroid.
     */
    fun exportCards(): String = exportCards(ExportFormat.JSON)

    /**
     * Exports all cards to the specified format.
     */
    fun exportCards(format: ExportFormat): String {
        val cards =
            cardPersister.getCards().map { savedCard ->
                cardSerializer.deserialize(savedCard.data)
            }
        return cardExporter.exportCards(cards, format)
    }

    /**
     * Exports selected cards to JSON format with metadata.
     */
    fun exportSelectedCards(): String = exportSelectedCards(ExportFormat.JSON)

    /**
     * Exports selected cards to the specified format.
     */
    fun exportSelectedCards(format: ExportFormat): String {
        val selectedIds = _uiState.value.selectedIds
        val cards =
            selectedIds.mapNotNull { id ->
                rawCardMap[id]
            }
        return cardExporter.exportCards(cards, format)
    }

    /**
     * Exports a single card by ID to the specified format.
     */
    fun exportSingleCard(
        itemId: String,
        format: ExportFormat = ExportFormat.JSON,
    ): String? {
        val card = rawCardMap[itemId] ?: return null
        return cardExporter.exportCard(card, format)
    }

    /**
     * Gets the suggested filename for export.
     */
    fun getExportFilename(format: ExportFormat = ExportFormat.JSON): String = cardExporter.generateBulkFilename(format)

    /**
     * Imports cards from JSON or XML data.
     * Returns the number of cards imported, or -1 on error.
     */
    fun importCards(data: String): Int =
        when (val result = cardImporter.importCards(data)) {
            is ImportResult.Success -> {
                val importedCards =
                    result.cards.map { rawCard ->
                        cardPersister.insertCard(
                            SavedCard(
                                type = rawCard.cardType(),
                                serial = rawCard.tagId().hex(),
                                data = cardSerializer.serialize(rawCard),
                            ),
                        )
                        rawCard
                    }

                // If exactly one card imported, navigate to it
                if (importedCards.size == 1) {
                    val allCards = cardPersister.getCards()
                    val lastCard = allCards.lastOrNull()
                    if (lastCard != null) {
                        val rawCard = cardSerializer.deserialize(lastCard.data)
                        val id = lastCard.id.toString()
                        rawCardMap[id] = rawCard
                        savedCardMap[id] = lastCard
                        val navKey = navDataHolder.put(rawCard)
                        viewModelScope.launch {
                            _navigateToCard.emit(navKey)
                        }
                    }
                }

                importedCards.size
            }
            is ImportResult.Error -> {
                // Return -1 to indicate error
                // Could potentially expose error message through UI state
                -1
            }
        }

    /**
     * Gets a detailed import result including error information.
     */
    fun importCardsDetailed(data: String): ImportResult = cardImporter.importCards(data)
}
