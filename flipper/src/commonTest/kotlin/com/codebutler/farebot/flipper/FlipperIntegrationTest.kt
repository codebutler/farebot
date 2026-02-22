/*
 * FlipperIntegrationTest.kt
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

package com.codebutler.farebot.flipper

import com.codebutler.farebot.flipper.FlipperRpcClientTest.Companion.buildMainEnvelope
import com.codebutler.farebot.flipper.FlipperRpcClientTest.Companion.buildStorageListResponseBytes
import com.codebutler.farebot.flipper.FlipperRpcClientTest.Companion.buildStorageReadResponseBytes
import com.codebutler.farebot.flipper.FlipperRpcClientTest.TestFileEntry
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * End-to-end integration test: connect → list directory → read file → parse content.
 * Tests the full RPC client flow with mock transport, then verifies FlipperKeyDictParser
 * can process the retrieved data.
 */
class FlipperIntegrationTest {
    @Test
    fun testFullFlowConnectListReadFile() =
        runTest {
            val transport = MockTransport()
            val client = FlipperRpcClient(transport)

            // 1. Connect — enqueue ping response
            val pingResponse = buildMainEnvelope(commandId = 1, contentFieldNumber = 6, contentBytes = byteArrayOf())
            transport.enqueueResponse(FlipperRpcClient.frameMessage(pingResponse))
            client.connect()
            assertTrue(transport.isConnected)

            // 2. List directory — enqueue response with 2 NFC files and 1 directory
            val listContent =
                buildStorageListResponseBytes(
                    listOf(
                        TestFileEntry("card.nfc", isDir = false, size = 512u),
                        TestFileEntry("assets", isDir = true, size = 0u),
                        TestFileEntry("backup.nfc", isDir = false, size = 256u),
                    ),
                )
            val listResponse = buildMainEnvelope(commandId = 2, contentFieldNumber = 8, contentBytes = listContent)
            transport.enqueueResponse(FlipperRpcClient.frameMessage(listResponse))

            val entries = client.listDirectory("/ext/nfc")
            assertEquals(3, entries.size)
            assertEquals("card.nfc", entries[0].name)
            assertEquals(false, entries[0].isDirectory)
            assertEquals(512L, entries[0].size)
            assertEquals("assets", entries[1].name)
            assertEquals(true, entries[1].isDirectory)
            assertEquals("backup.nfc", entries[2].name)

            // 3. Read an NFC dump file
            val nfcContent =
                """
                Filetype: Flipper NFC device
                Version: 4
                Device type: Mifare Classic
                UID: 01 02 03 04
                """.trimIndent()
            val fileData = nfcContent.encodeToByteArray()
            val readContent = buildStorageReadResponseBytes(fileData)
            val readResponse = buildMainEnvelope(commandId = 3, contentFieldNumber = 10, contentBytes = readContent)
            transport.enqueueResponse(FlipperRpcClient.frameMessage(readResponse))

            val data = client.readFile("/ext/nfc/card.nfc")
            val content = data.decodeToString()
            assertTrue(content.contains("Filetype: Flipper NFC device"))
            assertTrue(content.contains("Device type: Mifare Classic"))
            assertTrue(content.contains("UID: 01 02 03 04"))
        }

    @Test
    fun testFullFlowConnectReadKeyDictionary() =
        runTest {
            val transport = MockTransport()
            val client = FlipperRpcClient(transport)

            // Connect
            val pingResponse = buildMainEnvelope(commandId = 1, contentFieldNumber = 6, contentBytes = byteArrayOf())
            transport.enqueueResponse(FlipperRpcClient.frameMessage(pingResponse))
            client.connect()

            // Read key dictionary file from Flipper
            val dictContent =
                """
                # Flipper user dictionary
                A0A1A2A3A4A5
                B0B1B2B3B4B5
                # comment
                FFFFFFFFFFFF
                """.trimIndent()
            val dictData = dictContent.encodeToByteArray()
            val readContent = buildStorageReadResponseBytes(dictData)
            val readResponse = buildMainEnvelope(commandId = 2, contentFieldNumber = 10, contentBytes = readContent)
            transport.enqueueResponse(FlipperRpcClient.frameMessage(readResponse))

            val data = client.readFile("/ext/nfc/assets/mf_classic_dict_user.nfc")

            // Parse with FlipperKeyDictParser
            val keys = FlipperKeyDictParser.parse(data.decodeToString())

            assertEquals(3, keys.size)
            // Verify first key: A0 A1 A2 A3 A4 A5
            assertEquals(0xA0.toByte(), keys[0][0])
            assertEquals(0xA5.toByte(), keys[0][5])
            assertEquals(6, keys[0].size)
            // Verify second key: B0 B1 B2 B3 B4 B5
            assertEquals(0xB0.toByte(), keys[1][0])
            assertEquals(0xB5.toByte(), keys[1][5])
            // Verify last key: FF FF FF FF FF FF
            assertTrue(keys[2].all { it == 0xFF.toByte() })
        }

    @Test
    fun testMultiChunkFileRead() =
        runTest {
            val transport = MockTransport()
            val client = FlipperRpcClient(transport)

            // Connect
            val pingResponse = buildMainEnvelope(commandId = 1, contentFieldNumber = 6, contentBytes = byteArrayOf())
            transport.enqueueResponse(FlipperRpcClient.frameMessage(pingResponse))
            client.connect()

            // Simulate reading a large file in two chunks (has_next = true for first chunk)
            val chunk1 = "Filetype: Flipper NFC device\n".encodeToByteArray()
            val chunk2 = "Version: 4\nDevice type: Mifare Classic\n".encodeToByteArray()

            val readResponse1 =
                buildMainEnvelope(
                    commandId = 2,
                    contentFieldNumber = 10,
                    contentBytes = buildStorageReadResponseBytes(chunk1),
                    hasNext = true,
                )
            transport.enqueueResponse(FlipperRpcClient.frameMessage(readResponse1))

            val readResponse2 =
                buildMainEnvelope(
                    commandId = 2,
                    contentFieldNumber = 10,
                    contentBytes = buildStorageReadResponseBytes(chunk2),
                    hasNext = false,
                )
            transport.enqueueResponse(FlipperRpcClient.frameMessage(readResponse2))

            val data = client.readFile("/ext/nfc/card.nfc")
            val content = data.decodeToString()
            assertEquals("Filetype: Flipper NFC device\nVersion: 4\nDevice type: Mifare Classic\n", content)
        }

    @Test
    fun testDisconnectCleansUp() =
        runTest {
            val transport = MockTransport()
            val client = FlipperRpcClient(transport)

            // Connect
            val pingResponse = buildMainEnvelope(commandId = 1, contentFieldNumber = 6, contentBytes = byteArrayOf())
            transport.enqueueResponse(FlipperRpcClient.frameMessage(pingResponse))
            client.connect()
            assertTrue(transport.isConnected)

            // Disconnect via transport
            transport.close()
            assertTrue(!transport.isConnected)
        }
}
