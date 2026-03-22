/*
 * DesktopCardScanner.kt
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

package com.codebutler.farebot.desktop

import co.touchlab.kermit.Logger
import com.codebutler.farebot.card.nfc.pn533.PN533
import com.codebutler.farebot.card.nfc.pn533.PN533Device
import com.codebutler.farebot.shared.nfc.BaseCardScanner
import com.codebutler.farebot.shared.plugin.KeyManagerPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Desktop NFC card scanner that coordinates multiple reader backends.
 *
 * Launches one coroutine per [NfcReaderBackend] (PC/SC, PN533, etc.).
 * Each backend runs independently — if one fails to find hardware,
 * the error is logged and the other backends continue scanning.
 * Results from any backend are emitted to the shared [scannedCards] flow.
 */
private val log = Logger.withTag("DesktopCardScanner")

class DesktopCardScanner(
    private val keyManagerPlugin: KeyManagerPlugin? = null,
) : BaseCardScanner() {
    override val requiresActiveScan: Boolean = true
    override val supportsKeyRecovery: Boolean = true

    private var scanJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun startActiveScan() {
        if (scanJob?.isActive == true) return
        setScanning(true)

        scanJob =
            scope.launch {
                try {
                    val backends =
                        try {
                            discoverBackends()
                        } catch (e: Throwable) {
                            // UnsatisfiedLinkError (missing libusb) or other fatal errors
                            // during backend discovery — report to UI instead of silently failing
                            log.e(e) { "Backend discovery failed" }
                            emitError(Exception("NFC reader initialization failed: ${e.message}", e))
                            return@launch
                        }
                    val backendJobs =
                        backends.map { backend ->
                            launch {
                                log.i { "Starting ${backend.name} backend" }
                                try {
                                    backend.scanLoop(
                                        onCardDetected = ::emitTag,
                                        onCardRead = ::emitCard,
                                        onError = ::emitError,
                                        onProgress = ::emitProgress,
                                        onPartialCard = ::emitPartialCard,
                                        onRecoveryProgress = ::emitRecoveryProgress,
                                    )
                                } catch (e: Exception) {
                                    if (isActive) {
                                        log.w(e) { "${backend.name} backend failed" }
                                    }
                                } catch (e: Error) {
                                    // Catch LinkageError / UnsatisfiedLinkError from native libs
                                    log.w(e) { "${backend.name} backend unavailable" }
                                    emitError(Exception("${backend.name} reader unavailable: ${e.message}", e))
                                }
                            }
                        }

                    backendJobs.forEach { it.join() }

                    if (isActive) {
                        emitError(Exception("All NFC reader backends failed. Is a USB NFC reader connected?"))
                    }
                } finally {
                    setScanning(false)
                }
            }
    }

    override fun stopActiveScan() {
        scanJob?.cancel()
        scanJob = null
        resetScanState()
    }

    private suspend fun discoverBackends(): List<NfcReaderBackend> {
        val backends = mutableListOf<NfcReaderBackend>(PcscReaderBackend(keyManagerPlugin, recoveryMode))
        val transports =
            try {
                PN533Device.openAll()
            } catch (e: Throwable) {
                // UnsatisfiedLinkError when libusb is not installed, or other native lib failures.
                // Fall back to PC/SC-only mode rather than failing entirely.
                log.w(e) { "USB device enumeration failed (libusb not available?)" }
                emptyList()
            }
        if (transports.isEmpty()) {
            backends.add(PN533ReaderBackend(keyManagerPlugin, recoveryMode = recoveryMode))
        } else {
            transports.forEachIndexed { index, transport ->
                transport.flush()
                transport.sendAck()
                val probe = PN533(transport)
                val fw = probe.getFirmwareVersion()
                val label = "PN53x #${index + 1}"
                log.i { "$label firmware: $fw" }
                if (fw.version >= 2) {
                    backends.add(PN533ReaderBackend(keyManagerPlugin, transport, recoveryMode))
                } else {
                    backends.add(RCS956ReaderBackend(keyManagerPlugin, transport, label, recoveryMode))
                }
            }
        }
        return backends
    }
}
