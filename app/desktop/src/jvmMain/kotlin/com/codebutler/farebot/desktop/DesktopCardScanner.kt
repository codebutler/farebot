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

import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.RawCard
import com.codebutler.farebot.card.cepas.CEPASCardReader
import com.codebutler.farebot.card.classic.ClassicCardReader
import com.codebutler.farebot.card.felica.FeliCaReader
import com.codebutler.farebot.card.felica.PCSCFeliCaTagAdapter
import com.codebutler.farebot.card.nfc.PCSCCardInfo
import com.codebutler.farebot.card.nfc.PCSCCardTransceiver
import com.codebutler.farebot.card.nfc.PCSCClassicTechnology
import com.codebutler.farebot.card.nfc.PCSCUltralightTechnology
import com.codebutler.farebot.card.ultralight.UltralightCardReader
import com.codebutler.farebot.shared.nfc.CardScanner
import com.codebutler.farebot.shared.nfc.ISO7816Dispatcher
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
import javax.smartcardio.CardException
import javax.smartcardio.CommandAPDU
import javax.smartcardio.TerminalFactory

/**
 * Desktop NFC card scanner using PC/SC (javax.smartcardio).
 *
 * Polls USB NFC readers for card presence, determines card type from ATR,
 * dispatches to the appropriate shared card reader, then waits for card removal.
 */
class DesktopCardScanner : CardScanner {
    override val requiresActiveScan: Boolean = true

    private val _scannedCards = MutableSharedFlow<RawCard<*>>(extraBufferCapacity = 1)
    override val scannedCards: SharedFlow<RawCard<*>> = _scannedCards.asSharedFlow()

    private val _scanErrors = MutableSharedFlow<Throwable>(extraBufferCapacity = 1)
    override val scanErrors: SharedFlow<Throwable> = _scanErrors.asSharedFlow()

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var scanJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun startActiveScan() {
        if (scanJob?.isActive == true) return
        _isScanning.value = true

        scanJob =
            scope.launch {
                try {
                    scanLoop()
                } catch (e: Exception) {
                    if (isActive) {
                        _scanErrors.tryEmit(e)
                    }
                } finally {
                    _isScanning.value = false
                }
            }
    }

    override fun stopActiveScan() {
        scanJob?.cancel()
        scanJob = null
        _isScanning.value = false
    }

    private fun scanLoop() {
        val factory = TerminalFactory.getDefault()
        val terminals =
            try {
                factory.terminals().list()
            } catch (e: CardException) {
                throw Exception("No PC/SC readers found. Is a USB NFC reader connected?", e)
            }

        if (terminals.isEmpty()) {
            throw Exception("No PC/SC readers found. Is a USB NFC reader connected?")
        }

        val terminal = terminals.first()
        println("[DesktopCardScanner] Using reader: ${terminal.name}")

        while (true) {
            println("[DesktopCardScanner] Waiting for card…")
            terminal.waitForCardPresent(0)

            try {
                val card = terminal.connect("*")
                try {
                    val atr = card.atr.bytes
                    val info = PCSCCardInfo.fromATR(atr)
                    println("[DesktopCardScanner] Card detected: type=${info.cardType}, ATR=${atr.hex()}")

                    val channel = card.basicChannel

                    // Get UID via PC/SC GET DATA: FF CA 00 00 00
                    val uidCommand = CommandAPDU(0xFF, 0xCA, 0x00, 0x00, 0)
                    val uidResponse = channel.transmit(uidCommand)
                    val tagId =
                        if (uidResponse.sW1 == 0x90 && uidResponse.sW2 == 0x00) {
                            uidResponse.data
                        } else {
                            byteArrayOf()
                        }
                    println("[DesktopCardScanner] Tag ID: ${tagId.hex()}")

                    val rawCard = readCard(info, channel, tagId)
                    _scannedCards.tryEmit(rawCard)
                    println("[DesktopCardScanner] Card read successfully")
                } finally {
                    try {
                        card.disconnect(false)
                    } catch (_: Exception) {
                    }
                }
            } catch (e: Exception) {
                println("[DesktopCardScanner] Read error: ${e.message}")
                _scanErrors.tryEmit(e)
            }

            println("[DesktopCardScanner] Waiting for card removal…")
            terminal.waitForCardAbsent(0)
        }
    }

    private fun readCard(
        info: PCSCCardInfo,
        channel: javax.smartcardio.CardChannel,
        tagId: ByteArray,
    ): RawCard<*> =
        when (info.cardType) {
            CardType.MifareDesfire, CardType.ISO7816 -> {
                val transceiver = PCSCCardTransceiver(channel)
                ISO7816Dispatcher.readCard(tagId, transceiver)
            }

            CardType.MifareClassic -> {
                val tech = PCSCClassicTechnology(channel, info)
                ClassicCardReader.readCard(tagId, tech, null)
            }

            CardType.MifareUltralight -> {
                val tech = PCSCUltralightTechnology(channel, info)
                UltralightCardReader.readCard(tagId, tech)
            }

            CardType.FeliCa -> {
                val adapter = PCSCFeliCaTagAdapter(channel)
                FeliCaReader.readTag(tagId, adapter)
            }

            CardType.CEPAS -> {
                val transceiver = PCSCCardTransceiver(channel)
                CEPASCardReader.readCard(tagId, transceiver)
            }

            else -> {
                val transceiver = PCSCCardTransceiver(channel)
                ISO7816Dispatcher.readCard(tagId, transceiver)
            }
        }

    companion object {
        private fun ByteArray.hex(): String = joinToString("") { "%02X".format(it) }
    }
}
