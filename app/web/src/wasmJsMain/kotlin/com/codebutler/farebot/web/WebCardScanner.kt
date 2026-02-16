package com.codebutler.farebot.web

import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.RawCard
import com.codebutler.farebot.card.nfc.pn533.PN533
import com.codebutler.farebot.card.nfc.pn533.PN533CardInfo
import com.codebutler.farebot.card.nfc.pn533.PN533CommandException
import com.codebutler.farebot.card.nfc.pn533.PN533Exception
import com.codebutler.farebot.card.nfc.pn533.WebUsbPN533Transport
import com.codebutler.farebot.shared.nfc.CardScanner
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
import kotlinx.coroutines.isActive
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
 * Note: Full card DATA reading is not yet supported over WebUSB because
 * the card reader pipeline uses synchronous CardTransceiver.transceive()
 * which cannot be bridged to WebUSB's async API in Kotlin/WasmJs.
 * Card detection and identification works — for full data, export from
 * the desktop/Android app and import as JSON.
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

                    // Initialize PN533
                    webUsbTransport.sendAckAsync()
                    val fwResp = webUsbTransport.sendCommandAsync(PN533.CMD_GET_FIRMWARE_VERSION)
                    if (fwResp.size >= 4) {
                        val ic = (fwResp[0].toInt() and 0xFF).toString(16).uppercase().padStart(2, '0')
                        val major = fwResp[1].toInt() and 0xFF
                        val minor = fwResp[2].toInt() and 0xFF
                        println("[WebUSB] PN53x firmware: IC=0x$ic v$major.$minor")
                    }

                    // SAM configuration: normal mode
                    webUsbTransport.sendCommandAsync(
                        PN533.CMD_SAM_CONFIGURATION,
                        byteArrayOf(PN533.SAM_MODE_NORMAL, 0x00, 0x01),
                    )

                    // Set max retries
                    webUsbTransport.sendCommandAsync(
                        PN533.CMD_RF_CONFIGURATION,
                        byteArrayOf(
                            PN533.RF_CONFIG_MAX_RETRIES,
                            0xFF.toByte(),
                            0x01,
                            0x02,
                        ),
                    )

                    // Poll loop
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
        while (true) {
            // Try ISO 14443-A (covers Classic, Ultralight, DESFire)
            var target = inListPassiveTarget(transport, PN533.BAUD_RATE_106_ISO14443A)

            // Try FeliCa (212 kbps) if no Type A found
            if (target == null) {
                target =
                    inListPassiveTarget(
                        transport,
                        PN533.BAUD_RATE_212_FELICA,
                        SENSF_REQ,
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

            // Full card reading requires async CardTransceiver (not yet available)
            _scanErrors.tryEmit(
                UnsupportedOperationException(
                    "Detected $cardTypeName card (UID: ${tagId.hex()}). " +
                        "Full card reading over WebUSB is in development. " +
                        "For now, use the desktop app to scan cards, then import the JSON file here.",
                ),
            )

            // Release target
            try {
                transport.sendCommandAsync(PN533.CMD_IN_RELEASE, byteArrayOf(target.tg.toByte()))
            } catch (_: PN533Exception) {
            }

            // Wait for card removal
            waitForRemoval(transport)
        }
    }

    private suspend fun inListPassiveTarget(
        transport: WebUsbPN533Transport,
        baudRate: Byte,
        initiatorData: ByteArray = byteArrayOf(),
    ): PN533.TargetInfo? {
        val resp =
            try {
                transport.sendCommandAsync(
                    PN533.CMD_IN_LIST_PASSIVE_TARGET,
                    byteArrayOf(0x01, baudRate) + initiatorData,
                    timeoutMs = PN533.POLL_TIMEOUT_MS,
                )
            } catch (e: PN533CommandException) {
                return null
            } catch (e: PN533Exception) {
                if (e.message?.contains("timed out") == true) return null
                throw e
            }

        if (resp.isEmpty()) return null
        val nbTg = resp[0].toInt() and 0xFF
        if (nbTg == 0) return null

        val tg = resp[1].toInt() and 0xFF
        return when (baudRate) {
            PN533.BAUD_RATE_106_ISO14443A -> parseTypeATarget(resp, 2, tg)
            PN533.BAUD_RATE_212_FELICA, PN533.BAUD_RATE_424_FELICA -> parseFeliCaTarget(resp, 2, tg)
            else -> null
        }
    }

    private fun parseTypeATarget(
        resp: ByteArray,
        offset: Int,
        tg: Int,
    ): PN533.TargetInfo.TypeA {
        var pos = offset
        val atqa = resp.copyOfRange(pos, pos + 2)
        pos += 2
        val sak = resp[pos]
        pos += 1
        val nfcIdLen = resp[pos].toInt() and 0xFF
        pos += 1
        val uid = resp.copyOfRange(pos, pos + nfcIdLen)
        return PN533.TargetInfo.TypeA(tg = tg, atqa = atqa, sak = sak, uid = uid)
    }

    private fun parseFeliCaTarget(
        resp: ByteArray,
        offset: Int,
        tg: Int,
    ): PN533.TargetInfo.FeliCa {
        var pos = offset
        pos += 1 // polResLen
        pos += 1 // response code
        val idm = resp.copyOfRange(pos, pos + 8)
        pos += 8
        val pmm = resp.copyOfRange(pos, pos + 8)
        return PN533.TargetInfo.FeliCa(tg = tg, idm = idm, pmm = pmm)
    }

    private suspend fun waitForRemoval(transport: WebUsbPN533Transport) {
        while (true) {
            delay(REMOVAL_POLL_INTERVAL_MS)
            val target =
                try {
                    inListPassiveTarget(transport, PN533.BAUD_RATE_106_ISO14443A)
                        ?: inListPassiveTarget(transport, PN533.BAUD_RATE_212_FELICA, SENSF_REQ)
                } catch (_: PN533Exception) {
                    null
                }
            if (target == null) break
            try {
                transport.sendCommandAsync(PN533.CMD_IN_RELEASE, byteArrayOf(target.tg.toByte()))
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
