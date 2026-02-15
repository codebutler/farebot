/*
 * UltralightCardReaderTest.kt
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

package com.codebutler.farebot.card.ultralight

import com.codebutler.farebot.card.nfc.UltralightTechnology
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UltralightCardReaderTest {
    private val testTagId = byteArrayOf(0x01, 0x02, 0x03, 0x04)

    /**
     * Fake UltralightTechnology that tracks which page offsets were read.
     * Each readPages call returns 16 bytes (4 pages), where each page's first byte
     * is the page number for easy verification.
     *
     * transceive() simulates protocol-level commands:
     * - GET_VERSION (0x60) always throws (not an EV1 card)
     * - AUTH_1 (0x1a) returns a nonce for TYPE_ULTRALIGHT_C, throws for others
     * - HALT (0x50) always throws (expected behavior)
     *
     * This causes detectCardType to identify:
     * - TYPE_ULTRALIGHT -> MF0ICU1 (16 pages)
     * - TYPE_ULTRALIGHT_C -> MF0ICU2 (44 pages)
     */
    private class FakeUltralightTechnology(
        override val type: Int,
        private val totalPages: Int,
    ) : UltralightTechnology {
        val readPageOffsets = mutableListOf<Int>()

        override fun readPages(pageOffset: Int): ByteArray {
            readPageOffsets.add(pageOffset)
            check(pageOffset < totalPages) {
                "readPages called with offset $pageOffset, but card only has $totalPages pages"
            }
            // Return 16 bytes (4 pages of 4 bytes each).
            // First byte of each page is the page number for identification.
            val buffer = ByteArray(4 * UltralightTechnology.PAGE_SIZE)
            for (i in 0 until 4) {
                val pageNum = pageOffset + i
                buffer[i * UltralightTechnology.PAGE_SIZE] = pageNum.toByte()
            }
            return buffer
        }

        override fun transceive(data: ByteArray): ByteArray {
            val cmd = data[0].toInt() and 0xFF
            when (cmd) {
                0x1a -> {
                    // AUTH_1: Ultralight C cards reply with an 8-byte nonce
                    if (type == UltralightTechnology.TYPE_ULTRALIGHT_C) {
                        return ByteArray(8)
                    }
                }
            }
            // GET_VERSION (0x60), HALT (0x50), and all other commands: not supported
            throw UnsupportedOperationException("FakeUltralightTechnology: unsupported command 0x${cmd.toString(16)}")
        }

        override fun connect() {}

        override fun close() {}

        override val isConnected: Boolean = true
    }

    @Test
    fun testUltralightReadsCorrectNumberOfPages() {
        val tech =
            FakeUltralightTechnology(
                type = UltralightTechnology.TYPE_ULTRALIGHT,
                totalPages = UltralightCard.UltralightType.MF0ICU1.pageCount,
            )

        val result = UltralightCardReader.readCard(testTagId, tech)

        // Protocol detection identifies as MF0ICU1 (16 pages)
        assertEquals(UltralightCard.UltralightType.MF0ICU1.pageCount, result.pages.size)
    }

    @Test
    fun testUltralightCReadsCorrectNumberOfPages() {
        val tech =
            FakeUltralightTechnology(
                type = UltralightTechnology.TYPE_ULTRALIGHT_C,
                totalPages = UltralightCard.UltralightType.MF0ICU2.pageCount,
            )

        val result = UltralightCardReader.readCard(testTagId, tech)

        // Protocol detection identifies as MF0ICU2 (44 pages)
        assertEquals(UltralightCard.UltralightType.MF0ICU2.pageCount, result.pages.size)
    }

    @Test
    fun testPageIndicesAreSequential() {
        val tech =
            FakeUltralightTechnology(
                type = UltralightTechnology.TYPE_ULTRALIGHT,
                totalPages = UltralightCard.UltralightType.MF0ICU1.pageCount,
            )

        val result = UltralightCardReader.readCard(testTagId, tech)

        for (i in result.pages.indices) {
            assertEquals(i, result.pages[i].index)
        }
    }

    @Test
    fun testPageDataExtractedCorrectly() {
        val tech =
            FakeUltralightTechnology(
                type = UltralightTechnology.TYPE_ULTRALIGHT,
                totalPages = UltralightCard.UltralightType.MF0ICU1.pageCount,
            )

        val result = UltralightCardReader.readCard(testTagId, tech)

        // Each page's first byte should be its page number (set by FakeUltralightTechnology)
        for (page in result.pages) {
            assertEquals(page.index.toByte(), page.data[0])
            assertEquals(UltralightTechnology.PAGE_SIZE, page.data.size)
        }
    }

    @Test
    fun testReadPagesCalledAtCorrectOffsets() {
        val tech =
            FakeUltralightTechnology(
                type = UltralightTechnology.TYPE_ULTRALIGHT,
                totalPages = UltralightCard.UltralightType.MF0ICU1.pageCount,
            )

        UltralightCardReader.readCard(testTagId, tech)

        // readPages should be called every 4 pages (MF0ICU1 = 16 pages, size = 15, pages 0..15)
        val detectedPageCount = UltralightCard.UltralightType.MF0ICU1.pageCount
        val expectedOffsets = (0 until detectedPageCount step 4).toList()
        assertEquals(expectedOffsets, tech.readPageOffsets)
    }

    @Test
    fun testDoesNotReadBeyondCardSize() {
        val detectedPageCount = UltralightCard.UltralightType.MF0ICU1.pageCount
        val tech =
            FakeUltralightTechnology(
                type = UltralightTechnology.TYPE_ULTRALIGHT,
                totalPages = detectedPageCount,
            )

        val result = UltralightCardReader.readCard(testTagId, tech)

        // Verify no page with index >= detectedPageCount exists
        assertTrue(result.pages.all { it.index < detectedPageCount })
        // Verify readPages was never called with an offset >= detectedPageCount
        assertTrue(tech.readPageOffsets.all { it < detectedPageCount })
    }
}
