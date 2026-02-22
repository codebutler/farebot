package com.codebutler.farebot.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codebutler.farebot.base.util.hex
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.RawCard
import com.codebutler.farebot.card.serialize.CardSerializer
import com.codebutler.farebot.persist.CardPersister
import com.codebutler.farebot.persist.db.model.SavedCard
import com.codebutler.farebot.shared.core.NavDataHolder
import com.codebutler.farebot.shared.nfc.CardScanner
import com.codebutler.farebot.shared.nfc.CardUnauthorizedException
import com.codebutler.farebot.shared.platform.Analytics
import com.codebutler.farebot.shared.platform.NfcStatus
import com.codebutler.farebot.shared.ui.screen.HomeUiState
import dev.zacsweers.metro.Inject
import farebot.app.generated.resources.*
import farebot.app.generated.resources.Res
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

data class ScanError(
    val title: String,
    val message: String,
    val tagIdHex: String? = null,
    val cardType: CardType? = null,
)

@Inject
class HomeViewModel(
    private val cardScanner: CardScanner? = null,
    private val cardPersister: CardPersister,
    private val cardSerializer: CardSerializer,
    private val navDataHolder: NavDataHolder,
    private val analytics: Analytics,
) : ViewModel() {
    private val _uiState =
        MutableStateFlow(
            HomeUiState(
                nfcStatus = if (cardScanner != null) NfcStatus.AVAILABLE else NfcStatus.UNAVAILABLE,
                requiresActiveScan = cardScanner?.requiresActiveScan ?: true,
            ),
        )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _navigateToCard = MutableSharedFlow<String>()
    val navigateToCard: SharedFlow<String> = _navigateToCard.asSharedFlow()

    private val _errorMessage = MutableStateFlow<ScanError?>(null)
    val errorMessage: StateFlow<ScanError?> = _errorMessage.asStateFlow()

    private var isObserving = false

    fun setNfcStatus(status: NfcStatus) {
        _uiState.value = _uiState.value.copy(nfcStatus = status)
    }

    fun startObserving() {
        if (isObserving || cardScanner == null) return
        isObserving = true

        viewModelScope.launch {
            cardScanner.isScanning.collect { scanning ->
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = scanning,
                        isReadingCard = if (!scanning) false else _uiState.value.isReadingCard,
                    )
            }
        }

        viewModelScope.launch {
            cardScanner.scannedTags.collect {
                _uiState.value = _uiState.value.copy(isReadingCard = true)
            }
        }

        viewModelScope.launch {
            cardScanner.scannedCards.collect { rawCard ->
                _uiState.value = _uiState.value.copy(isReadingCard = false)
                processScannedCard(rawCard)
            }
        }

        viewModelScope.launch {
            cardScanner.scanErrors.collect { error ->
                _uiState.value = _uiState.value.copy(isReadingCard = false)
                println("[HomeViewModel] Scan error: ${error::class.simpleName}: ${error.message}")
                error.printStackTrace()
                val scanError = categorizeError(error)
                analytics.logEvent(
                    "scan_card_error",
                    mapOf(
                        "error_type" to error::class.simpleName.orEmpty(),
                        "error_message" to (error.message ?: "Unknown"),
                    ),
                )
                _errorMessage.value = scanError
            }
        }
    }

    fun startActiveScan() {
        cardScanner?.startActiveScan()
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    private suspend fun categorizeError(error: Throwable): ScanError =
        when {
            error is CardUnauthorizedException ->
                ScanError(
                    title = getString(Res.string.locked_card),
                    message = getString(Res.string.keys_required),
                    tagIdHex = error.tagId.hex(),
                    cardType = error.cardType,
                )
            error.message?.contains("Tag was lost", ignoreCase = true) == true ->
                ScanError(
                    title = getString(Res.string.tag_lost),
                    message = getString(Res.string.tag_lost_message),
                )
            else ->
                ScanError(
                    title = getString(Res.string.error),
                    message = error.message ?: getString(Res.string.unknown_error),
                )
        }

    private suspend fun processScannedCard(rawCard: RawCard<*>) {
        try {
            // Stop active scan after reading a card so the user can press Scan again
            cardScanner?.stopActiveScan()

            cardPersister.insertCard(
                SavedCard(
                    type = rawCard.cardType(),
                    serial = rawCard.tagId().hex(),
                    data = cardSerializer.serialize(rawCard),
                ),
            )
            analytics.logEvent(
                "scan_card",
                mapOf(
                    "card_type" to rawCard.cardType().toString(),
                ),
            )
            val key = navDataHolder.put(rawCard)
            _navigateToCard.emit(key)
        } catch (e: Exception) {
            println("[HomeViewModel] Card processing error: ${e::class.simpleName}: ${e.message}")
            e.printStackTrace()
            _errorMessage.value =
                ScanError(
                    title = getString(Res.string.error),
                    message = e.message ?: getString(Res.string.failed_to_process_card),
                )
        }
    }
}
