/*
 * IosNfcScanner.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2025 Eric Butler <eric@codebutler.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.codebutler.farebot.shared.nfc

import com.codebutler.farebot.card.RawCard
import com.codebutler.farebot.card.cepas.CEPASCardReader
import com.codebutler.farebot.card.felica.FeliCaReader
import com.codebutler.farebot.card.felica.IosFeliCaTagAdapter
import com.codebutler.farebot.card.nfc.IosCardTransceiver
import com.codebutler.farebot.card.nfc.IosUltralightTechnology
import com.codebutler.farebot.card.nfc.toByteArray
import com.codebutler.farebot.card.ultralight.UltralightCardReader
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.CoreNFC.NFCFeliCaTagProtocol
import platform.CoreNFC.NFCMiFareDESFire
import platform.CoreNFC.NFCMiFareTagProtocol
import platform.CoreNFC.NFCMiFareUltralight
import platform.CoreNFC.NFCPollingISO14443
import platform.CoreNFC.NFCPollingISO18092
import platform.CoreNFC.NFCTagReaderSession
import platform.CoreNFC.NFCTagReaderSessionDelegateProtocol
import platform.Foundation.NSError
import platform.darwin.DISPATCH_TIME_FOREVER
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create
import platform.darwin.dispatch_queue_t
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait

/**
 * iOS NFC card scanner using Core NFC's [NFCTagReaderSession].
 *
 * Discovers NFC tags (DESFire, FeliCa, CEPAS, Ultralight), connects to them,
 * reads the card data using the appropriate tag reader, and returns the raw card.
 */
@OptIn(ExperimentalForeignApi::class)
class IosNfcScanner : CardScanner {
    override val requiresActiveScan: Boolean get() = true

    private var session: NFCTagReaderSession? = null
    private var delegate: ScanDelegate? = null
    private val nfcQueue: dispatch_queue_t = dispatch_queue_create("com.codebutler.farebot.nfc", null)
    private val workerQueue: dispatch_queue_t = dispatch_queue_create("com.codebutler.farebot.nfc.worker", null)

    private val _scannedCards = MutableSharedFlow<RawCard<*>>(extraBufferCapacity = 1)
    override val scannedCards: SharedFlow<RawCard<*>> = _scannedCards.asSharedFlow()

    private val _scanErrors = MutableSharedFlow<Throwable>(extraBufferCapacity = 1)
    override val scanErrors: SharedFlow<Throwable> = _scanErrors.asSharedFlow()

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    override fun startActiveScan() {
        if (!NFCTagReaderSession.readingAvailable) {
            _scanErrors.tryEmit(Exception("NFC Tag reading is not available on this device"))
            return
        }

        // Invalidate any existing session before starting a new one
        session?.invalidateSession()
        session = null
        delegate = null

        val scanDelegate =
            ScanDelegate(
                workerQueue = workerQueue,
                onCardScanned = { rawCard ->
                    delegate = null
                    _scannedCards.tryEmit(rawCard)
                },
                onError = { error ->
                    delegate = null
                    _scanErrors.tryEmit(Exception(error))
                },
                onSessionEnded = {
                    session = null
                    delegate = null
                },
            )
        delegate = scanDelegate

        dispatch_async(dispatch_get_main_queue()) {
            val newSession =
                NFCTagReaderSession(
                    pollingOption = NFCPollingISO14443 or NFCPollingISO18092,
                    delegate = scanDelegate,
                    queue = nfcQueue,
                )
            newSession.alertMessage = "Hold your transit card near the top of your iPhone."
            session = newSession
            newSession.beginSession()
        }
    }

    override fun stopActiveScan() {
        session?.invalidateSession()
        session = null
        delegate = null
    }

    private class ScanDelegate(
        private val workerQueue: dispatch_queue_t,
        private val onCardScanned: (RawCard<*>) -> Unit,
        private val onError: (String) -> Unit,
        private val onSessionEnded: () -> Unit,
    ) : NSObject(),
        NFCTagReaderSessionDelegateProtocol {
        override fun tagReaderSession(
            session: NFCTagReaderSession,
            didDetectTags: List<*>,
        ) {
            val tag =
                didDetectTags.firstOrNull() ?: run {
                    onError("No tags detected")
                    return
                }

            // Dispatch blocking work to a separate queue so the delegate queue
            // remains free to receive connectToTag/sendMiFareCommand completions.
            dispatch_async(workerQueue) {
                // Connect to the tag
                val connectSemaphore = dispatch_semaphore_create(0)
                var connectError: NSError? = null

                session.connectToTag(tag as platform.CoreNFC.NFCTagProtocol) { error: NSError? ->
                    connectError = error
                    dispatch_semaphore_signal(connectSemaphore)
                }

                dispatch_semaphore_wait(connectSemaphore, DISPATCH_TIME_FOREVER)

                connectError?.let {
                    session.invalidateSessionWithErrorMessage("Connection failed: ${it.localizedDescription}")
                    onError("Connection failed: ${it.localizedDescription}")
                    return@dispatch_async
                }

                session.alertMessage = "Reading card… Keep holding."
                try {
                    val rawCard = readTag(tag)
                    session.alertMessage = "Done!"
                    session.invalidateSession()
                    onCardScanned(rawCard)
                } catch (e: Exception) {
                    session.invalidateSessionWithErrorMessage("Read failed: ${e.message}")
                    onError("Read failed: ${e.message ?: "Unknown error"}")
                }
            }
        }

        override fun tagReaderSession(
            session: NFCTagReaderSession,
            didInvalidateWithError: NSError,
        ) {
            onSessionEnded()
            // Session invalidated - this is called when session ends (normally or with error)
            // Error code 200 = user cancelled, which is not an error
            if (didInvalidateWithError.code != 200L) {
                onError(didInvalidateWithError.localizedDescription)
            }
        }

        override fun tagReaderSessionDidBecomeActive(session: NFCTagReaderSession) {
        }

        private fun readTag(tag: Any): RawCard<*> =
            when (tag) {
                is NFCFeliCaTagProtocol -> readFelicaTag(tag)
                is NFCMiFareTagProtocol -> readMiFareTag(tag)
                else -> throw Exception("Unsupported NFC tag type")
            }

        private fun readFelicaTag(tag: NFCFeliCaTagProtocol): RawCard<*> {
            val tagId = tag.currentIDm.toByteArray()
            /*
             * onlyFirst = true is an iOS-specific hack to work around
             * https://github.com/metrodroid/metrodroid/issues/613
             *
             * _NFReaderSession._validateFelicaCommand asserts that you're talking to the exact
             * IDm that the system discovered -- including the upper 4 bits (which indicate the
             * system number).
             *
             * Tell FeliCaReader to only dump the first service.
             *
             * Once iOS fixes this, do an iOS version check instead.
             */
            return FeliCaReader.readTag(tagId, IosFeliCaTagAdapter(tag), onlyFirst = true)
        }

        private fun readMiFareTag(tag: NFCMiFareTagProtocol): RawCard<*> {
            val tagId = tag.identifier.toByteArray()
            return when (tag.mifareFamily) {
                NFCMiFareDESFire -> {
                    val transceiver = IosCardTransceiver(tag)
                    transceiver.connect()
                    try {
                        ISO7816Dispatcher.readCard(tagId, transceiver)
                    } finally {
                        if (transceiver.isConnected) {
                            try {
                                transceiver.close()
                            } catch (_: Exception) {
                            }
                        }
                    }
                }

                NFCMiFareUltralight -> {
                    val tech = IosUltralightTechnology(tag)
                    tech.connect()
                    try {
                        UltralightCardReader.readCard(tagId, tech)
                    } finally {
                        if (tech.isConnected) {
                            try {
                                tech.close()
                            } catch (_: Exception) {
                            }
                        }
                    }
                }

                else -> {
                    // Try CEPAS (ISO-DEP) as fallback for unknown MIFARE types
                    val transceiver = IosCardTransceiver(tag)
                    transceiver.connect()
                    try {
                        CEPASCardReader.readCard(tagId, transceiver)
                    } finally {
                        if (transceiver.isConnected) {
                            try {
                                transceiver.close()
                            } catch (_: Exception) {
                            }
                        }
                    }
                }
            }
        }
    }
}
