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

import com.codebutler.farebot.card.RawCard
import com.codebutler.farebot.card.nfc.pn533.PN533
import com.codebutler.farebot.card.nfc.pn533.PN533Device
import com.codebutler.farebot.shared.nfc.CardScanner
import com.codebutler.farebot.shared.nfc.ReadingProgress
import com.codebutler.farebot.shared.nfc.ScannedTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
class DesktopCardScanner : CardScanner {
    override val requiresActiveScan: Boolean = true

    private val _scannedTags = MutableSharedFlow<ScannedTag>(extraBufferCapacity = 1)
    override val scannedTags: SharedFlow<ScannedTag> = _scannedTags.asSharedFlow()

    private val _scannedCards = MutableSharedFlow<RawCard<*>>(extraBufferCapacity = 1)
    override val scannedCards: SharedFlow<RawCard<*>> = _scannedCards.asSharedFlow()

    private val _scanErrors = MutableSharedFlow<Throwable>(extraBufferCapacity = 1)
    override val scanErrors: SharedFlow<Throwable> = _scanErrors.asSharedFlow()

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _readingProgress = MutableStateFlow<ReadingProgress?>(null)
    override val readingProgress: StateFlow<ReadingProgress?> = _readingProgress.asStateFlow()

    private var scanJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun startActiveScan() {
        if (scanJob?.isActive == true) return
        _isScanning.value = true

        scanJob =
            scope.launch {
                try {
                    val backends = discoverBackends()
                    val backendJobs =
                        backends.map { backend ->
                            launch {
                                println("[DesktopCardScanner] Starting ${backend.name} backend")
                                try {
                                    backend.scanLoop(
                                        onCardDetected = { tag ->
                                            _scannedTags.tryEmit(tag)
                                        },
                                        onCardRead = { rawCard ->
                                            _readingProgress.value = null
                                            _scannedCards.tryEmit(rawCard)
                                        },
                                        onError = { error ->
                                            _readingProgress.value = null
                                            _scanErrors.tryEmit(error)
                                        },
                                        onProgress = { current, total ->
                                            _readingProgress.value = ReadingProgress(current, total)
                                        },
                                    )
                                } catch (e: Exception) {
                                    if (isActive) {
                                        println("[DesktopCardScanner] ${backend.name} backend failed: ${e.message}")
                                    }
                                } catch (e: Error) {
                                    // Catch LinkageError / UnsatisfiedLinkError from native libs
                                    println("[DesktopCardScanner] ${backend.name} backend unavailable: ${e.message}")
                                }
                            }
                        }

                    backendJobs.forEach { it.join() }

                    // All backends exited — emit error only if none ran successfully
                    if (isActive) {
                        _scanErrors.tryEmit(Exception("All NFC reader backends failed. Is a USB NFC reader connected?"))
                    }
                } finally {
                    _isScanning.value = false
                }
            }
    }

    override fun stopActiveScan() {
        scanJob?.cancel()
        scanJob = null
    }

    private suspend fun discoverBackends(): List<NfcReaderBackend> {
        val backends = mutableListOf<NfcReaderBackend>(PcscReaderBackend())
        val transports =
            try {
                PN533Device.openAll()
            } catch (e: UnsatisfiedLinkError) {
                println("[DesktopCardScanner] libusb not available: ${e.message}")
                emptyList()
            }
        if (transports.isEmpty()) {
            backends.add(PN533ReaderBackend())
        } else {
            transports.forEachIndexed { index, transport ->
                transport.flush()
                transport.sendAck() // RC-S956 needs ACK before first command
                val probe = PN533(transport)
                val fw = probe.getFirmwareVersion()
                val label = "PN53x #${index + 1}"
                println("[DesktopCardScanner] $label firmware: $fw")
                if (fw.version >= 2) {
                    backends.add(PN533ReaderBackend(transport))
                } else {
                    backends.add(RCS956ReaderBackend(transport, label))
                }
            }
        }
        return backends
    }
}
