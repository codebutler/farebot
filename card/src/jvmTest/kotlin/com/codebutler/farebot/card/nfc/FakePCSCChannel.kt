/*
 * FakePCSCChannel.kt
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

import java.nio.ByteBuffer
import javax.smartcardio.CardChannel
import javax.smartcardio.CommandAPDU
import javax.smartcardio.ResponseAPDU

/**
 * Minimal fake [CardChannel] for unit testing PC/SC technology classes.
 */
class FakePCSCChannel : CardChannel() {
    var lastCommand: CommandAPDU? = null
    var nextResponse: ByteArray = byteArrayOf(0x90.toByte(), 0x00)

    override fun getCard() = throw UnsupportedOperationException()

    override fun getChannelNumber() = 0

    override fun transmit(command: CommandAPDU): ResponseAPDU {
        lastCommand = command
        return ResponseAPDU(nextResponse)
    }

    override fun transmit(
        command: ByteBuffer,
        response: ByteBuffer,
    ): Int {
        throw UnsupportedOperationException()
    }

    override fun close() {}
}
