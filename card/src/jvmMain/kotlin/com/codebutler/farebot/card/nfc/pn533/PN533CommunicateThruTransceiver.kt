/*
 * PN533CommunicateThruTransceiver.kt
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

import com.codebutler.farebot.card.nfc.CardTransceiver

/**
 * CardTransceiver using InCommunicateThru with ISO-DEP (ISO 14443-4) I-block framing.
 *
 * Used for devices like the Sony RC-S956 where InDataExchange (0x40) is not
 * supported. Requires the target to have been activated with RATS
 * (via setParameters auto RATS flag) during InListPassiveTarget.
 *
 * Handles:
 * - I-block wrapping with alternating block numbers
 * - S(WTX) waiting time extension responses
 */
class PN533CommunicateThruTransceiver(
    private val pn533: PN533,
) : CardTransceiver {
    private var blockNumber = 0
    private var connected = true

    override fun connect() {
        connected = true
    }

    override fun close() {
        connected = false
    }

    override val isConnected: Boolean get() = connected

    override fun transceive(data: ByteArray): ByteArray {
        val pcb = (0x02 or blockNumber).toByte()
        val frame = byteArrayOf(pcb) + data
        var response = pn533.inCommunicateThru(frame)

        // Handle S(WTX) requests: card needs more time
        while (response.isNotEmpty() && response[0] == PCB_S_WTX) {
            response = pn533.inCommunicateThru(response.copyOfRange(0, 2))
        }

        if (response.isEmpty()) {
            throw PN533Exception("Empty ISO-DEP response")
        }

        blockNumber = blockNumber xor 1
        return response.copyOfRange(1, response.size)
    }

    override val maxTransceiveLength: Int get() = 261

    companion object {
        private val PCB_S_WTX: Byte = 0xF2.toByte()
    }
}
