/*
 * PCSCUltralightTechnology.kt
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

package com.codebutler.farebot.card.nfc

import javax.smartcardio.CardChannel
import javax.smartcardio.CommandAPDU

/**
 * PC/SC implementation of [UltralightTechnology] for MIFARE Ultralight cards.
 *
 * Uses PC/SC READ BINARY pseudo-APDU: FF B0 00 {page} {length}
 * Reads 4 pages (16 bytes) at a time, matching the Android MifareUltralight behavior.
 */
class PCSCUltralightTechnology(
    private val channel: CardChannel,
    private val info: PCSCCardInfo,
) : UltralightTechnology {
    private var connected = true

    override fun connect() {
        connected = true
    }

    override fun close() {
        connected = false
    }

    override val isConnected: Boolean get() = connected

    override val type: Int get() = info.ultralightType

    override suspend fun readPages(pageOffset: Int): ByteArray {
        // READ BINARY: FF B0 00 {page} 10 (read 16 bytes = 4 pages)
        val command = CommandAPDU(0xFF, 0xB0, 0x00, pageOffset, 16)
        val response = channel.transmit(command)
        if (response.sW1 != 0x90 || response.sW2 != 0x00) {
            throw Exception("Read page $pageOffset failed: SW=%02X%02X".format(response.sW1, response.sW2))
        }
        return response.data
    }

    override suspend fun transceive(data: ByteArray): ByteArray {
        // For raw transceive, we need to send the data as-is through PC/SC
        // This is used for GET_VERSION (0x60) and AUTH_1 (0x1A) commands
        val command = CommandAPDU(0xFF, 0x00, 0x00, 0x00, data)
        val response = channel.transmit(command)
        if (response.sW1 != 0x90 || response.sW2 != 0x00) {
            throw Exception("Transceive failed: SW=%02X%02X".format(response.sW1, response.sW2))
        }
        return response.data
    }
}
