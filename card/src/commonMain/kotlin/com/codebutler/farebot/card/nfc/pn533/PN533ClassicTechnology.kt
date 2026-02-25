/*
 * PN533ClassicTechnology.kt
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

package com.codebutler.farebot.card.nfc.pn533

import co.touchlab.kermit.Logger
import com.codebutler.farebot.card.nfc.ClassicTechnology
import kotlinx.coroutines.delay
import kotlin.time.TimeSource

/**
 * PN533 implementation of [ClassicTechnology] for MIFARE Classic cards.
 *
 * Uses native MIFARE commands via [PN533.inDataExchange]:
 * - AUTH A: 0x60, AUTH B: 0x61  (followed by block, UID bytes, key bytes)
 * - READ: 0x30 (followed by block number)
 *
 * The PN533 handles the MIFARE Classic crypto1 cipher internally
 * after a successful authentication.
 */
private val log = Logger.withTag("PN533ClassicTechnology")

class PN533ClassicTechnology(
    private val pn533: PN533,
    private val tg: Int,
    private val uid: ByteArray,
    private val info: PN533CardInfo,
) : ClassicTechnology {
    private var connected = true

    /** The underlying PN533 instance. Exposed for raw MIFARE operations (key recovery). */
    val rawPn533: PN533 get() = pn533

    /** The card UID bytes. */
    val rawUid: ByteArray get() = uid

    /** UID as UInt (first 4 bytes, big-endian). */
    val uidAsUInt: UInt
        get() {
            val b = if (uid.size >= 4) uid.copyOfRange(0, 4) else uid
            return ((b[0].toUInt() and 0xFFu) shl 24) or
                ((b[1].toUInt() and 0xFFu) shl 16) or
                ((b[2].toUInt() and 0xFFu) shl 8) or
                (b[3].toUInt() and 0xFFu)
        }

    override fun connect() {
        connected = true
    }

    override fun close() {
        connected = false
    }

    override val isConnected: Boolean get() = connected

    override val sectorCount: Int get() = info.classicSectorCount

    override suspend fun authenticateSectorWithKeyA(
        sectorIndex: Int,
        key: ByteArray,
    ): Boolean = authenticate(sectorIndex, key, MIFARE_CMD_AUTH_A)

    override suspend fun authenticateSectorWithKeyB(
        sectorIndex: Int,
        key: ByteArray,
    ): Boolean = authenticate(sectorIndex, key, MIFARE_CMD_AUTH_B)

    override suspend fun readBlock(blockIndex: Int): ByteArray =
        pn533.inDataExchange(
            tg,
            byteArrayOf(MIFARE_CMD_READ, blockIndex.toByte()),
        )

    override fun sectorToBlock(sectorIndex: Int): Int =
        if (sectorIndex < 32) {
            sectorIndex * 4
        } else {
            32 * 4 + (sectorIndex - 32) * 16
        }

    override fun getBlockCountInSector(sectorIndex: Int): Int = if (sectorIndex < 32) 4 else 16

    private suspend fun authenticate(
        sectorIndex: Int,
        key: ByteArray,
        authCommand: Byte,
    ): Boolean {
        val t0 = TimeSource.Monotonic.markNow()
        return try {
            val block = sectorToBlock(sectorIndex)
            val uidBytes = if (uid.size >= 4) uid.copyOfRange(0, 4) else uid
            val data = byteArrayOf(authCommand, block.toByte()) + key + uidBytes
            pn533.inDataExchange(tg, data)
            log.d { "sector=$sectorIndex OK ${t0.elapsedNow()}" }
            true
        } catch (e: PN533Exception) {
            if (e is PN533TransportException) throw e
            val authTime = t0.elapsedNow()
            val t1 = TimeSource.Monotonic.markNow()
            // After failed MIFARE auth, the card enters HALT state and won't
            // respond to subsequent commands, causing slow PN533 timeouts.
            // Cycle the RF field to reset the card, then re-select it.
            reselectCard()
            log.d(e) { "sector=$sectorIndex FAIL auth=$authTime reselect=${t1.elapsedNow()}" }
            false
        }
    }

    private suspend fun reselectCard() {
        try {
            // After failed MIFARE auth, card enters HALT state (ISO 14443-3).
            // HALTed cards only respond to WUPA (ALL_REQ), not REQA (SENS_REQ).
            // Cycling the RF field resets the card to IDLE state so it responds to REQA
            // from InListPassiveTarget.
            pn533.rfFieldOff()
            delay(RF_RESET_DELAY_MS)
            pn533.rfFieldOn()
            delay(RF_RESET_DELAY_MS)
            pn533.inListPassiveTarget(baudRate = PN533.BAUD_RATE_106_ISO14443A, timeoutMs = RESELECT_TIMEOUT_MS)
        } catch (e: PN533Exception) {
            if (e is PN533TransportException) throw e
            // Card may have been removed — caller will handle this
        }
    }

    companion object {
        const val MIFARE_CMD_AUTH_A: Byte = 0x60
        const val MIFARE_CMD_AUTH_B: Byte = 0x61
        const val MIFARE_CMD_READ: Byte = 0x30
        private const val RF_RESET_DELAY_MS = 25L
        private const val RESELECT_TIMEOUT_MS = 500
    }
}
