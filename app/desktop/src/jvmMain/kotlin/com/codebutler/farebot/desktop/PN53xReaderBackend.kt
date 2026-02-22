/*
 * PN53xReaderBackend.kt
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

import com.codebutler.farebot.base.util.hex
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.RawCard
import com.codebutler.farebot.card.cepas.CEPASCardReader
import com.codebutler.farebot.card.classic.ClassicCardReader
import com.codebutler.farebot.card.felica.FeliCaReader
import com.codebutler.farebot.card.felica.PN533FeliCaTagAdapter
import com.codebutler.farebot.card.nfc.CardTransceiver
import com.codebutler.farebot.card.nfc.pn533.PN533
import com.codebutler.farebot.card.nfc.pn533.PN533CardInfo
import com.codebutler.farebot.card.nfc.pn533.PN533CardTransceiver
import com.codebutler.farebot.card.nfc.pn533.PN533ClassicTechnology
import com.codebutler.farebot.card.nfc.pn533.PN533Device
import com.codebutler.farebot.card.nfc.pn533.PN533Exception
import com.codebutler.farebot.card.nfc.pn533.PN533TransportException
import com.codebutler.farebot.card.nfc.pn533.PN533UltralightTechnology
import com.codebutler.farebot.card.nfc.pn533.Usb4JavaPN533Transport
import com.codebutler.farebot.card.ultralight.UltralightCardReader
import com.codebutler.farebot.shared.nfc.CardUnauthorizedException
import com.codebutler.farebot.shared.nfc.ISO7816Dispatcher
import com.codebutler.farebot.shared.nfc.ScannedTag
import com.codebutler.farebot.shared.plugin.KeyManagerPlugin
import kotlinx.coroutines.delay

/**
 * Abstract base for PN53x-family USB reader backends.
 *
 * Subclasses provide device-specific initialization ([initDevice]).
 * The shared poll loop, card reading, and target release logic lives here.
 */
abstract class PN53xReaderBackend(
    private val preOpenedTransport: Usb4JavaPN533Transport? = null,
    private val keyManagerPlugin: KeyManagerPlugin? = null,
) : NfcReaderBackend {
    protected abstract suspend fun initDevice(pn533: PN533)

    protected open fun createTransceiver(
        pn533: PN533,
        tg: Int,
    ): CardTransceiver = PN533CardTransceiver(pn533, tg)

    override suspend fun scanLoop(
        onCardDetected: (ScannedTag) -> Unit,
        onCardRead: (RawCard<*>) -> Unit,
        onError: (Throwable) -> Unit,
        onProgress: (suspend (current: Int, total: Int) -> Unit)?,
    ) {
        val transport =
            preOpenedTransport
                ?: PN533Device.open()
                ?: throw Exception("PN53x device not found")

        transport.flush()
        val pn533 = PN533(transport)
        try {
            initDevice(pn533)
            pollLoop(pn533, onCardDetected, onCardRead, onError, onProgress)
        } finally {
            pn533.close()
        }
    }

    private suspend fun pollLoop(
        pn533: PN533,
        onCardDetected: (ScannedTag) -> Unit,
        onCardRead: (RawCard<*>) -> Unit,
        onError: (Throwable) -> Unit,
        onProgress: (suspend (current: Int, total: Int) -> Unit)?,
    ) {
        while (true) {
            println("[$name] Polling for cards...")

            // Try ISO 14443-A (106 kbps) first — covers Classic, Ultralight, DESFire
            var target = pn533.inListPassiveTarget(baudRate = PN533.BAUD_RATE_106_ISO14443A)

            // Try FeliCa (212 kbps) if no Type A card found.
            // SENSF_REQ initiator data is required for RC-S956; PN533 generates defaults internally.
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
            onCardDetected(ScannedTag(id = tagId, techList = listOf(cardTypeName)))

            try {
                val rawCard = readTarget(pn533, target, onProgress)
                onCardRead(rawCard)
                println("[$name] Card read successfully")
            } catch (e: PN533TransportException) {
                throw e
            } catch (e: Exception) {
                println("[$name] Read error: ${e.message}")
                onError(e)
            }

            // Release target
            try {
                pn533.inRelease(target.tg)
            } catch (e: PN533TransportException) {
                throw e
            } catch (_: PN533Exception) {
            }

            // Wait for card removal by polling until no target detected
            println("[$name] Waiting for card removal...")
            waitForRemoval(pn533)
        }
    }

    private suspend fun readTarget(
        pn533: PN533,
        target: PN533.TargetInfo,
        onProgress: (suspend (current: Int, total: Int) -> Unit)?,
    ): RawCard<*> =
        when (target) {
            is PN533.TargetInfo.TypeA -> readTypeACard(pn533, target, onProgress)
            is PN533.TargetInfo.FeliCa -> readFeliCaCard(pn533, target, onProgress)
        }

    private suspend fun readTypeACard(
        pn533: PN533,
        target: PN533.TargetInfo.TypeA,
        onProgress: (suspend (current: Int, total: Int) -> Unit)?,
    ): RawCard<*> {
        val info = PN533CardInfo.fromTypeA(target)
        val tagId = target.uid
        println("[$name] Type A card: type=${info.cardType}, SAK=0x%02X, UID=${tagId.hex()}".format(target.sak))

        return when (info.cardType) {
            CardType.MifareDesfire, CardType.ISO7816 -> {
                val transceiver = createTransceiver(pn533, target.tg)
                ISO7816Dispatcher.readCard(tagId, transceiver, onProgress)
            }

            CardType.MifareClassic -> {
                val tech = PN533ClassicTechnology(pn533, target.tg, tagId, info)
                val tagIdHex = tagId.hex()
                val cardKeys = keyManagerPlugin?.getCardKeysForTag(tagIdHex)
                val globalKeys = keyManagerPlugin?.getGlobalKeys()
                // Don't attempt key recovery during initial scan — that happens
                // on the dedicated key recovery screen after user interaction.
                val rawCard =
                    ClassicCardReader.readCard(tagId, tech, cardKeys, globalKeys, onProgress = onProgress)
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
                val transceiver = createTransceiver(pn533, target.tg)
                CEPASCardReader.readCard(tagId, transceiver, onProgress)
            }

            else -> {
                val transceiver = createTransceiver(pn533, target.tg)
                ISO7816Dispatcher.readCard(tagId, transceiver, onProgress)
            }
        }
    }

    private suspend fun readFeliCaCard(
        pn533: PN533,
        target: PN533.TargetInfo.FeliCa,
        onProgress: (suspend (current: Int, total: Int) -> Unit)?,
    ): RawCard<*> {
        val tagId = target.idm
        println("[$name] FeliCa card: IDm=${tagId.hex()}")
        val adapter = PN533FeliCaTagAdapter(pn533, target.idm)
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
                } catch (_: PN533Exception) {
                    null
                }
            if (target == null) {
                break
            }
            // Card still present, release and keep waiting
            try {
                pn533.inRelease(target.tg)
            } catch (e: PN533TransportException) {
                throw e
            } catch (_: PN533Exception) {
            }
        }
    }

    companion object {
        private const val POLL_INTERVAL_MS = 250L
        private const val REMOVAL_POLL_INTERVAL_MS = 300L

        // Default SENSF_REQ for FeliCa polling: length=5, code=0x00 (SENSF_REQ),
        // system code=0xFFFF (wildcard), request code=0x01 (with PMm), time slot=0x00.
        // PN533 generates this internally, but RC-S956 requires it explicitly.
        private val SENSF_REQ = byteArrayOf(0x00, 0xFF.toByte(), 0xFF.toByte(), 0x01, 0x00)
    }
}
