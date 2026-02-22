/*
 * FlipperRpcClientTest.kt
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

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FlipperRpcClientTest {
    @Test
    fun testFrameMessage() {
        // Verify that a message of N bytes is prefixed with varint(N)
        val data = ByteArray(300) { it.toByte() }
        val framed = FlipperRpcClient.frameMessage(data)
        val (length, bytesRead) = Varint.decode(framed, 0)
        assertEquals(300, length)
        assertEquals(framed.size, bytesRead + 300)
    }

    @Test
    fun testFrameSmallMessage() {
        val data = ByteArray(10) { 0x42 }
        val framed = FlipperRpcClient.frameMessage(data)
        // varint(10) = 0x0A (1 byte), so total = 11
        assertEquals(11, framed.size)
        assertEquals(0x0A.toByte(), framed[0])
    }

    @Test
    fun testConnectSendsPing() =
        runTest {
            val transport = MockTransport()
            val client = FlipperRpcClient(transport)

            // Enqueue a valid ping response (Main envelope with command_id=1, status=OK, ping_response)
            // Main fields: command_id=1 (field 1, varint), command_status=0 (field 2, varint),
            //              has_next=false (field 3, varint=0), system_ping_response (field 6, LEN)
            val pingResponse = buildMainEnvelope(commandId = 1, contentFieldNumber = 6, contentBytes = byteArrayOf())
            transport.enqueueResponse(FlipperRpcClient.frameMessage(pingResponse))

            client.connect()

            assertTrue(transport.isConnected)
            assertTrue(transport.writtenData.isNotEmpty(), "Connect should send a ping request")
        }

    @Test
    fun testBuildMainEnvelope() {
        // Build envelope with command_id=1, empty ping request (field 4)
        val envelope =
            FlipperRpcClient.buildMainEnvelope(
                commandId = 1,
                contentFieldNumber = 4,
                contentBytes = byteArrayOf(),
            )
        // Should start with field 1 (command_id) tag = 0x08, then varint 1
        assertEquals(0x08.toByte(), envelope[0])
        assertEquals(0x01.toByte(), envelope[1])
    }

    @Test
    fun testListDirectory() =
        runTest {
            val transport = MockTransport()
            val client = FlipperRpcClient(transport)

            // Enqueue ping response for connect
            val pingResponse = buildMainEnvelope(commandId = 1, contentFieldNumber = 6, contentBytes = byteArrayOf())
            transport.enqueueResponse(FlipperRpcClient.frameMessage(pingResponse))

            client.connect()

            // Build a StorageListResponse with two files
            val listResponseContent =
                buildStorageListResponseBytes(
                    listOf(
                        TestFileEntry("card.nfc", isDir = false, size = 1024u),
                        TestFileEntry("keys", isDir = true, size = 0u),
                    ),
                )
            val listResponse =
                buildMainEnvelope(
                    commandId = 2,
                    contentFieldNumber = 8, // storage_list_response
                    contentBytes = listResponseContent,
                )
            transport.enqueueResponse(FlipperRpcClient.frameMessage(listResponse))

            val files = client.listDirectory("/ext/nfc")
            assertEquals(2, files.size)
            assertEquals("card.nfc", files[0].name)
            assertEquals(false, files[0].isDirectory)
            assertEquals("keys", files[1].name)
            assertEquals(true, files[1].isDirectory)
        }

    @Test
    fun testReadFile() =
        runTest {
            val transport = MockTransport()
            val client = FlipperRpcClient(transport)

            // Enqueue ping response for connect
            val pingResponse = buildMainEnvelope(commandId = 1, contentFieldNumber = 6, contentBytes = byteArrayOf())
            transport.enqueueResponse(FlipperRpcClient.frameMessage(pingResponse))

            client.connect()

            // Build a StorageReadResponse with file data
            val fileData = "Filetype: Flipper NFC device\n".encodeToByteArray()
            val readResponseContent = buildStorageReadResponseBytes(fileData)
            val readResponse =
                buildMainEnvelope(
                    commandId = 2,
                    contentFieldNumber = 10, // storage_read_response
                    contentBytes = readResponseContent,
                )
            transport.enqueueResponse(FlipperRpcClient.frameMessage(readResponse))

            val data = client.readFile("/ext/nfc/card.nfc")
            assertEquals("Filetype: Flipper NFC device\n", data.decodeToString())
        }

    @Test
    fun testMultiPartReadFile() =
        runTest {
            val transport = MockTransport()
            val client = FlipperRpcClient(transport)

            // Enqueue ping response for connect
            val pingResponse = buildMainEnvelope(commandId = 1, contentFieldNumber = 6, contentBytes = byteArrayOf())
            transport.enqueueResponse(FlipperRpcClient.frameMessage(pingResponse))

            client.connect()

            // Part 1: has_next = true
            val chunk1 = "Hello, ".encodeToByteArray()
            val readResponse1 =
                buildMainEnvelope(
                    commandId = 2,
                    contentFieldNumber = 10,
                    contentBytes = buildStorageReadResponseBytes(chunk1),
                    hasNext = true,
                )
            transport.enqueueResponse(FlipperRpcClient.frameMessage(readResponse1))

            // Part 2: has_next = false (final)
            val chunk2 = "World!".encodeToByteArray()
            val readResponse2 =
                buildMainEnvelope(
                    commandId = 2,
                    contentFieldNumber = 10,
                    contentBytes = buildStorageReadResponseBytes(chunk2),
                    hasNext = false,
                )
            transport.enqueueResponse(FlipperRpcClient.frameMessage(readResponse2))

            val data = client.readFile("/ext/nfc/card.nfc")
            assertEquals("Hello, World!", data.decodeToString())
        }

    // --- Test helpers to build raw protobuf bytes ---

    data class TestFileEntry(
        val name: String,
        val isDir: Boolean,
        val size: UInt,
    )

    companion object {
        /** Build a raw protobuf Main envelope. */
        fun buildMainEnvelope(
            commandId: Int,
            contentFieldNumber: Int,
            contentBytes: ByteArray,
            hasNext: Boolean = false,
            commandStatus: Int = 0,
        ): ByteArray {
            val buf = mutableListOf<Byte>()

            // Field 1: command_id (varint)
            buf.add(0x08.toByte()) // tag = (1 << 3) | 0
            buf.addAll(Varint.encode(commandId).toList())

            // Field 2: command_status (varint) - only if non-zero
            if (commandStatus != 0) {
                buf.add(0x10.toByte()) // tag = (2 << 3) | 0
                buf.addAll(Varint.encode(commandStatus).toList())
            }

            // Field 3: has_next (varint)
            if (hasNext) {
                buf.add(0x18.toByte()) // tag = (3 << 3) | 0
                buf.add(0x01.toByte())
            }

            // Content field (wire type 2 = length-delimited)
            if (contentBytes.isNotEmpty() || contentFieldNumber > 0) {
                val tag = (contentFieldNumber shl 3) or 2
                buf.addAll(Varint.encode(tag).toList())
                buf.addAll(Varint.encode(contentBytes.size).toList())
                buf.addAll(contentBytes.toList())
            }

            return buf.toByteArray()
        }

        /** Build raw protobuf bytes for StorageListResponse (field 1 = repeated StorageFile). */
        fun buildStorageListResponseBytes(files: List<TestFileEntry>): ByteArray {
            val buf = mutableListOf<Byte>()
            for (file in files) {
                val fileBytes = buildStorageFileBytes(file)
                // field 1, wire type 2 (length-delimited)
                buf.add(0x0A.toByte()) // (1 << 3) | 2
                buf.addAll(Varint.encode(fileBytes.size).toList())
                buf.addAll(fileBytes.toList())
            }
            return buf.toByteArray()
        }

        /** Build raw protobuf bytes for a StorageFile message. */
        private fun buildStorageFileBytes(file: TestFileEntry): ByteArray {
            val buf = mutableListOf<Byte>()

            // Field 1: type (varint) - 0=FILE, 1=DIR
            buf.add(0x08.toByte()) // (1 << 3) | 0
            buf.add(if (file.isDir) 0x01.toByte() else 0x00.toByte())

            // Field 2: name (length-delimited string)
            val nameBytes = file.name.encodeToByteArray()
            buf.add(0x12.toByte()) // (2 << 3) | 2
            buf.addAll(Varint.encode(nameBytes.size).toList())
            buf.addAll(nameBytes.toList())

            // Field 3: size (varint)
            if (file.size > 0u) {
                buf.add(0x18.toByte()) // (3 << 3) | 0
                buf.addAll(Varint.encode(file.size.toInt()).toList())
            }

            return buf.toByteArray()
        }

        /** Build raw protobuf bytes for StorageReadResponse (field 1 = StorageFile with data). */
        fun buildStorageReadResponseBytes(data: ByteArray): ByteArray {
            val buf = mutableListOf<Byte>()

            // The StorageReadResponse has field 1 = StorageFile
            // We need a StorageFile with field 4 = data
            val fileBytes = buildStorageFileWithData(data)
            buf.add(0x0A.toByte()) // (1 << 3) | 2
            buf.addAll(Varint.encode(fileBytes.size).toList())
            buf.addAll(fileBytes.toList())

            return buf.toByteArray()
        }

        /** Build a StorageFile with just the data field populated. */
        private fun buildStorageFileWithData(data: ByteArray): ByteArray {
            val buf = mutableListOf<Byte>()
            // Field 4: data (length-delimited bytes)
            buf.add(0x22.toByte()) // (4 << 3) | 2
            buf.addAll(Varint.encode(data.size).toList())
            buf.addAll(data.toList())
            return buf.toByteArray()
        }
    }
}
