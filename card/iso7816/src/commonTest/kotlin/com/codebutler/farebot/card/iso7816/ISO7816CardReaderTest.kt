/*
 * ISO7816CardReaderTest.kt
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for ISO7816CardReader, focusing on the unselectFile behavior
 * during file selector reads.
 */
class ISO7816CardReaderTest {
    /**
     * A mock transceiver that records sent APDUs and returns scripted responses.
     */
    private class MockTransceiver : CardTransceiver {
        val sentCommands = mutableListOf<ByteArray>()
        private val responses = mutableListOf<() -> ByteArray>()

        fun enqueueResponse(response: ByteArray) {
            responses.add { response }
        }

        fun enqueueResponse(provider: () -> ByteArray) {
            responses.add(provider)
        }

        override fun transceive(data: ByteArray): ByteArray {
            sentCommands.add(data.copyOf())
            if (responses.isEmpty()) {
                error("No more responses queued, got command: ${data.toHexString()}")
            }
            return responses.removeFirst()()
        }

        override val maxTransceiveLength: Int = 253

        override fun connect() {}

        override fun close() {}

        override val isConnected: Boolean = true
    }

    companion object {
        // Status OK response (SW1=90, SW2=00)
        private val STATUS_OK = byteArrayOf(0x90.toByte(), 0x00)

        // File not found error (SW1=6A, SW2=82)
        private val FILE_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x82.toByte())

        // Instruction not supported error (SW1=6D, SW2=00)
        private val INS_NOT_SUPPORTED = byteArrayOf(0x6D.toByte(), 0x00)

        // Record not found / EOF (SW1=6A, SW2=83)
        private val RECORD_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x83.toByte())

        // Sample AID
        @OptIn(ExperimentalStdlibApi::class)
        private val SAMPLE_AID = "A000000004101001".hexToByteArray()

        // Sample FCI response data + status OK
        private val SAMPLE_FCI = byteArrayOf(0x6F, 0x00) + STATUS_OK

        private fun dataWithStatus(data: ByteArray): ByteArray = data + STATUS_OK
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testReadCardWithFileSelectorsCallsUnselectFile() {
        val transceiver = MockTransceiver()

        // Response for selectByName (AID)
        transceiver.enqueueResponse(SAMPLE_FCI)

        // No SFI records (EOF immediately for each SFI in 0..31)
        // SFI read record attempts: for each SFI, we try record 1
        for (sfi in 0..31) {
            // readRecord for SFI returns EOF
            transceiver.enqueueResponse(RECORD_NOT_FOUND)
            // readBinary for SFI returns file not found
            transceiver.enqueueResponse(FILE_NOT_FOUND)
        }

        // File selector: unselectFile (SELECT with no params) -> STATUS_OK
        transceiver.enqueueResponse(STATUS_OK)
        // File selector: selectById -> FCI + STATUS_OK
        transceiver.enqueueResponse(SAMPLE_FCI)
        // File selector: readRecord(1) -> EOF
        transceiver.enqueueResponse(RECORD_NOT_FOUND)
        // File selector: readBinary -> some data
        transceiver.enqueueResponse(dataWithStatus("hello".encodeToByteArray()))

        val config =
            ISO7816CardReader.AppConfig(
                appNames = listOf(SAMPLE_AID),
                type = "test",
                sfiRange = 0..31,
                fileSelectors =
                    listOf(
                        ISO7816CardReader.FileSelector(fileId = 0x0001),
                    ),
            )

        val result =
            ISO7816CardReader.readCard(
                tagId = byteArrayOf(0x01, 0x02, 0x03, 0x04),
                transceiver = transceiver,
                appConfigs = listOf(config),
            )

        assertNotNull(result)

        // Find the unselectFile APDU in the sent commands.
        // unselectFile sends: CLA=00 INS=A4 P1=00 P2=00 Le=00
        // (5 bytes total with no parameters)
        val unselectCommands =
            transceiver.sentCommands.filter { cmd ->
                cmd.size == 5 &&
                    cmd[0] == 0x00.toByte() &&
                    // CLA
                    cmd[1] == 0xA4.toByte() &&
                    // INS (SELECT)
                    cmd[2] == 0x00.toByte() &&
                    // P1
                    cmd[3] == 0x00.toByte() &&
                    // P2
                    cmd[4] == 0x00.toByte() // Le
            }

        // At least one unselectFile command should have been sent
        assertTrue(unselectCommands.isNotEmpty(), "Expected unselectFile APDU to be sent before file selection")
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testReadCardContinuesWhenUnselectFileFails() {
        val transceiver = MockTransceiver()

        // Response for selectByName (AID)
        transceiver.enqueueResponse(SAMPLE_FCI)

        // No SFI records
        for (sfi in 0..31) {
            transceiver.enqueueResponse(RECORD_NOT_FOUND)
            transceiver.enqueueResponse(FILE_NOT_FOUND)
        }

        // File selector: unselectFile -> INS_NOT_SUPPORTED (card doesn't support it)
        transceiver.enqueueResponse(INS_NOT_SUPPORTED)
        // File selector: selectById -> should still proceed
        transceiver.enqueueResponse(SAMPLE_FCI)
        // File selector: readRecord(1) -> EOF
        transceiver.enqueueResponse(RECORD_NOT_FOUND)
        // File selector: readBinary -> some data
        transceiver.enqueueResponse(dataWithStatus("world".encodeToByteArray()))

        val config =
            ISO7816CardReader.AppConfig(
                appNames = listOf(SAMPLE_AID),
                type = "test",
                sfiRange = 0..31,
                fileSelectors =
                    listOf(
                        ISO7816CardReader.FileSelector(fileId = 0x0002),
                    ),
            )

        val result =
            ISO7816CardReader.readCard(
                tagId = byteArrayOf(0x01, 0x02, 0x03, 0x04),
                transceiver = transceiver,
                appConfigs = listOf(config),
            )

        // Should succeed even though unselectFile threw
        assertNotNull(result)
        val app = result.applications.first()
        val file = app.files["2"]
        assertNotNull(file, "File should have been read despite unselectFile failure")
        assertEquals(
            "world",
            file.binaryData?.decodeToString(),
            "File binary data should be readable",
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testNoApplicationsReturnsNull() {
        val transceiver = MockTransceiver()

        // selectByName fails (file not found)
        transceiver.enqueueResponse(FILE_NOT_FOUND)

        val config =
            ISO7816CardReader.AppConfig(
                appNames = listOf(SAMPLE_AID),
                type = "test",
                sfiRange = 0..0,
                fileSelectors = emptyList(),
            )

        val result =
            ISO7816CardReader.readCard(
                tagId = byteArrayOf(0x01, 0x02, 0x03, 0x04),
                transceiver = transceiver,
                appConfigs = listOf(config),
            )

        assertNull(result)
    }
}
