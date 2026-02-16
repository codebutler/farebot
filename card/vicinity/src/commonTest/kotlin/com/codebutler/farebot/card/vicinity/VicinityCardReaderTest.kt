/*
 * VicinityCardReaderTest.kt
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

package com.codebutler.farebot.card.vicinity

import com.codebutler.farebot.card.nfc.VicinityTechnology
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VicinityCardReaderTest {
    private val testUid = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08)
    private val testTagId = byteArrayOf(0x0A, 0x0B, 0x0C, 0x0D)

    @Test
    fun testReadCard_multiplePages() = runTest {
        val pageData =
            mapOf(
                0 to byteArrayOf(0x11, 0x22, 0x33, 0x44),
                1 to byteArrayOf(0x55, 0x66, 0x77, 0x88.toByte()),
                2 to byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte()),
            )
        val tech = FakeVicinityTechnology(testUid, pageData, sysInfoResponse = null)

        val card = VicinityCardReader.readCard(testTagId, tech)

        assertEquals(3, card.pages.size)
        assertEquals(0, card.pages[0].index)
        assertEquals(1, card.pages[1].index)
        assertEquals(2, card.pages[2].index)
        assertTrue(card.pages[0].data.contentEquals(byteArrayOf(0x11, 0x22, 0x33, 0x44)))
        assertTrue(card.pages[1].data.contentEquals(byteArrayOf(0x55, 0x66, 0x77, 0x88.toByte())))
        assertTrue(
            card.pages[2].data.contentEquals(byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte())),
        )
        assertFalse(card.isPartialRead)
        assertTrue(card.tagId().contentEquals(testTagId))
    }

    @Test
    fun testReadCard_withSysInfo() = runTest {
        val sysInfoBytes = byteArrayOf(0x0F, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08)
        val pageData = mapOf(0 to byteArrayOf(0x11, 0x22, 0x33, 0x44))
        val tech = FakeVicinityTechnology(testUid, pageData, sysInfoResponse = sysInfoBytes)

        val card = VicinityCardReader.readCard(testTagId, tech)

        val sysInfo = assertNotNull(card.sysInfo)
        // sysInfo should have the status byte (0x00) stripped — the fake returns [0x00] + sysInfoBytes
        // so card.sysInfo should be sysInfoBytes
        assertTrue(sysInfo.contentEquals(sysInfoBytes))
        assertEquals(1, card.pages.size)
    }

    @Test
    fun testReadCard_sysInfoFails() = runTest {
        val pageData = mapOf(0 to byteArrayOf(0x11, 0x22, 0x33, 0x44))
        val tech = FakeVicinityTechnology(testUid, pageData, sysInfoResponse = null, sysInfoThrows = true)

        val card = VicinityCardReader.readCard(testTagId, tech)

        assertNull(card.sysInfo)
        assertEquals(1, card.pages.size)
    }

    @Test
    fun testReadCard_partialRead() = runTest {
        // Pages 0 and 1 succeed, page 2 throws (simulating tag lost)
        val pageData =
            mapOf(0 to byteArrayOf(0x11, 0x22, 0x33, 0x44), 1 to byteArrayOf(0x55, 0x66, 0x77, 0x88.toByte()))
        val tech = FakeVicinityTechnology(testUid, pageData, sysInfoResponse = null, throwOnMissingPage = true)

        val card = VicinityCardReader.readCard(testTagId, tech)

        assertEquals(2, card.pages.size)
        assertTrue(card.isPartialRead)
    }

    @Test
    fun testReadCard_emptyCard() = runTest {
        // No readable pages — first read returns error status
        val tech = FakeVicinityTechnology(testUid, emptyMap(), sysInfoResponse = null)

        val card = VicinityCardReader.readCard(testTagId, tech)

        assertEquals(0, card.pages.size)
        assertFalse(card.isPartialRead)
    }

    @Test
    fun testReadCard_errorResponseStopsReading() = runTest {
        // Page 0 returns error status (non-zero first byte)
        val tech = FakeVicinityTechnology(testUid, emptyMap(), sysInfoResponse = null, errorOnPage = 0)

        val card = VicinityCardReader.readCard(testTagId, tech)

        assertEquals(0, card.pages.size)
        assertFalse(card.isPartialRead)
    }

    /**
     * Fake [VicinityTechnology] for testing the card reader algorithm.
     *
     * Simulates NFC-V transceive responses. The command format expected:
     * - System info: [0x22, 0x2b, ...uid...]
     * - Read single block: [0x22, 0x20, ...uid..., blockNumber]
     *
     * Responses include a leading 0x00 status byte for success.
     */
    private class FakeVicinityTechnology(
        override val uid: ByteArray,
        private val pageData: Map<Int, ByteArray>,
        private val sysInfoResponse: ByteArray?,
        private val sysInfoThrows: Boolean = false,
        private val throwOnMissingPage: Boolean = false,
        private val errorOnPage: Int = -1,
    ) : VicinityTechnology {
        override fun connect() {}

        override fun close() {}

        override val isConnected: Boolean = true

        override suspend fun transceive(data: ByteArray): ByteArray {
            require(data.size >= 2)

            val command = data[1]

            // System info command (0x2b)
            if (command == 0x2b.toByte()) {
                if (sysInfoThrows) throw Exception("System info not supported")
                return if (sysInfoResponse != null) {
                    byteArrayOf(0x00) + sysInfoResponse
                } else {
                    byteArrayOf(0x01) // error status
                }
            }

            // Read single block command (0x20)
            if (command == 0x20.toByte()) {
                val blockNumber = data.last().toInt() and 0xFF

                if (blockNumber == errorOnPage) {
                    return byteArrayOf(0x01) // error status
                }

                val blockData = pageData[blockNumber]
                if (blockData != null) {
                    return byteArrayOf(0x00) + blockData
                }

                if (throwOnMissingPage) {
                    throw Exception("Tag lost")
                }

                // No data for this page — return empty to signal end
                return byteArrayOf()
            }

            throw Exception("Unknown command: ${command.toInt() and 0xFF}")
        }
    }
}
