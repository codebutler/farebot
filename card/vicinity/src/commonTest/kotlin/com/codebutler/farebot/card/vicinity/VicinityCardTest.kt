/*
 * VicinityCardTest.kt
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

import com.codebutler.farebot.card.CardType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class VicinityCardTest {
    private val testTagId = byteArrayOf(0x01, 0x02, 0x03, 0x04)
    private val testTime = Instant.fromEpochSeconds(1609459200) // 2021-01-01T00:00:00Z

    private fun createCard(pages: List<VicinityPage>): VicinityCard = VicinityCard.create(testTagId, testTime, pages)

    @Test
    fun testCardType() {
        val card = createCard(emptyList())
        assertEquals(CardType.Vicinity, card.cardType)
    }

    @Test
    fun testGetPage() {
        val pages =
            listOf(
                VicinityPage.create(0, byteArrayOf(0x11, 0x22, 0x33, 0x44)),
                VicinityPage.create(1, byteArrayOf(0x55, 0x66, 0x77, 0x88.toByte())),
            )
        val card = createCard(pages)

        assertEquals(0, card.getPage(0).index)
        assertEquals(1, card.getPage(1).index)
        assertTrue(card.getPage(0).data.contentEquals(byteArrayOf(0x11, 0x22, 0x33, 0x44)))
    }

    @Test
    fun testReadPages() {
        val pages =
            listOf(
                VicinityPage.create(0, byteArrayOf(0x11, 0x22, 0x33, 0x44)),
                VicinityPage.create(1, byteArrayOf(0x55, 0x66, 0x77, 0x88.toByte())),
                VicinityPage.create(2, byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte())),
            )
        val card = createCard(pages)

        // Read pages 0-1
        val data01 = card.readPages(0, 2)
        assertEquals(8, data01.size)
        assertTrue(
            data01.contentEquals(
                byteArrayOf(0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88.toByte()),
            ),
        )

        // Read pages 1-2
        val data12 = card.readPages(1, 2)
        assertEquals(8, data12.size)
        assertTrue(
            data12.contentEquals(
                byteArrayOf(
                    0x55,
                    0x66,
                    0x77,
                    0x88.toByte(),
                    0xAA.toByte(),
                    0xBB.toByte(),
                    0xCC.toByte(),
                    0xDD.toByte(),
                ),
            ),
        )

        // Read single page
        val data2 = card.readPages(2, 1)
        assertEquals(4, data2.size)
        assertTrue(data2.contentEquals(byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte())))
    }

    @Test
    fun testReadBytes_withinSinglePage() {
        val pages =
            listOf(
                VicinityPage.create(0, byteArrayOf(0x11, 0x22, 0x33, 0x44)),
                VicinityPage.create(1, byteArrayOf(0x55, 0x66, 0x77, 0x88.toByte())),
            )
        val card = createCard(pages)

        // Read 2 bytes from offset 1 (within page 0)
        val data = card.readBytes(1, 2)
        assertEquals(2, data.size)
        assertTrue(data.contentEquals(byteArrayOf(0x22, 0x33)))
    }

    @Test
    fun testReadBytes_crossPageBoundary() {
        val pages =
            listOf(
                VicinityPage.create(0, byteArrayOf(0x11, 0x22, 0x33, 0x44)),
                VicinityPage.create(1, byteArrayOf(0x55, 0x66, 0x77, 0x88.toByte())),
            )
        val card = createCard(pages)

        // Read 4 bytes from offset 2 (crosses page 0->1 boundary)
        val data = card.readBytes(2, 4)
        assertEquals(4, data.size)
        assertTrue(data.contentEquals(byteArrayOf(0x33, 0x44, 0x55, 0x66)))
    }

    @Test
    fun testReadBytes_fullPage() {
        val pages =
            listOf(
                VicinityPage.create(0, byteArrayOf(0x11, 0x22, 0x33, 0x44)),
                VicinityPage.create(1, byteArrayOf(0x55, 0x66, 0x77, 0x88.toByte())),
            )
        val card = createCard(pages)

        // Read entire page 1
        val data = card.readBytes(4, 4)
        assertEquals(4, data.size)
        assertTrue(data.contentEquals(byteArrayOf(0x55, 0x66, 0x77, 0x88.toByte())))
    }

    @Test
    fun testRawVicinityCardRoundTrip() {
        val pages =
            listOf(
                VicinityPage.create(0, byteArrayOf(0x11, 0x22, 0x33, 0x44)),
                VicinityPage.create(1, byteArrayOf(0x55, 0x66, 0x77, 0x88.toByte())),
            )
        val sysInfo = byteArrayOf(0x0F, 0x01, 0x02, 0x03)

        val rawCard =
            com.codebutler.farebot.card.vicinity.raw.RawVicinityCard.create(
                tagId = testTagId,
                scannedAt = testTime,
                pages = pages,
                sysInfo = sysInfo,
                isPartialRead = true,
            )

        assertEquals(CardType.Vicinity, rawCard.cardType())
        assertTrue(rawCard.tagId().contentEquals(testTagId))
        assertEquals(testTime, rawCard.scannedAt())
        assertTrue(rawCard.isPartialRead)

        val parsed = rawCard.parse()
        assertEquals(CardType.Vicinity, parsed.cardType)
        assertEquals(2, parsed.pages.size)
        assertNotNull(parsed.sysInfo)
        assertTrue(parsed.isPartialRead)
    }

    @Test
    fun testVicinityPageCreate() {
        val page = VicinityPage.create(5, byteArrayOf(0x11, 0x22))
        assertEquals(5, page.index)
        assertTrue(page.data.contentEquals(byteArrayOf(0x11, 0x22)))
        assertEquals(false, page.isUnauthorized)
    }

    @Test
    fun testVicinityPageUnauthorized() {
        val page = VicinityPage.unauthorized(3)
        assertEquals(3, page.index)
        assertEquals(0, page.data.size)
        assertTrue(page.isUnauthorized)
    }
}
