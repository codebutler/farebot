/*
 * ISO7816CardReaderTest.kt
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for ISO7816CardReader, verifying that unselectFile() is called
 * before file reads and that failures are handled gracefully.
 */
class ISO7816CardReaderTest {
    companion object {
        private const val STATUS_OK = 0x90.toByte()
        private val ERROR_FILE_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x82.toByte())
        private val ERROR_RECORD_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x83.toByte())
        private val ERROR_INS_NOT_SUPPORTED = byteArrayOf(0x6D.toByte(), 0x00.toByte())
        private val ERROR_NO_CURRENT_EF = byteArrayOf(0x69.toByte(), 0x86.toByte())
        private val OK = byteArrayOf(STATUS_OK, 0x00)

        private val SAMPLE_AID = byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x00, 0x04, 0x10, 0x10, 0x01)
    }

    /**
     * A scripted mock transceiver that matches expected APDUs and returns
     * corresponding responses. Tracks all commands for verification.
     */
    private class ScriptedTransceiver : CardTransceiver {
        data class Exchange(
            val description: String,
            val matcher: (ByteArray) -> Boolean,
            val response: ByteArray,
        )

        private val script = mutableListOf<Exchange>()
        val sentCommands = mutableListOf<ByteArray>()

        fun expect(
            description: String,
            matcher: (ByteArray) -> Boolean,
            response: ByteArray,
        ) {
            script.add(Exchange(description, matcher, response))
        }

        override fun transceive(data: ByteArray): ByteArray {
            sentCommands.add(data.copyOf())
            for (i in script.indices) {
                if (script[i].matcher(data)) {
                    return script[i].response
                }
            }
            // Default: return error (no current EF)
            return ERROR_NO_CURRENT_EF
        }

        override val maxTransceiveLength: Int = 256

        override fun connect() {}

        override fun close() {}

        override val isConnected: Boolean = true
    }

    private fun isSelectByName(data: ByteArray): Boolean =
        data.size >= 5 &&
            data[0] == 0x00.toByte() &&
            data[1] == 0xA4.toByte() &&
            data[2] == 0x04.toByte() // P1 = SELECT_BY_NAME

    private fun isUnselectFile(data: ByteArray): Boolean =
        data.size == 5 &&
            data[0] == 0x00.toByte() &&
            data[1] == 0xA4.toByte() &&
            data[2] == 0x00.toByte() &&
            data[3] == 0x00.toByte()
    // No parameters (5 bytes total: CLA INS P1 P2 Le)

    private fun isSelectById(
        data: ByteArray,
        fileId: Int? = null,
    ): Boolean {
        if (data.size != 8) return false
        if (data[0] != 0x00.toByte() || data[1] != 0xA4.toByte()) return false
        if (data[2] != 0x00.toByte() || data[3] != 0x00.toByte()) return false
        if (fileId != null) {
            val high = (fileId shr 8).toByte()
            val low = (fileId and 0xFF).toByte()
            if (data[5] != high || data[6] != low) return false
        }
        return true
    }

    private fun isReadRecord(data: ByteArray): Boolean =
        data.size >= 5 &&
            data[0] == 0x00.toByte() &&
            data[1] == 0xB2.toByte()

    private fun isReadBinary(data: ByteArray): Boolean =
        data.size >= 5 &&
            data[0] == 0x00.toByte() &&
            data[1] == 0xB0.toByte()

    @Test
    fun testReadCardWithFileSelectorCallsUnselectFile() {
        val transceiver = ScriptedTransceiver()

        // Set up responses for a card read with a file selector
        // 1. SELECT BY NAME (application selection)
        transceiver.expect("select by name", ::isSelectByName, "fci".encodeToByteArray() + OK)

        // 2. SFI reads will all fail (no SFI files)
        transceiver.expect("read record (sfi)", ::isReadRecord, ERROR_RECORD_NOT_FOUND)
        transceiver.expect("read binary (sfi)", ::isReadBinary, ERROR_FILE_NOT_FOUND)

        // 3. Unselect file (before file selector read) -- should succeed
        transceiver.expect("unselect file", ::isUnselectFile, OK)

        // 4. SELECT BY ID for the file selector
        transceiver.expect(
            "select by id 0x2001",
            { d -> isSelectById(d, 0x2001) },
            "file_fci".encodeToByteArray() + OK,
        )

        // 5. Read records from selected file
        transceiver.expect("read record (file)", ::isReadRecord, "record_data".encodeToByteArray() + OK)

        val config =
            ISO7816CardReader.AppConfig(
                appNames = listOf(SAMPLE_AID),
                type = "test",
                sfiRange = IntRange.EMPTY, // Skip SFI scanning for simplicity
                fileSelectors =
                    listOf(
                        ISO7816CardReader.FileSelector(fileId = 0x2001),
                    ),
            )

        val result =
            ISO7816CardReader.readCard(
                tagId = byteArrayOf(0x01, 0x02, 0x03, 0x04),
                transceiver = transceiver,
                appConfigs = listOf(config),
            )

        assertNotNull(result, "Card read should succeed")

        // Verify that an unselectFile command was sent
        val unselectCommands = transceiver.sentCommands.filter(::isUnselectFile)
        assertTrue(unselectCommands.isNotEmpty(), "unselectFile should have been called at least once")

        // Verify the ordering: unselectFile must come before selectById(0x2001)
        val unselectIndex = transceiver.sentCommands.indexOfFirst(::isUnselectFile)
        val selectByIdIndex = transceiver.sentCommands.indexOfFirst { isSelectById(it, 0x2001) }
        assertTrue(
            unselectIndex < selectByIdIndex,
            "unselectFile (index=$unselectIndex) should be called before selectById (index=$selectByIdIndex)",
        )
    }

    @Test
    fun testReadCardFileSelectorContinuesWhenUnselectFails() {
        val transceiver = ScriptedTransceiver()

        // Application selection succeeds
        transceiver.expect("select by name", ::isSelectByName, "fci".encodeToByteArray() + OK)

        // Unselect file will FAIL (instruction not supported) -- should be caught
        transceiver.expect("unselect file (fail)", ::isUnselectFile, ERROR_INS_NOT_SUPPORTED)

        // Select by ID should still proceed after unselect failure
        transceiver.expect(
            "select by id 0x2001",
            { d -> isSelectById(d, 0x2001) },
            "file_fci".encodeToByteArray() + OK,
        )

        // Read records from the file -- first record succeeds, then EOF
        var recordCallCount = 0
        val origReadRecord = transceiver.script.find { it.description == "read record (file)" }
        transceiver.expect("read record (file)", ::isReadRecord, ERROR_RECORD_NOT_FOUND)

        // Read binary from the file
        transceiver.expect("read binary (file)", ::isReadBinary, "binary_data".encodeToByteArray() + OK)

        val config =
            ISO7816CardReader.AppConfig(
                appNames = listOf(SAMPLE_AID),
                type = "test",
                sfiRange = IntRange.EMPTY,
                fileSelectors =
                    listOf(
                        ISO7816CardReader.FileSelector(fileId = 0x2001),
                    ),
            )

        val result =
            ISO7816CardReader.readCard(
                tagId = byteArrayOf(0x01, 0x02, 0x03, 0x04),
                transceiver = transceiver,
                appConfigs = listOf(config),
            )

        assertNotNull(result, "Card read should succeed even when unselectFile fails")

        // Verify the card has one application with data
        assertEquals(1, result.applications.size)
        val app = result.applications[0]
        assertEquals("test", app.type)
    }

    @Test
    fun testReadCardWithParentDfFileSelectorCallsUnselectFile() {
        val transceiver = ScriptedTransceiver()

        // Application selection succeeds
        transceiver.expect("select by name", ::isSelectByName, "fci".encodeToByteArray() + OK)

        // When parentDf is set, the reader re-selects the app, selects parentDf, then unselects
        // Re-select app by name (for parentDf state reset)
        // Already matched above

        // Select parent DF by ID
        transceiver.expect(
            "select by id 0x3F00 (parent DF)",
            { d -> isSelectById(d, 0x3F00) },
            OK,
        )

        // Unselect file
        transceiver.expect("unselect file", ::isUnselectFile, OK)

        // Select file by ID
        transceiver.expect(
            "select by id 0x0001 (file)",
            { d -> isSelectById(d, 0x0001) },
            "file_fci".encodeToByteArray() + OK,
        )

        // Read records (none found)
        transceiver.expect("read record", ::isReadRecord, ERROR_RECORD_NOT_FOUND)

        // Read binary
        transceiver.expect("read binary", ::isReadBinary, ERROR_FILE_NOT_FOUND)

        val config =
            ISO7816CardReader.AppConfig(
                appNames = listOf(SAMPLE_AID),
                type = "test",
                sfiRange = IntRange.EMPTY,
                fileSelectors =
                    listOf(
                        ISO7816CardReader.FileSelector(parentDf = 0x3F00, fileId = 0x0001),
                    ),
            )

        val result =
            ISO7816CardReader.readCard(
                tagId = byteArrayOf(0x01, 0x02, 0x03, 0x04),
                transceiver = transceiver,
                appConfigs = listOf(config),
            )

        // This particular case will return null because no files have data,
        // but the important thing is that unselectFile was called
        val unselectCommands = transceiver.sentCommands.filter(::isUnselectFile)
        assertTrue(
            unselectCommands.isNotEmpty(),
            "unselectFile should be called even when parentDf is set",
        )
    }
}
