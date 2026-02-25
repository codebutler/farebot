package com.codebutler.farebot.web

import co.touchlab.kermit.Logger
import com.codebutler.farebot.base.util.hex
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.RawCard
import com.codebutler.farebot.card.cepas.CEPASCardReader
import com.codebutler.farebot.card.classic.ClassicCardReader
import com.codebutler.farebot.card.felica.FeliCaReader
import com.codebutler.farebot.card.felica.PN533FeliCaTagAdapter
import com.codebutler.farebot.card.nfc.pn533.PN533
import com.codebutler.farebot.card.nfc.pn533.PN533CardInfo
import com.codebutler.farebot.card.nfc.pn533.PN533CardTransceiver
import com.codebutler.farebot.card.nfc.pn533.PN533ClassicTechnology
import com.codebutler.farebot.card.nfc.pn533.PN533Exception
import com.codebutler.farebot.card.nfc.pn533.PN533TransportException
import com.codebutler.farebot.card.nfc.pn533.PN533UltralightTechnology
import com.codebutler.farebot.card.nfc.pn533.WebUsbPN533Transport
import com.codebutler.farebot.card.ultralight.UltralightCardReader
import com.codebutler.farebot.shared.nfc.BaseCardScanner
import com.codebutler.farebot.shared.nfc.CardUnauthorizedException
import com.codebutler.farebot.shared.nfc.ISO7816Dispatcher
import com.codebutler.farebot.shared.nfc.ScannedTag
import com.codebutler.farebot.shared.plugin.KeyManagerPlugin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Web NFC card scanner using WebUSB with a PN533-based USB NFC reader.
 *
 * WebUSB is supported in Chrome, Edge, and Opera. The user must grant
 * permission to access the USB device through the browser's device picker.
 *
 * Supported readers:
 * - NXP PN533 (VID 04CC:2533)
 * - SCM SCL3711 (VID 04E6:5591)
 * - Sony RC-S380 (VID 054C:02E1)
 *
 * Uses the same card reader pipeline as desktop/Android/iOS. All NFC I/O
 * interfaces are suspend-compatible, allowing WebUSB's async API to be
 * used seamlessly through Kotlin coroutines.
 */
private val log = Logger.withTag("WebCardScanner")

class WebCardScanner(
    private val keyManagerPlugin: KeyManagerPlugin? = null,
) : BaseCardScanner() {
    override val requiresActiveScan: Boolean = true
    override val supportsKeyRecovery: Boolean = true

    private var scanJob: Job? = null
    private var transport: WebUsbPN533Transport? = null
    private val scope = CoroutineScope(SupervisorJob())

    override fun startActiveScan() {
        if (scanJob?.isActive == true) return
        setScanning(true)

        scanJob =
            scope.launch {
                try {
                    val webUsbTransport = WebUsbPN533Transport()
                    transport = webUsbTransport

                    val opened = webUsbTransport.openAsync()
                    if (!opened) {
                        emitError(
                            UnsupportedOperationException(
                                "Could not open USB NFC reader. Make sure:\n" +
                                    "• You're using Chrome, Edge, or Opera\n" +
                                    "• A PN533/SCL3711 USB reader is connected\n" +
                                    "• You selected the device in the browser popup\n\n" +
                                    "Alternatively, import card data from a JSON file.",
                            ),
                        )
                        setScanning(false)
                        return@launch
                    }

                    pollLoop(webUsbTransport)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    emitError(e)
                } finally {
                    transport?.close()
                    transport = null
                    setScanning(false)
                }
            }
    }

    override fun stopActiveScan() {
        scanJob?.cancel()
        scanJob = null
        transport?.close()
        transport = null
        resetScanState()
    }

    private suspend fun pollLoop(transport: WebUsbPN533Transport) {
        val pn533 = PN533(transport)

        pn533.sendAck()
        val fw = pn533.getFirmwareVersion()
        log.i { "PN53x firmware: $fw" }
        pn533.samConfiguration()
        pn533.setMaxRetries(atrRetries = 0x02, passiveActivation = 0x02)

        while (true) {
            var target = pn533.inListPassiveTarget(baudRate = PN533.BAUD_RATE_106_ISO14443A)

            if (target == null) {
                target =
                    pn533.inListPassiveTarget(
                        baudRate = PN533.BAUD_RATE_212_FELICA,
                        initiatorData = SENSF_REQ,
                    )
            }

            if (target == null) {
                delay(POLL_INTERVAL_MS)
                continue
            }

            val tagId =
                when (target) {
                    is PN533.TargetInfo.TypeA -> target.uid
                    is PN533.TargetInfo.FeliCa -> target.idm
                }
            val detectedCardType =
                when (target) {
                    is PN533.TargetInfo.TypeA -> PN533CardInfo.fromTypeA(target).cardType
                    is PN533.TargetInfo.FeliCa -> CardType.FeliCa
                }

            emitTag(ScannedTag(id = tagId, cardType = detectedCardType))

            try {
                val rawCard = readTarget(pn533, target)
                emitCard(rawCard)
                log.i { "Card read successfully" }
            } catch (e: PN533TransportException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Read error" }
                emitError(e)
            }

            try {
                pn533.inRelease(target.tg)
            } catch (e: PN533TransportException) {
                throw e
            } catch (e: PN533Exception) {
                log.d(e) { "inRelease failed (expected)" }
            }

            log.i { "Waiting for card removal..." }
            waitForRemoval(pn533)
        }
    }

    private suspend fun readTarget(
        pn533: PN533,
        target: PN533.TargetInfo,
    ): RawCard<*> =
        when (target) {
            is PN533.TargetInfo.TypeA -> readTypeACard(pn533, target)
            is PN533.TargetInfo.FeliCa -> readFeliCaCard(pn533, target)
        }

    private val onProgress: suspend (Int, Int) -> Unit = { current, total ->
        emitProgress(current, total)
    }

    private suspend fun readTypeACard(
        pn533: PN533,
        target: PN533.TargetInfo.TypeA,
    ): RawCard<*> {
        val info = PN533CardInfo.fromTypeA(target)
        val tagId = target.uid
        log.i {
            "Type A card: type=${info.cardType}, SAK=0x${(target.sak.toInt() and 0xFF).toString(
                16,
            ).padStart(2, '0')}, UID=${tagId.hex()}"
        }

        return when (info.cardType) {
            CardType.MifareDesfire, CardType.ISO7816 -> {
                val transceiver = PN533CardTransceiver(pn533, target.tg)
                ISO7816Dispatcher.readCard(tagId, transceiver, onProgress)
            }

            CardType.MifareClassic -> {
                val tech = PN533ClassicTechnology(pn533, target.tg, tagId, info)
                val tagIdHex = tagId.hex()
                val cardKeys = keyManagerPlugin?.getCardKeysForTag(tagIdHex)
                val globalKeys = keyManagerPlugin?.getGlobalKeys()
                val keyRecovery = if (recoveryMode) keyManagerPlugin?.classicKeyRecovery else null
                val rawCard =
                    ClassicCardReader.readCard(
                        tagId,
                        tech,
                        cardKeys,
                        globalKeys,
                        keyRecovery,
                        onProgress = onProgress,
                        onPartialCard = ::emitPartialCard,
                    )
                rawCard.extractKeys()?.let { keys ->
                    keyManagerPlugin?.saveCardKeys(tagIdHex, keys)
                }
                if (rawCard.hasUnauthorizedSectors()) {
                    throw CardUnauthorizedException(rawCard.tagId(), rawCard.cardType())
                }
                rawCard
            }

            CardType.MifareUltralight -> {
                val tech = PN533UltralightTechnology(pn533, target.tg, info)
                UltralightCardReader.readCard(tagId, tech, onProgress)
            }

            CardType.CEPAS -> {
                val transceiver = PN533CardTransceiver(pn533, target.tg)
                CEPASCardReader.readCard(tagId, transceiver, onProgress)
            }

            else -> {
                val transceiver = PN533CardTransceiver(pn533, target.tg)
                ISO7816Dispatcher.readCard(tagId, transceiver, onProgress)
            }
        }
    }

    private suspend fun readFeliCaCard(
        pn533: PN533,
        target: PN533.TargetInfo.FeliCa,
    ): RawCard<*> {
        val tagId = target.idm
        log.i { "FeliCa card: IDm=${tagId.hex()}" }
        val adapter = PN533FeliCaTagAdapter(pn533, tagId)
        return FeliCaReader.readTag(tagId, adapter, onProgress = onProgress)
    }

    private suspend fun waitForRemoval(pn533: PN533) {
        while (true) {
            delay(REMOVAL_POLL_INTERVAL_MS)
            val target =
                try {
                    pn533.inListPassiveTarget(baudRate = PN533.BAUD_RATE_106_ISO14443A)
                        ?: pn533.inListPassiveTarget(
                            baudRate = PN533.BAUD_RATE_212_FELICA,
                            initiatorData = SENSF_REQ,
                        )
                } catch (e: PN533TransportException) {
                    throw e
                } catch (e: PN533Exception) {
                    log.d(e) { "Poll during removal check failed" }
                    null
                }
            if (target == null) break
            try {
                pn533.inRelease(target.tg)
            } catch (e: PN533TransportException) {
                throw e
            } catch (e: PN533Exception) {
                log.d(e) { "inRelease during removal wait failed" }
            }
        }
    }

    companion object {
        private const val POLL_INTERVAL_MS = 250L
        private const val REMOVAL_POLL_INTERVAL_MS = 300L

        private val SENSF_REQ = byteArrayOf(0x00, 0xFF.toByte(), 0xFF.toByte(), 0x01, 0x00)
    }
}
