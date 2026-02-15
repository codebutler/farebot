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
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Tests for DESFire protocol status code handling.
 * Validates the exception types introduced in PR #218:
 * - PermissionDeniedException (0x9D)
 * - UnauthorizedException / "Authentication error" (0xAE)
 * - NotFoundException for AID_NOT_FOUND (0xA0) and FILE_NOT_FOUND (0xF0)
 */
class DesfireProtocolTest {
    /**
     * A mock [CardTransceiver] that returns a pre-configured response
     * for any transceive call.
     */
    private class MockTransceiver(
        private val response: ByteArray,
    ) : CardTransceiver {
        override fun transceive(data: ByteArray): ByteArray = response

        override val maxTransceiveLength: Int = 253

        override fun connect() {}

        override fun close() {}

        override val isConnected: Boolean = true
    }

    // -- Buffer size guard --

    @Test
    fun testEmptyResponseThrows() {
        val protocol = DesfireProtocol(MockTransceiver(byteArrayOf()))
        assertFails {
            protocol.getFileList()
        }
    }

    @Test
    fun testSingleByteResponseThrows() {
        val protocol = DesfireProtocol(MockTransceiver(byteArrayOf(0x91.toByte())))
        assertFails {
            protocol.getFileList()
        }
    }

    // -- Status code: OPERATION_OK --

    @Test
    fun testOperationOkReturnsData() {
        // Response: [data byte 0x42] [0x91] [0x00 = OPERATION_OK]
        val response = byteArrayOf(0x42, 0x91.toByte(), 0x00)
        val protocol = DesfireProtocol(MockTransceiver(response))
        val result = protocol.getFileList()
        // getFileList converts each byte to an int
        assertTrue(result.size == 1)
        assertTrue(result[0] == 0x42)
    }

    // -- Status code: PERMISSION_DENIED (0x9D) --

    @Test
    fun testPermissionDeniedThrowsPermissionDeniedException() {
        // Response: [0x91] [0x9D = PERMISSION_DENIED]
        val response = byteArrayOf(0x91.toByte(), 0x9D.toByte())
        val protocol = DesfireProtocol(MockTransceiver(response))
        // PermissionDeniedException extends UnauthorizedException
        val ex =
            assertFailsWith<PermissionDeniedException> {
                protocol.getFileList()
            }
        assertTrue(ex.message!!.contains("Permission denied"))
    }

    @Test
    fun testPermissionDeniedIsUnauthorizedException() {
        val response = byteArrayOf(0x91.toByte(), 0x9D.toByte())
        val protocol = DesfireProtocol(MockTransceiver(response))
        // Should also be catchable as UnauthorizedException
        assertFailsWith<UnauthorizedException> {
            protocol.getFileList()
        }
    }

    // -- Status code: AUTHENTICATION_ERROR (0xAE) --

    @Test
    fun testAuthenticationErrorThrowsUnauthorizedException() {
        // Response: [0x91] [0xAE = AUTHENTICATION_ERROR]
        val response = byteArrayOf(0x91.toByte(), 0xAE.toByte())
        val protocol = DesfireProtocol(MockTransceiver(response))
        val ex =
            assertFailsWith<UnauthorizedException> {
                protocol.getFileList()
            }
        assertTrue(ex.message!!.contains("Authentication error"))
    }

    // -- Status code: AID_NOT_FOUND (0xA0) --

    @Test
    fun testAidNotFoundThrowsNotFoundException() {
        // Response: [0x91] [0xA0 = AID_NOT_FOUND]
        val response = byteArrayOf(0x91.toByte(), 0xA0.toByte())
        val protocol = DesfireProtocol(MockTransceiver(response))
        val ex =
            assertFailsWith<NotFoundException> {
                protocol.selectApp(0x000001)
            }
        assertTrue(ex.message!!.contains("AID not found"))
    }

    // -- Status code: FILE_NOT_FOUND (0xF0) --

    @Test
    fun testFileNotFoundThrowsNotFoundException() {
        // Response: [0x91] [0xF0 = FILE_NOT_FOUND]
        val response = byteArrayOf(0x91.toByte(), 0xF0.toByte())
        val protocol = DesfireProtocol(MockTransceiver(response))
        val ex =
            assertFailsWith<NotFoundException> {
                protocol.readFile(1)
            }
        assertTrue(ex.message!!.contains("File not found"))
    }

    // -- Status code: unknown --

    @Test
    fun testUnknownStatusCodeThrowsException() {
        // Response: [0x91] [0xBB = unknown]
        val response = byteArrayOf(0x91.toByte(), 0xBB.toByte())
        val protocol = DesfireProtocol(MockTransceiver(response))
        val ex =
            assertFailsWith<Exception> {
                protocol.getFileList()
            }
        assertTrue(ex.message!!.contains("Unknown status code"))
        assertTrue(ex.message!!.contains("bb"))
    }

    // -- Missing 0x91 marker --

    @Test
    fun testResponseWithWrongMarkerThrowsInvalidResponse() {
        // Response has 2 bytes but the second-to-last is not 0x91
        val response = byteArrayOf(0x90.toByte(), 0x00)
        val protocol = DesfireProtocol(MockTransceiver(response))
        val ex =
            assertFailsWith<Exception> {
                protocol.getFileList()
            }
        assertTrue(ex.message!!.contains("Invalid response"))
    }

    // -- Valid two-byte response (no data, just status) --

    @Test
    fun testMinimalOkResponseReturnsEmptyData() {
        // Response: [0x91] [0x00 = OPERATION_OK] -- no data bytes before status
        val response = byteArrayOf(0x91.toByte(), 0x00)
        val protocol = DesfireProtocol(MockTransceiver(response))
        val result = protocol.getFileList()
        assertTrue(result.isEmpty())
    }

    // -- ADDITIONAL_FRAME with getAdditionalFrame=false (sendUnlock) --

    @Test
    fun testSendUnlockReturnsOnAdditionalFrame() {
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
    fun testSendAdditionalFrameReturnsOnAdditionalFrame() {
        // sendAdditionalFrame also uses getAdditionalFrame=false
        val responseData = byteArrayOf(0x0A, 0x0B, 0x0C, 0x0D)
        val response = responseData + byteArrayOf(0x91.toByte(), 0xAF.toByte())
        val protocol = DesfireProtocol(MockTransceiver(response))
        val result = protocol.sendAdditionalFrame(byteArrayOf(0x01, 0x02))
        assertTrue(result.contentEquals(responseData))
    }
}
