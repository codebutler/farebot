/*
 * PCSCVicinityTechnology.kt
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
 * PC/SC implementation of [VicinityTechnology] for ISO 15693 (NFC-V) cards.
 *
 * Uses PC/SC transparent pseudo-APDU (FF 00 00 00) to send raw ISO 15693
 * command frames through the reader's contactless interface. The reader
 * firmware handles ISO 15693 framing, CRC, and RF modulation.
 *
 * Requires a PC/SC reader with ISO 15693 firmware support (e.g., ACR1252U,
 * HID Omnikey 5022/5427, Identiv uTrust 3700F, SpringCard H663).
 * Readers without ISO 15693 support (e.g., ACR122U) will never present
 * NFC-V cards, so this code path won't be reached on incompatible hardware.
 */
class PCSCVicinityTechnology(
    private val channel: CardChannel,
    override val uid: ByteArray,
) : VicinityTechnology {
    private var connected = true

    override fun connect() {
        connected = true
    }

    override fun close() {
        connected = false
    }

    override val isConnected: Boolean get() = connected

    override suspend fun transceive(data: ByteArray): ByteArray {
        // Send raw ISO 15693 command via PC/SC transparent pseudo-APDU:
        // CLA=FF INS=00 P1=00 P2=00 Lc={len} Data={ISO 15693 command}
        val command = CommandAPDU(0xFF, 0x00, 0x00, 0x00, data)
        val response = channel.transmit(command)
        if (response.sW1 != 0x90 || response.sW2 != 0x00) {
            throw Exception(
                "NFC-V transceive failed: SW=%02X%02X".format(response.sW1, response.sW2),
            )
        }
        return response.data
    }
}
