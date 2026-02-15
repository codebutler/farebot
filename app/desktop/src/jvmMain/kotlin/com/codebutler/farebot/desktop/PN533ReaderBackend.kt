/*
 * PN533ReaderBackend.kt
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
import com.codebutler.farebot.card.felica.PN533FeliCaTagAdapter
import com.codebutler.farebot.card.nfc.pn533.PN533
import com.codebutler.farebot.card.nfc.pn533.PN533CardInfo
import com.codebutler.farebot.card.nfc.pn533.PN533CardTransceiver
import com.codebutler.farebot.card.nfc.pn533.PN533ClassicTechnology
import com.codebutler.farebot.card.nfc.pn533.PN533Device
import com.codebutler.farebot.card.nfc.pn533.PN533Exception
import com.codebutler.farebot.card.nfc.pn533.PN533UltralightTechnology
import com.codebutler.farebot.card.ultralight.UltralightCardReader
import com.codebutler.farebot.shared.nfc.ISO7816Dispatcher
import com.codebutler.farebot.shared.nfc.ScannedTag

/**
 * PN533 raw USB reader backend.
 *
 * Communicates with PN533-based NFC readers (e.g., SCM SCL3711)
 * directly over USB bulk transfers, bypassing the PC/SC subsystem.
 */
class PN533ReaderBackend : NfcReaderBackend {
    override val name: String = "PN533"

    override fun scanLoop(
        onCardDetected: (ScannedTag) -> Unit,
        onCardRead: (RawCard<*>) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val transport =
            PN533Device.open()
                ?: throw Exception("PN533 device not found")

        transport.flush()
        val pn533 = PN533(transport)
        try {
            initDevice(pn533)
            pollLoop(pn533, onCardDetected, onCardRead, onError)
        } finally {
            pn533.close()
        }
    }

    private fun initDevice(pn533: PN533) {
        val fw = pn533.getFirmwareVersion()
        println("[PN533] Firmware: $fw")

        pn533.samConfiguration()
        pn533.setMaxRetries()
    }

    private fun pollLoop(
        pn533: PN533,
        onCardDetected: (ScannedTag) -> Unit,
        onCardRead: (RawCard<*>) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        while (true) {
            println("[PN533] Polling for cards...")

            // Try ISO 14443-A (106 kbps) first — covers Classic, Ultralight, DESFire
            var target = pn533.inListPassiveTarget(baudRate = PN533.BAUD_RATE_106_ISO14443A)

            // Try FeliCa (212 kbps) if no Type A card found
            if (target == null) {
                target = pn533.inListPassiveTarget(baudRate = PN533.BAUD_RATE_212_FELICA)
            }

            if (target == null) {
                Thread.sleep(POLL_INTERVAL_MS)
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
            onCardDetected(ScannedTag(id = tagId, techList = listOf(cardTypeName)))

            try {
                val rawCard = readTarget(pn533, target)
                onCardRead(rawCard)
                println("[PN533] Card read successfully")
            } catch (e: Exception) {
                println("[PN533] Read error: ${e.message}")
                onError(e)
            }

            // Release target
            try {
                pn533.inRelease(target.tg)
            } catch (_: PN533Exception) {
            }

            // Wait for card removal by polling until no target detected
            println("[PN533] Waiting for card removal...")
            waitForRemoval(pn533)
        }
    }

    private fun readTarget(
        pn533: PN533,
        target: PN533.TargetInfo,
    ): RawCard<*> =
        when (target) {
            is PN533.TargetInfo.TypeA -> readTypeACard(pn533, target)
            is PN533.TargetInfo.FeliCa -> readFeliCaCard(pn533, target)
        }

    private fun readTypeACard(
        pn533: PN533,
        target: PN533.TargetInfo.TypeA,
    ): RawCard<*> {
        val info = PN533CardInfo.fromTypeA(target)
        val tagId = target.uid
        println("[PN533] Type A card: type=${info.cardType}, SAK=0x%02X, UID=${tagId.hex()}".format(target.sak))

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

    private fun readFeliCaCard(
        pn533: PN533,
        target: PN533.TargetInfo.FeliCa,
    ): RawCard<*> {
        val tagId = target.idm
        println("[PN533] FeliCa card: IDm=${tagId.hex()}")
        val adapter = PN533FeliCaTagAdapter(pn533, target.idm)
        return FeliCaReader.readTag(tagId, adapter)
    }

    private fun waitForRemoval(pn533: PN533) {
        while (true) {
            Thread.sleep(REMOVAL_POLL_INTERVAL_MS)
            val target =
                try {
                    pn533.inListPassiveTarget(baudRate = PN533.BAUD_RATE_106_ISO14443A)
                        ?: pn533.inListPassiveTarget(baudRate = PN533.BAUD_RATE_212_FELICA)
                } catch (_: PN533Exception) {
                    null
                }
            if (target == null) {
                break
            }
            // Card still present, release and keep waiting
            try {
                pn533.inRelease(target.tg)
            } catch (_: PN533Exception) {
            }
        }
    }

    companion object {
        private const val POLL_INTERVAL_MS = 250L
        private const val REMOVAL_POLL_INTERVAL_MS = 300L

        private fun ByteArray.hex(): String = joinToString("") { "%02X".format(it) }
    }
}
