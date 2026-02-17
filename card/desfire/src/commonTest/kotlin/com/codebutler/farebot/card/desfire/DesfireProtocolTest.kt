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
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Tests for DESFire protocol status code handling and authentication.
 */
class DesfireProtocolTest {
    /**
     * A mock [CardTransceiver] that returns pre-configured responses.
     * Supports both single response and multiple response patterns.
     */
    private class MockTransceiver : CardTransceiver {
        private val responses = mutableListOf<ByteArray>()

        constructor(response: ByteArray) {
            responses.add(response)
        }

        constructor(responses: MutableList<ByteArray>) {
            this.responses.addAll(responses)
        }

        override suspend fun transceive(data: ByteArray): ByteArray = responses.removeFirst()

        override val maxTransceiveLength: Int = 253

        override fun connect() {}

        override fun close() {}

        override val isConnected: Boolean = true
    }

    // -- Buffer size guard --

    @Test
    fun testEmptyResponseThrows() =
        runTest {
            val protocol = DesfireProtocol(MockTransceiver(byteArrayOf()))
            assertFails {
                protocol.getFileList()
            }
        }

    @Test
    fun testEmptyResponseThrowsInvalidResponse() =
        runTest {
            val protocol = DesfireProtocol(MockTransceiver(mutableListOf(byteArrayOf())))
            val exception = assertFailsWith<Exception> { protocol.getAppList() }
            assertEquals("Invalid response", exception.message)
        }

    @Test
    fun testSingleByteResponseThrows() =
        runTest {
            val protocol = DesfireProtocol(MockTransceiver(byteArrayOf(0x91.toByte())))
            assertFails {
                protocol.getFileList()
            }
        }

    @Test
    fun testSingleByteResponseThrowsInvalidResponse() =
        runTest {
            val protocol = DesfireProtocol(MockTransceiver(mutableListOf(byteArrayOf(0x91.toByte()))))
            val exception = assertFailsWith<Exception> { protocol.getAppList() }
            assertEquals("Invalid response", exception.message)
        }

    @Test
    fun testMissingStatusPrefixThrowsInvalidResponse() =
        runTest {
            val protocol = DesfireProtocol(MockTransceiver(mutableListOf(byteArrayOf(0x00, 0x00))))
            val exception = assertFailsWith<Exception> { protocol.getAppList() }
            assertEquals("Invalid response", exception.message)
        }

    @Test
    fun testResponseWithWrongMarkerThrowsInvalidResponse() =
        runTest {
            // Response has 2 bytes but the second-to-last is not 0x91
            val response = byteArrayOf(0x90.toByte(), 0x00)
            val protocol = DesfireProtocol(MockTransceiver(response))
            val ex =
                assertFailsWith<Exception> {
                    protocol.getFileList()
                }
            assertEquals(ex.message?.contains("Invalid response"), true)
        }

    // -- Status code: OPERATION_OK --

    @Test
    fun testOperationOkReturnsData() =
        runTest {
            // Response: [data byte 0x42] [0x91] [0x00 = OPERATION_OK]
            val response = byteArrayOf(0x42, 0x91.toByte(), 0x00)
            val protocol = DesfireProtocol(MockTransceiver(response))
            val result = protocol.getFileList()
            // getFileList converts each byte to an int
            assertTrue(result.size == 1)
            assertTrue(result[0] == 0x42)
        }

    @Test
    fun testOperationOkReturnsPayload() =
        runTest {
            // Response: [0x01, 0x02, 0x03, 0x91, 0x00] -> payload = [0x01, 0x02, 0x03]
            val response = byteArrayOf(0x01, 0x02, 0x03, 0x91.toByte(), 0x00)
            val protocol = DesfireProtocol(MockTransceiver(mutableListOf(response)))
            val result = protocol.getAppList()
            // getAppList interprets 3 bytes as 1 app ID
            assertEquals(1, result.size)
        }

    @Test
    fun testMinimalOkResponseReturnsEmptyData() =
        runTest {
            // Response: [0x91] [0x00 = OPERATION_OK] -- no data bytes before status
            val response = byteArrayOf(0x91.toByte(), 0x00)
            val protocol = DesfireProtocol(MockTransceiver(response))
            val result = protocol.getFileList()
            assertTrue(result.isEmpty())
        }

    @Test
    fun testEmptyPayloadWithOperationOk() =
        runTest {
            // Response with no payload, just status OK
            val response = byteArrayOf(0x91.toByte(), 0x00)
            val protocol = DesfireProtocol(MockTransceiver(mutableListOf(response)))
            val result = protocol.getAppList()
            assertEquals(0, result.size)
        }

    // -- Status code: PERMISSION_DENIED (0x9D) --

    @Test
    fun testPermissionDeniedThrowsAccessControlException() =
        runTest {
            // Status 0x9D = PERMISSION_DENIED
            val response = byteArrayOf(0x91.toByte(), 0x9D.toByte())
            val protocol = DesfireProtocol(MockTransceiver(response))
            val exception = assertFailsWith<DesfireAccessControlException> { protocol.getAppList() }
            assertEquals("Permission denied", exception.message)
        }

    @Test
    fun testPermissionDeniedThrowsDesfireAccessControlException() =
        runTest {
            // Response: [0x91] [0x9D = PERMISSION_DENIED]
            val response = byteArrayOf(0x91.toByte(), 0x9D.toByte())
            val protocol = DesfireProtocol(MockTransceiver(mutableListOf(response)))
            val ex =
                assertFailsWith<DesfireAccessControlException> {
                    protocol.getFileList()
                }
            assertTrue(ex.message.contains("Permission denied"))
        }

    // -- Status code: AUTHENTICATION_ERROR (0xAE) --

    @Test
    fun testAuthenticationErrorThrowsAccessControlException() =
        runTest {
            // Status 0xAE = AUTHENTICATION_ERROR
            val response = byteArrayOf(0x91.toByte(), 0xAE.toByte())
            val protocol = DesfireProtocol(MockTransceiver(response))
            val exception = assertFailsWith<DesfireAccessControlException> { protocol.getAppList() }
            assertEquals("Authentication error", exception.message)
        }

    @Test
    fun testAuthenticationErrorThrowsDesfireAccessControlException() =
        runTest {
            // Response: [0x91] [0xAE = AUTHENTICATION_ERROR]
            val response = byteArrayOf(0x91.toByte(), 0xAE.toByte())
            val protocol = DesfireProtocol(MockTransceiver(mutableListOf(response)))
            val ex =
                assertFailsWith<DesfireAccessControlException> {
                    protocol.getFileList()
                }
            assertTrue(ex.message.contains("Authentication error"))
        }

    // -- Status code: AID_NOT_FOUND (0xA0) --

    @Test
    fun testAidNotFoundThrowsNotFoundException() =
        runTest {
            // Status 0xA0 = AID_NOT_FOUND
            val response = byteArrayOf(0x91.toByte(), 0xA0.toByte())
            val protocol = DesfireProtocol(MockTransceiver(response))
            val exception = assertFailsWith<DesfireNotFoundException> { protocol.getAppList() }
            assertEquals("AID not found", exception.message)
        }

    @Test
    fun testAidNotFoundThrowsDesfireNotFoundException() =
        runTest {
            // Response: [0x91] [0xA0 = AID_NOT_FOUND]
            val response = byteArrayOf(0x91.toByte(), 0xA0.toByte())
            val protocol = DesfireProtocol(MockTransceiver(mutableListOf(response)))
            val ex =
                assertFailsWith<DesfireNotFoundException> {
                    protocol.selectApp(0x000001)
                }
            assertTrue(ex.message.contains("AID not found"))
        }

    // -- Status code: FILE_NOT_FOUND (0xF0) --

    @Test
    fun testFileNotFoundThrowsNotFoundException() =
        runTest {
            // Status 0xF0 = FILE_NOT_FOUND
            val response = byteArrayOf(0x91.toByte(), 0xF0.toByte())
            val protocol = DesfireProtocol(MockTransceiver(response))
            val exception = assertFailsWith<DesfireNotFoundException> { protocol.getFileList() }
            assertEquals("File not found", exception.message)
        }

    @Test
    fun testFileNotFoundThrowsDesfireNotFoundException() =
        runTest {
            // Response: [0x91] [0xF0 = FILE_NOT_FOUND]
            val response = byteArrayOf(0x91.toByte(), 0xF0.toByte())
            val protocol = DesfireProtocol(MockTransceiver(mutableListOf(response)))
            val ex =
                assertFailsWith<DesfireNotFoundException> {
                    protocol.readFile(1)
                }
            assertTrue(ex.message.contains("File not found"))
        }

    // -- Status code: unknown --

    @Test
    fun testUnknownStatusCodeThrowsException() =
        runTest {
            // Response: [0x91] [0xBB = unknown]
            val response = byteArrayOf(0x91.toByte(), 0xBB.toByte())
            val protocol = DesfireProtocol(MockTransceiver(response))
            val ex =
                assertFailsWith<Exception> {
                    protocol.getFileList()
                }
            assertEquals(ex.message?.contains("Unknown status code"), true)
            assertEquals(ex.message?.contains("bb"), true)
        }

    @Test
    fun testUnknownStatusCodeThrows() =
        runTest {
            // Status 0xCD = unknown
            val response = byteArrayOf(0x91.toByte(), 0xCD.toByte())
            val protocol = DesfireProtocol(MockTransceiver(mutableListOf(response)))
            val exception = assertFailsWith<Exception> { protocol.getAppList() }
            assertEquals("Unknown status code: cd", exception.message)
        }

    // -- ADDITIONAL_FRAME chaining --

    @Test
    fun testAdditionalFrameChaining() =
        runTest {
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

    // -- ADDITIONAL_FRAME with getAdditionalFrame=false (authentication) --

    @Test
    fun testSendUnlockReturnsOnAdditionalFrame() =
        runTest {
            // sendUnlock uses getAdditionalFrame=false, so when we get ADDITIONAL_FRAME status
            // it should return the data so far instead of requesting more
            // Response: [8 bytes challenge data] [0x91] [0xAF = ADDITIONAL_FRAME]
            val challengeData = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08)
            val response = challengeData + byteArrayOf(0x91.toByte(), 0xAF.toByte())
            val protocol = DesfireProtocol(MockTransceiver(response))
            val result = protocol.sendUnlock(0)
            assertTrue(result.contentEquals(challengeData))
        }

    @Test
    fun testSendAdditionalFrameReturnsOnAdditionalFrame() =
        runTest {
            // sendAdditionalFrame also uses getAdditionalFrame=false
            val responseData = byteArrayOf(0x0A, 0x0B, 0x0C, 0x0D)
            val response = responseData + byteArrayOf(0x91.toByte(), 0xAF.toByte())
            val protocol = DesfireProtocol(MockTransceiver(response))
            val result = protocol.sendAdditionalFrame(byteArrayOf(0x01, 0x02))
            assertTrue(result.contentEquals(responseData))
        }

    // -- Read operations --

    @Test
    fun testReadFileReturnsPayload() =
        runTest {
            // Simulate reading a file: 4 bytes of data + status OK
            val response = byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0x91.toByte(), 0x00)
            val protocol = DesfireProtocol(MockTransceiver(mutableListOf(response)))
            val result = protocol.readFile(0)
            assertContentEquals(byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte()), result)
        }
}
