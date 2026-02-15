/*
 * UltralightPageTest.kt
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

import com.codebutler.farebot.card.UnauthorizedException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UltralightPageTest {
    @Test
    fun testCreatePageHasCorrectIndex() {
        val page = UltralightPage.create(5, byteArrayOf(0x01, 0x02, 0x03, 0x04))
        assertEquals(5, page.index)
    }

    @Test
    fun testCreatePageIsNotUnauthorized() {
        val page = UltralightPage.create(0, byteArrayOf(0x01, 0x02, 0x03, 0x04))
        assertFalse(page.isUnauthorized)
    }

    @Test
    fun testCreatePageDataIsAccessible() {
        val data = byteArrayOf(0x0A, 0x0B, 0x0C, 0x0D)
        val page = UltralightPage.create(0, data)
        assertContentEquals(data, page.data)
    }

    @Test
    fun testUnauthorizedPageHasCorrectIndex() {
        val page = UltralightPage.unauthorized(7)
        assertEquals(7, page.index)
    }

    @Test
    fun testUnauthorizedPageIsMarkedUnauthorized() {
        val page = UltralightPage.unauthorized(0)
        assertTrue(page.isUnauthorized)
    }

    @Test
    fun testUnauthorizedPageDataThrowsUnauthorizedException() {
        val page = UltralightPage.unauthorized(0)
        assertFailsWith<UnauthorizedException> {
            page.data
        }
    }

    @Test
    fun testMixedAuthorizedAndUnauthorizedPages() {
        val pages =
            listOf(
                UltralightPage.create(0, byteArrayOf(0x01, 0x02, 0x03, 0x04)),
                UltralightPage.create(1, byteArrayOf(0x05, 0x06, 0x07, 0x08)),
                UltralightPage.unauthorized(2),
                UltralightPage.unauthorized(3),
            )

        assertFalse(pages[0].isUnauthorized)
        assertFalse(pages[1].isUnauthorized)
        assertTrue(pages[2].isUnauthorized)
        assertTrue(pages[3].isUnauthorized)

        // Authorized pages are readable
        assertContentEquals(byteArrayOf(0x01, 0x02, 0x03, 0x04), pages[0].data)
        assertContentEquals(byteArrayOf(0x05, 0x06, 0x07, 0x08), pages[1].data)

        // Unauthorized pages throw
        assertFailsWith<UnauthorizedException> { pages[2].data }
        assertFailsWith<UnauthorizedException> { pages[3].data }
    }

    @Test
    fun testUltralightCardReadPagesThrowsOnUnauthorizedPage() {
        val pages =
            listOf(
                UltralightPage.create(0, byteArrayOf(0x01, 0x02, 0x03, 0x04)),
                UltralightPage.create(1, byteArrayOf(0x05, 0x06, 0x07, 0x08)),
                UltralightPage.unauthorized(2),
                UltralightPage.create(3, byteArrayOf(0x0D, 0x0E, 0x0F, 0x10)),
            )

        val card =
            UltralightCard.create(
                tagId = byteArrayOf(0x01, 0x02, 0x03, 0x04),
                scannedAt = kotlin.time.Instant.fromEpochMilliseconds(0),
                pages = pages,
                type = 1,
            )

        // Reading authorized pages succeeds
        val data = card.readPages(0, 2)
        assertEquals(8, data.size)

        // Reading range that includes unauthorized page throws
        assertFailsWith<UnauthorizedException> {
            card.readPages(1, 3)
        }
    }

    @Test
    fun testUltralightCardGetPageReturnsUnauthorizedPage() {
        val pages =
            listOf(
                UltralightPage.create(0, byteArrayOf(0x01, 0x02, 0x03, 0x04)),
                UltralightPage.unauthorized(1),
            )

        val card =
            UltralightCard.create(
                tagId = byteArrayOf(0x01, 0x02, 0x03, 0x04),
                scannedAt = kotlin.time.Instant.fromEpochMilliseconds(0),
                pages = pages,
                type = 1,
            )

        // getPage returns the page object regardless of authorization
        val authorizedPage = card.getPage(0)
        assertFalse(authorizedPage.isUnauthorized)

        val unauthorizedPage = card.getPage(1)
        assertTrue(unauthorizedPage.isUnauthorized)
    }
}
