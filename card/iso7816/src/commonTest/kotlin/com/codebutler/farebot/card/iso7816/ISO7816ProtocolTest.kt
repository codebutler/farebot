/*
 * ISO7816ProtocolTest.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2026 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.card.iso7816

import com.codebutler.farebot.card.nfc.CardTransceiver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Tests for ISO7816Protocol, focusing on the unselectFile method.
 */
class ISO7816ProtocolTest {
    private class MockTransceiver(
        private val responseProvider: (ByteArray) -> ByteArray,
    ) : CardTransceiver {
        val sentCommands = mutableListOf<ByteArray>()

        override fun transceive(data: ByteArray): ByteArray {
            sentCommands.add(data.copyOf())
            return responseProvider(data)
        }

        override val maxTransceiveLength: Int = 253

        override fun connect() {}

        override fun close() {}

        override val isConnected: Boolean = true
    }

    @Test
    fun testUnselectFileSendsCorrectApdu() {
        val transceiver = MockTransceiver { byteArrayOf(0x90.toByte(), 0x00) }
        val protocol = ISO7816Protocol(transceiver)

        protocol.unselectFile()

        assertEquals(1, transceiver.sentCommands.size)
        val cmd = transceiver.sentCommands[0]

        // unselectFile should send: CLA=00 INS=A4 P1=00 P2=00 Le=00
        assertEquals(5, cmd.size, "APDU should be 5 bytes (no parameters)")
        assertEquals(0x00.toByte(), cmd[0], "CLA should be 0x00 (ISO7816)")
        assertEquals(0xA4.toByte(), cmd[1], "INS should be 0xA4 (SELECT)")
        assertEquals(0x00.toByte(), cmd[2], "P1 should be 0x00")
        assertEquals(0x00.toByte(), cmd[3], "P2 should be 0x00")
        assertEquals(0x00.toByte(), cmd[4], "Le should be 0x00")
    }

    @Test
    fun testUnselectFileThrowsOnError() {
        // Card returns instruction not supported (6D00)
        val transceiver =
            MockTransceiver {
                byteArrayOf(0x6D.toByte(), 0x00)
            }
        val protocol = ISO7816Protocol(transceiver)

        assertFailsWith<ISOInstructionCodeNotSupported> {
            protocol.unselectFile()
        }
    }

    @Test
    fun testUnselectFileThrowsOnFileNotFound() {
        // Card returns file not found (6A82)
        val transceiver =
            MockTransceiver {
                byteArrayOf(0x6A.toByte(), 0x82.toByte())
            }
        val protocol = ISO7816Protocol(transceiver)

        assertFailsWith<ISOFileNotFoundException> {
            protocol.unselectFile()
        }
    }

    @Test
    fun testSelectByIdSendsFileIdAsParameters() {
        val transceiver =
            MockTransceiver {
                // FCI data + status OK
                byteArrayOf(0x6F, 0x00, 0x90.toByte(), 0x00)
            }
        val protocol = ISO7816Protocol(transceiver)

        protocol.selectById(0x1234)

        assertEquals(1, transceiver.sentCommands.size)
        val cmd = transceiver.sentCommands[0]

        // selectById should send: CLA=00 INS=A4 P1=00 P2=00 Lc=02 [fileId high] [fileId low] Le=00
        assertEquals(0x00.toByte(), cmd[0], "CLA")
        assertEquals(0xA4.toByte(), cmd[1], "INS")
        assertEquals(0x00.toByte(), cmd[2], "P1")
        assertEquals(0x00.toByte(), cmd[3], "P2")
        assertEquals(0x02.toByte(), cmd[4], "Lc (parameter length)")
        assertEquals(0x12.toByte(), cmd[5], "File ID high byte")
        assertEquals(0x34.toByte(), cmd[6], "File ID low byte")
    }
}
