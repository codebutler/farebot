package com.codebutler.farebot.app.keymanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codebutler.farebot.app.keymanager.ui.KeyRecoveryUiState
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.shared.nfc.CardScanner
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class KeyRecoveryViewModel(
    private val cardScanner: CardScanner?,
) : ViewModel() {
    private val _uiState = MutableStateFlow(KeyRecoveryUiState())
    val uiState: StateFlow<KeyRecoveryUiState> = _uiState.asStateFlow()

    private val _recoveryComplete = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val recoveryComplete: SharedFlow<Unit> = _recoveryComplete.asSharedFlow()

    private var observing = false

    fun init(
        tagId: String,
        cardType: CardType,
    ) {
        _uiState.value = _uiState.value.copy(tagId = tagId, cardType = cardType)
        startObserving()
        startRecovery()
    }

    private fun startObserving() {
        if (observing || cardScanner == null) return
        observing = true

        viewModelScope.launch {
            cardScanner.scannedCards.collect { rawCard ->
                cardScanner.stopActiveScan()
                _uiState.value =
                    _uiState.value.copy(
                        isRecovering = false,
                        success = true,
                        isWaitingForCard = false,
                        readingProgress = null,
                        recoveryProgress = null,
                    )
                _recoveryComplete.tryEmit(Unit)
            }
        }

        viewModelScope.launch {
            cardScanner.scanErrors.collect { error ->
                cardScanner.stopActiveScan()
                _uiState.value =
                    _uiState.value.copy(
                        isRecovering = false,
                        error = error.message ?: "",
                        isWaitingForCard = false,
                        readingProgress = null,
                        recoveryProgress = null,
                    )
            }
        }

        viewModelScope.launch {
            cardScanner.readingProgress.collect { progress ->
                if (progress != null) {
                    _uiState.value =
                        _uiState.value.copy(
                            isWaitingForCard = false,
                            readingProgress = progress,
                        )
                }
            }
        }

        viewModelScope.launch {
            cardScanner.recoveryProgress.collect { recoveryInfo ->
                if (recoveryInfo != null) {
                    _uiState.value =
                        _uiState.value.copy(
                            isWaitingForCard = false,
                            recoveryProgress = recoveryInfo,
                        )
                }
            }
        }
    }

    fun startRecovery() {
        if (cardScanner == null) return
        _uiState.value =
            _uiState.value.copy(
                isRecovering = true,
                isWaitingForCard = true,
                error = null,
                success = false,
                readingProgress = null,
                recoveryProgress = null,
            )
        cardScanner.startRecoveryScan()
    }
}
