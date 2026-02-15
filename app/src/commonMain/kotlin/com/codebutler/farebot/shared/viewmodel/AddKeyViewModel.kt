package com.codebutler.farebot.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codebutler.farebot.base.util.ByteUtils
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.persist.CardKeysPersister
import com.codebutler.farebot.persist.db.model.SavedKey
import com.codebutler.farebot.shared.nfc.CardScanner
import com.codebutler.farebot.shared.nfc.ScannedTag
import com.codebutler.farebot.shared.ui.screen.AddKeyUiState
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Inject
class AddKeyViewModel(
    private val keysPersister: CardKeysPersister,
    private val cardScanner: CardScanner? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddKeyUiState(hasNfc = cardScanner != null))
    val uiState: StateFlow<AddKeyUiState> = _uiState.asStateFlow()

    private val _keySaved = MutableSharedFlow<Unit>()
    val keySaved: SharedFlow<Unit> = _keySaved.asSharedFlow()

    private var isObserving = false

    fun startObservingTags() {
        if (isObserving || cardScanner == null) return
        isObserving = true

        viewModelScope.launch {
            cardScanner.scannedTags.collect { tag ->
                onTagDetected(tag)
            }
        }
    }

    fun prefillCardData(
        tagId: String,
        cardType: CardType,
    ) {
        _uiState.value =
            _uiState.value.copy(
                detectedTagId = tagId,
                detectedCardType = cardType,
            )
    }

    fun enterManualMode() {
        _uiState.value =
            _uiState.value.copy(
                detectedTagId = "",
                detectedCardType = CardType.MifareClassic,
            )
    }

    fun importKeyFile(bytes: ByteArray) {
        // Try to interpret as hex-encoded key data
        val hexString =
            try {
                ByteUtils.getHexString(bytes)
            } catch (_: Exception) {
                // If binary, use raw hex
                bytes.joinToString("") {
                    it
                        .toInt()
                        .and(0xFF)
                        .toString(16)
                        .padStart(2, '0')
                }
            }
        _uiState.value = _uiState.value.copy(importedKeyData = hexString)
    }

    fun saveKey(
        cardId: String,
        cardType: CardType,
        keyData: String,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            try {
                keysPersister.insert(
                    SavedKey(
                        cardId = cardId,
                        cardType = cardType,
                        keyData = keyData,
                    ),
                )
                _uiState.value = _uiState.value.copy(isSaving = false)
                _keySaved.emit(Unit)
            } catch (e: Throwable) {
                _uiState.value =
                    _uiState.value.copy(
                        isSaving = false,
                        error = e.message ?: "Failed to save key",
                    )
            }
        }
    }

    private fun onTagDetected(tag: ScannedTag) {
        val tagIdHex =
            tag.id.joinToString("") {
                it
                    .toInt()
                    .and(0xFF)
                    .toString(16)
                    .padStart(2, '0')
                    .uppercase()
            }
        val cardType = detectCardType(tag.techList)

        if (cardType == null) {
            _uiState.value =
                _uiState.value.copy(
                    error = "FareBot does not support keys for this card type.",
                )
            return
        }

        _uiState.value =
            _uiState.value.copy(
                detectedTagId = tagIdHex,
                detectedCardType = cardType,
                error = null,
            )
    }

    private fun detectCardType(techList: List<String>): CardType? =
        when {
            techList.any { it.contains("MifareClassic") } -> CardType.MifareClassic
            techList.any { it.contains("MifareUltralight") } -> CardType.MifareUltralight
            techList.any { it.contains("IsoDep") || it.contains("NfcA") } -> CardType.MifareDesfire
            techList.any { it.contains("NfcF") } -> CardType.FeliCa
            else -> null
        }
}
