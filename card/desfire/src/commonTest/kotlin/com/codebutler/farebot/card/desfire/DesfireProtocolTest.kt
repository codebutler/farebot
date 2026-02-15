/*
 * DesfireProtocolTest.kt
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

package com.codebutler.farebot.card.desfire

import com.codebutler.farebot.card.nfc.CardTransceiver
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DesfireProtocolTest {
    private class MockTransceiver(
        private val responses: MutableList<ByteArray>,
    ) : CardTransceiver {
        override fun transceive(data: ByteArray): ByteArray = responses.removeFirst()

        override val maxTransceiveLength: Int = 253

        override fun connect() {}

        override fun close() {}

        override val isConnected: Boolean = true
    }

    @Test
    fun testEmptyResponseThrowsInvalidResponse() {
        val protocol = DesfireProtocol(MockTransceiver(mutableListOf(byteArrayOf())))
        val exception = assertFailsWith<Exception> { protocol.getAppList() }
        assertEquals("Invalid response", exception.message)
    }

    @Test
    fun testSingleByteResponseThrowsInvalidResponse() {
        val protocol = DesfireProtocol(MockTransceiver(mutableListOf(byteArrayOf(0x91.toByte()))))
        val exception = assertFailsWith<Exception> { protocol.getAppList() }
        assertEquals("Invalid response", exception.message)
    }

    @Test
    fun testMissingStatusPrefixThrowsInvalidResponse() {
        val protocol = DesfireProtocol(MockTransceiver(mutableListOf(byteArrayOf(0x00, 0x00))))
        val exception = assertFailsWith<Exception> { protocol.getAppList() }
        assertEquals("Invalid response", exception.message)
    }

    @Test
    fun testOperationOkReturnsPayload() {
        // Response: [0x01, 0x02, 0x03, 0x91, 0x00] -> payload = [0x01, 0x02, 0x03]
        val response = byteArrayOf(0x01, 0x02, 0x03, 0x91.toByte(), 0x00)
        val protocol = DesfireProtocol(MockTransceiver(mutableListOf(response)))
        val result = protocol.getAppList()
        // getAppList interprets 3 bytes as 1 app ID
        assertEquals(1, result.size)
    }

    @Test
    fun testPermissionDeniedThrowsAccessControlException() {
        // Status 0x9D = PERMISSION_DENIED
        val response = byteArrayOf(0x91.toByte(), 0x9D.toByte())
        val protocol = DesfireProtocol(MockTransceiver(mutableListOf(response)))
        val exception = assertFailsWith<DesfireAccessControlException> { protocol.getAppList() }
        assertEquals("Permission denied", exception.message)
    }

    @Test
    fun testAuthenticationErrorThrowsAccessControlException() {
        // Status 0xAE = AUTHENTICATION_ERROR
        val response = byteArrayOf(0x91.toByte(), 0xAE.toByte())
        val protocol = DesfireProtocol(MockTransceiver(mutableListOf(response)))
        val exception = assertFailsWith<DesfireAccessControlException> { protocol.getAppList() }
        assertEquals("Authentication error", exception.message)
    }

    @Test
    fun testAidNotFoundThrowsNotFoundException() {
        // Status 0xA0 = AID_NOT_FOUND
        val response = byteArrayOf(0x91.toByte(), 0xA0.toByte())
        val protocol = DesfireProtocol(MockTransceiver(mutableListOf(response)))
        val exception = assertFailsWith<DesfireNotFoundException> { protocol.getAppList() }
        assertEquals("AID not found", exception.message)
    }

    @Test
    fun testFileNotFoundThrowsNotFoundException() {
        // Status 0xF0 = FILE_NOT_FOUND
        val response = byteArrayOf(0x91.toByte(), 0xF0.toByte())
        val protocol = DesfireProtocol(MockTransceiver(mutableListOf(response)))
        val exception = assertFailsWith<DesfireNotFoundException> { protocol.getFileList() }
        assertEquals("File not found", exception.message)
    }

    @Test
    fun testUnknownStatusCodeThrows() {
        // Status 0xCD = unknown
        val response = byteArrayOf(0x91.toByte(), 0xCD.toByte())
        val protocol = DesfireProtocol(MockTransceiver(mutableListOf(response)))
        val exception = assertFailsWith<Exception> { protocol.getAppList() }
        assertEquals("Unknown status code: cd", exception.message)
    }

    @Test
    fun testAdditionalFrameChaining() {
        // First response: [0x01, 0x02, 0x03, 0x91, 0xAF] -> more data
        // Second response: [0x04, 0x05, 0x06, 0x91, 0x00] -> done
        val responses =
            mutableListOf(
                byteArrayOf(0x01, 0x02, 0x03, 0x91.toByte(), 0xAF.toByte()),
                byteArrayOf(0x04, 0x05, 0x06, 0x91.toByte(), 0x00),
            )
        val protocol = DesfireProtocol(MockTransceiver(responses))
        val result = protocol.getAppList()
        // 6 bytes = 2 app IDs (3 bytes each)
        assertEquals(2, result.size)
    }

    @Test
    fun testReadFileReturnsPayload() {
        // Simulate reading a file: 4 bytes of data + status OK
        val response = byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0x91.toByte(), 0x00)
        val protocol = DesfireProtocol(MockTransceiver(mutableListOf(response)))
        val result = protocol.readFile(0)
        assertContentEquals(byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte()), result)
    }

    @Test
    fun testEmptyPayloadWithOperationOk() {
        // Response with no payload, just status OK
        val response = byteArrayOf(0x91.toByte(), 0x00)
        val protocol = DesfireProtocol(MockTransceiver(mutableListOf(response)))
        val result = protocol.getAppList()
        assertEquals(0, result.size)
    }
}
