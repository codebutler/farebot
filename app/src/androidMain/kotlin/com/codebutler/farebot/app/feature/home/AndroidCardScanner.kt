package com.codebutler.farebot.app.feature.home

import com.codebutler.farebot.app.core.nfc.NfcStream
import com.codebutler.farebot.app.core.nfc.TagReaderFactory
import com.codebutler.farebot.base.util.hex
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.key.ClassicCardKeys
import com.codebutler.farebot.card.classic.raw.RawClassicCard
import com.codebutler.farebot.key.CardKeys
import com.codebutler.farebot.persist.CardKeysPersister
import com.codebutler.farebot.shared.nfc.BaseCardScanner
import com.codebutler.farebot.shared.nfc.CardUnauthorizedException
import com.codebutler.farebot.shared.nfc.ScannedTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Android implementation of [BaseCardScanner] that wraps [NfcStream] and [TagReaderFactory].
 *
 * Uses passive scanning via Android NFC foreground dispatch. Tags arrive through
 * [NfcStream] when the Activity has NFC foreground dispatch enabled.
 */
class AndroidCardScanner(
    private val nfcStream: NfcStream,
    private val tagReaderFactory: TagReaderFactory,
    private val cardKeysPersister: CardKeysPersister,
    private val json: Json,
) : BaseCardScanner() {
    override val requiresActiveScan: Boolean get() = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var isObserving = false

    fun startObservingTags() {
        if (isObserving) return
        isObserving = true

        scope.launch {
            nfcStream.observe().collect { tag ->
                val detectedCardType = tag.techList?.let { cardTypeFromTechList(it) }
                emitTag(ScannedTag(id = tag.id, cardType = detectedCardType))

                setScanning(true)
                try {
                    val cardKeys = getCardKeys(tag.id.hex())
                    val rawCard =
                        tagReaderFactory.getTagReader(tag.id, tag, cardKeys).readTag { current, total ->
                            emitProgress(current, total)
                        }
                    if (rawCard.isUnauthorized()) {
                        throw CardUnauthorizedException(rawCard.tagId(), rawCard.cardType())
                    }
                    if (rawCard is RawClassicCard && rawCard.hasUnauthorizedSectors()) {
                        throw CardUnauthorizedException(rawCard.tagId(), rawCard.cardType())
                    }
                    emitCard(rawCard)
                } catch (error: Throwable) {
                    emitError(error)
                } finally {
                    setScanning(false)
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

    private fun cardTypeFromTechList(techList: Array<String>): CardType? =
        when {
            techList.any { "MifareClassic" in it } -> CardType.MifareClassic
            techList.any { "MifareUltralight" in it } -> CardType.MifareUltralight
            techList.any { "IsoDep" in it } -> CardType.MifareDesfire
            techList.any { "NfcF" in it } -> CardType.FeliCa
            techList.any { "NfcB" in it } -> CardType.CEPAS
            techList.any { "NfcV" in it } -> CardType.Vicinity
            else -> null
        }

    private fun getCardKeys(tagId: String): CardKeys? {
        val savedKey = cardKeysPersister.getForTagId(tagId) ?: return null
        return when (savedKey.cardType) {
            CardType.MifareClassic -> json.decodeFromString(ClassicCardKeys.serializer(), savedKey.keyData)
            else -> null
        }
    }
}
