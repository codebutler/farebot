/*
 * ISO7816ProtocolTest.kt
 *
 * Copyright 2026 Eric Butler <eric@codebutler.com>
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
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Tests for ISO7816Protocol, including the unselectFile() method.
 */
class ISO7816ProtocolTest {
    /**
     * A mock CardTransceiver that records all sent APDUs and returns
     * preconfigured responses.
     */
    private class MockTransceiver(
        private val responses: MutableList<ByteArray> = mutableListOf(),
    ) : CardTransceiver {
        val sentCommands = mutableListOf<ByteArray>()

        fun enqueueResponse(vararg bytes: Byte) {
            responses.add(bytes.toList().toByteArray())
        }

        fun enqueueOk(data: ByteArray = ByteArray(0)) {
            val response = data + byteArrayOf(STATUS_OK, 0x00)
            responses.add(response)
        }

        fun enqueueError(
            sw1: Byte,
            sw2: Byte,
        ) {
            responses.add(byteArrayOf(sw1, sw2))
        }

        override suspend fun transceive(data: ByteArray): ByteArray {
            sentCommands.add(data.copyOf())
            check(responses.isNotEmpty()) { "No more responses enqueued" }
            return responses.removeFirst()
        }

        override val maxTransceiveLength: Int = 256

        override fun connect() {}

        override fun close() {}

        override val isConnected: Boolean = true

        companion object {
            const val STATUS_OK = 0x90.toByte()
        }
    }

    @Test
    fun testUnselectFileSendsCorrectApdu() =
        runTest {
            val transceiver = MockTransceiver()
            val protocol = ISO7816Protocol(transceiver)

            // Enqueue a success response for the unselectFile call
            transceiver.enqueueOk()

            protocol.unselectFile()

            // Verify exactly one command was sent
            assertEquals(1, transceiver.sentCommands.size)

            val apdu = transceiver.sentCommands[0]

            // unselectFile sends: CLASS_ISO7816(0x00), INS_SELECT(0xA4), P1=0, P2=0, Le=0
            // wrapMessage with no parameters produces: [CLA, INS, P1, P2, Le]
            assertEquals(0x00.toByte(), apdu[0], "CLA should be CLASS_ISO7816 (0x00)")
            assertEquals(0xA4.toByte(), apdu[1], "INS should be INSTRUCTION_ISO7816_SELECT (0xA4)")
            assertEquals(0x00.toByte(), apdu[2], "P1 should be 0x00")
            assertEquals(0x00.toByte(), apdu[3], "P2 should be 0x00")
            assertEquals(0x00.toByte(), apdu[4], "Le should be 0x00")

            // Total APDU length should be 5 bytes (no parameters)
            assertEquals(5, apdu.size, "APDU should be 5 bytes (CLA + INS + P1 + P2 + Le)")
        }

    @Test
    fun testUnselectFileThrowsOnError() =
        runTest {
            val transceiver = MockTransceiver()
            val protocol = ISO7816Protocol(transceiver)

            // Enqueue an error response: file not found
            transceiver.enqueueError(0x6A.toByte(), 0x82.toByte())

            assertFailsWith<ISOFileNotFoundException> {
                protocol.unselectFile()
            }
        }

    @Test
    fun testUnselectFileThrowsOnInstructionNotSupported() =
        runTest {
            val transceiver = MockTransceiver()
            val protocol = ISO7816Protocol(transceiver)

            // Some cards may not support unselect (INS not supported: 6D00)
            transceiver.enqueueError(0x6D.toByte(), 0x00.toByte())

            assertFailsWith<ISOInstructionCodeNotSupported> {
                protocol.unselectFile()
            }
        }

    @Test
    fun testSelectByIdSendsCorrectApdu() =
        runTest {
            val transceiver = MockTransceiver()
            val protocol = ISO7816Protocol(transceiver)

            transceiver.enqueueOk("fci_data".encodeToByteArray())

            val result = protocol.selectById(0x2001)

            assertEquals(1, transceiver.sentCommands.size)
            val apdu = transceiver.sentCommands[0]

            // selectById sends: CLA=0x00, INS=0xA4, P1=0, P2=0, Lc=2, data=[high, low], Le=0
            assertEquals(0x00.toByte(), apdu[0], "CLA")
            assertEquals(0xA4.toByte(), apdu[1], "INS")
            assertEquals(0x00.toByte(), apdu[2], "P1")
            assertEquals(0x00.toByte(), apdu[3], "P2")
            assertEquals(0x02.toByte(), apdu[4], "Lc (parameter length)")
            assertEquals(0x20.toByte(), apdu[5], "File ID high byte")
            assertEquals(0x01.toByte(), apdu[6], "File ID low byte")
            assertEquals(0x00.toByte(), apdu[7], "Le")

            assertContentEquals("fci_data".encodeToByteArray(), result)
        }

    @Test
    fun testUnselectFileApduDiffersFromSelectById() =
        runTest {
            // Verify that unselectFile and selectById produce distinct APDUs.
            // unselectFile: no parameters (5-byte APDU)
            // selectById: has 2-byte file ID parameter (8-byte APDU)
            val transceiver = MockTransceiver()
            val protocol = ISO7816Protocol(transceiver)

            transceiver.enqueueOk() // for unselectFile
            transceiver.enqueueOk() // for selectById

            protocol.unselectFile()
            protocol.selectById(0x0001)

            val unselectApdu = transceiver.sentCommands[0]
            val selectApdu = transceiver.sentCommands[1]

            // They use the same INS but unselectFile has no data
            assertEquals(unselectApdu[1], selectApdu[1], "Both use SELECT instruction")
            assertTrue(unselectApdu.size < selectApdu.size, "unselectFile APDU should be shorter (no file ID)")
            assertEquals(5, unselectApdu.size)
            assertEquals(8, selectApdu.size)
        }
}
