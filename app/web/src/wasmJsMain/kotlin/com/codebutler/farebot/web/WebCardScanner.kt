package com.codebutler.farebot.web

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
import com.codebutler.farebot.card.nfc.pn533.PN533UltralightTechnology
import com.codebutler.farebot.card.nfc.pn533.WebUsbPN533Transport
import com.codebutler.farebot.card.ultralight.UltralightCardReader
import com.codebutler.farebot.shared.nfc.CardScanner
import com.codebutler.farebot.shared.nfc.ISO7816Dispatcher
import com.codebutler.farebot.shared.nfc.ScannedTag
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private var scanJob: Job? = null
    private var transport: WebUsbPN533Transport? = null
    private val scope = CoroutineScope(SupervisorJob())

    override fun startActiveScan() {
        if (scanJob?.isActive == true) return
        _isScanning.value = true

        scanJob =
            scope.launch {
                try {
                    val webUsbTransport = WebUsbPN533Transport()
                    transport = webUsbTransport

                    val opened = webUsbTransport.openAsync()
                    if (!opened) {
                        _scanErrors.tryEmit(
                            UnsupportedOperationException(
                                "Could not open USB NFC reader. Make sure:\n" +
                                    "• You're using Chrome, Edge, or Opera\n" +
                                    "• A PN533/SCL3711 USB reader is connected\n" +
                                    "• You selected the device in the browser popup\n\n" +
                                    "Alternatively, import card data from a JSON file.",
                            ),
                        )
                        _isScanning.value = false
                        return@launch
                    }

                    pollLoop(webUsbTransport)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _scanErrors.tryEmit(e)
                } finally {
                    transport?.close()
                    transport = null
                    _isScanning.value = false
                }
            }
    }

    override fun stopActiveScan() {
        scanJob?.cancel()
        scanJob = null
        transport?.close()
        transport = null
        _isScanning.value = false
    }

    private suspend fun pollLoop(transport: WebUsbPN533Transport) {
        val pn533 = PN533(transport)

        // Initialize PN533
        pn533.sendAck()
        val fw = pn533.getFirmwareVersion()
        println("[WebUSB] PN53x firmware: $fw")
        pn533.samConfiguration()
        // Use finite ATR retries on WebUSB. WebUSB's transferIn cannot be
        // cancelled, so InListPassiveTarget must self-resolve within its own
        // timeout rather than relying on client-side abort. With atrRetries=2,
        // the PN533 polls ~2 times (~300ms) then returns NbTg=0.
        pn533.setMaxRetries(atrRetries = 0x02, passiveActivation = 0x02)

        while (true) {
            // Try ISO 14443-A (covers Classic, Ultralight, DESFire)
            var target = pn533.inListPassiveTarget(baudRate = PN533.BAUD_RATE_106_ISO14443A)

            // Try FeliCa (212 kbps) if no Type A found
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
            val cardTypeName =
                when (target) {
                    is PN533.TargetInfo.TypeA -> PN533CardInfo.fromTypeA(target).cardType.name
                    is PN533.TargetInfo.FeliCa -> CardType.FeliCa.name
                }

            _scannedTags.tryEmit(ScannedTag(id = tagId, techList = listOf(cardTypeName)))

            try {
                val rawCard = readTarget(pn533, target)
                _scannedCards.tryEmit(rawCard)
                println("[WebUSB] Card read successfully")
            } catch (e: Exception) {
                println("[WebUSB] Read error: ${e.message}")
                _scanErrors.tryEmit(e)
            }

            // Release target
            try {
                pn533.inRelease(target.tg)
            } catch (_: PN533Exception) {
            }

            // Wait for card removal
            println("[WebUSB] Waiting for card removal...")
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

    private suspend fun readTypeACard(
        pn533: PN533,
        target: PN533.TargetInfo.TypeA,
    ): RawCard<*> {
        val info = PN533CardInfo.fromTypeA(target)
        val tagId = target.uid
        println(
            "[WebUSB] Type A card: type=${info.cardType}, SAK=0x${(target.sak.toInt() and 0xFF).toString(
                16,
            ).padStart(2, '0')}, UID=${tagId.hex()}",
        )

        return when (info.cardType) {
            CardType.MifareDesfire, CardType.ISO7816 -> {
                val transceiver = PN533CardTransceiver(pn533, target.tg)
                ISO7816Dispatcher.readCard(tagId, transceiver)
            }

            CardType.MifareClassic -> {
                val tech = PN533ClassicTechnology(pn533, target.tg, tagId, info)
                ClassicCardReader.readCard(tagId, tech, null)
            }

            CardType.MifareUltralight -> {
                val tech = PN533UltralightTechnology(pn533, target.tg, info)
                UltralightCardReader.readCard(tagId, tech)
            }

            CardType.CEPAS -> {
                val transceiver = PN533CardTransceiver(pn533, target.tg)
                CEPASCardReader.readCard(tagId, transceiver)
            }

            else -> {
                val transceiver = PN533CardTransceiver(pn533, target.tg)
                ISO7816Dispatcher.readCard(tagId, transceiver)
            }
        }
    }

    private suspend fun readFeliCaCard(
        pn533: PN533,
        target: PN533.TargetInfo.FeliCa,
    ): RawCard<*> {
        val tagId = target.idm
        println("[WebUSB] FeliCa card: IDm=${tagId.hex()}")
        val adapter = PN533FeliCaTagAdapter(pn533, tagId)
        return FeliCaReader.readTag(tagId, adapter)
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
                } catch (_: PN533Exception) {
                    null
                }
            if (target == null) break
            try {
                pn533.inRelease(target.tg)
            } catch (_: PN533Exception) {
            }
        }
    }

    companion object {
        private const val POLL_INTERVAL_MS = 250L
        private const val REMOVAL_POLL_INTERVAL_MS = 300L

        private val SENSF_REQ = byteArrayOf(0x00, 0xFF.toByte(), 0xFF.toByte(), 0x01, 0x00)

        private fun ByteArray.hex(): String {
            val chars = "0123456789ABCDEF".toCharArray()
            return buildString(size * 2) {
                for (b in this@hex) {
                    val i = b.toInt() and 0xFF
                    append(chars[i shr 4])
                    append(chars[i and 0x0F])
                }
            }
        }
    }
}
