package com.codebutler.farebot.app.feature.home

import com.codebutler.farebot.app.core.nfc.NfcStream
import com.codebutler.farebot.app.core.nfc.TagReaderFactory
import com.codebutler.farebot.base.util.ByteUtils
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.RawCard
import com.codebutler.farebot.card.classic.key.ClassicCardKeys
import com.codebutler.farebot.key.CardKeys
import com.codebutler.farebot.persist.CardKeysPersister
import com.codebutler.farebot.shared.nfc.CardScanner
import com.codebutler.farebot.shared.nfc.CardUnauthorizedException
import com.codebutler.farebot.shared.nfc.ScannedTag
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Android implementation of [CardScanner] that wraps [NfcStream] and [TagReaderFactory].
 *
 * Uses passive scanning via Android NFC foreground dispatch. Tags arrive through
 * [NfcStream] when the Activity has NFC foreground dispatch enabled.
 */
class AndroidCardScanner(
    private val nfcStream: NfcStream,
    private val tagReaderFactory: TagReaderFactory,
    private val cardKeysPersister: CardKeysPersister,
    private val json: Json,
) : CardScanner {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _scannedCards = MutableSharedFlow<RawCard<*>>()
    override val scannedCards: SharedFlow<RawCard<*>> = _scannedCards.asSharedFlow()

    private val _scanErrors = MutableSharedFlow<Throwable>()
    override val scanErrors: SharedFlow<Throwable> = _scanErrors.asSharedFlow()

    private val _scannedTags = MutableSharedFlow<ScannedTag>()
    override val scannedTags: SharedFlow<ScannedTag> = _scannedTags.asSharedFlow()

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var isObserving = false

    fun startObservingTags() {
        if (isObserving) return
        isObserving = true

        scope.launch {
            nfcStream.observe().collect { tag ->
                val techList = tag.techList?.toList() ?: emptyList()
                _scannedTags.emit(ScannedTag(id = tag.id, techList = techList))

                _isScanning.value = true
                try {
                    val cardKeys = getCardKeys(ByteUtils.getHexString(tag.id))
                    val rawCard = tagReaderFactory.getTagReader(tag.id, tag, cardKeys).readTag()
                    if (rawCard.isUnauthorized()) {
                        throw CardUnauthorizedException(rawCard.tagId(), rawCard.cardType())
                    }
                    _scannedCards.emit(rawCard)
                } catch (error: Throwable) {
                    _scanErrors.emit(error)
                } finally {
                    _isScanning.value = false
                }
            }
        }
    }

    override fun startActiveScan() {
        // No-op on Android - uses passive NFC foreground dispatch
    }

    override fun stopActiveScan() {
        // No-op on Android
    }

    private fun getCardKeys(tagId: String): CardKeys? {
        val savedKey = cardKeysPersister.getForTagId(tagId) ?: return null
        return when (savedKey.cardType) {
            CardType.MifareClassic -> json.decodeFromString(ClassicCardKeys.serializer(), savedKey.keyData)
            else -> null
        }
    }

}
