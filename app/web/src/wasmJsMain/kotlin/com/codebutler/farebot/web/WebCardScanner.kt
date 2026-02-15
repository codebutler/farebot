package com.codebutler.farebot.web

import com.codebutler.farebot.card.RawCard
import com.codebutler.farebot.shared.nfc.CardScanner
import com.codebutler.farebot.shared.nfc.ScannedTag
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * No-op card scanner for the web platform.
 *
 * Web NFC (via the Web NFC API) is only available in Chrome on Android
 * and is not yet widely supported. This placeholder implementation
 * reports scanning as unavailable.
 */
class WebCardScanner : CardScanner {
    override val requiresActiveScan: Boolean = true

    private val _scannedTags = MutableSharedFlow<ScannedTag>(extraBufferCapacity = 1)
    override val scannedTags: SharedFlow<ScannedTag> = _scannedTags.asSharedFlow()

    private val _scannedCards = MutableSharedFlow<RawCard<*>>(extraBufferCapacity = 1)
    override val scannedCards: SharedFlow<RawCard<*>> = _scannedCards.asSharedFlow()

    private val _scanErrors = MutableSharedFlow<Throwable>(extraBufferCapacity = 1)
    override val scanErrors: SharedFlow<Throwable> = _scanErrors.asSharedFlow()

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    override fun startActiveScan() {
        _scanErrors.tryEmit(
            UnsupportedOperationException("NFC scanning is not available in the web browser. Import card data from a JSON file instead."),
        )
    }

    override fun stopActiveScan() {
        _isScanning.value = false
    }
}
