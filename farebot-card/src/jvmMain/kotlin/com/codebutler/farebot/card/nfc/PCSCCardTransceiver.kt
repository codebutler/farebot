/*
 * PCSCCardTransceiver.kt
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
import javax.smartcardio.ResponseAPDU

/**
 * PC/SC implementation of [CardTransceiver].
 *
 * Wraps a [javax.smartcardio.CardChannel] to provide APDU transceive for
 * DESFire, CEPAS, and ISO 7816 protocols.
 */
class PCSCCardTransceiver(
    private val channel: CardChannel,
) : CardTransceiver {
    private var connected = true

    override fun connect() {
        // Already connected when CardChannel is obtained
        connected = true
    }

    override fun close() {
        connected = false
        // Card disconnect is handled by the scanner
    }

    override val isConnected: Boolean get() = connected

    override fun transceive(data: ByteArray): ByteArray {
        val command = CommandAPDU(data)
        val response: ResponseAPDU = channel.transmit(command)
        // Return full response including SW1 SW2 for protocols that expect it
        return response.bytes
    }

    override val maxTransceiveLength: Int get() = 261 // Standard APDU max
}
