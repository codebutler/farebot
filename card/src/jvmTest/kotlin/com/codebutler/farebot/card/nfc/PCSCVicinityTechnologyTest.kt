/*
 * PCSCVicinityTechnologyTest.kt
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

package com.codebutler.farebot.card.nfc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PCSCVicinityTechnologyTest {
    @Test
    fun testUidReturnsProvidedUid() {
        val expectedUid = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08)
        val tech =
            PCSCVicinityTechnology(
                channel = FakePCSCChannel(),
                uid = expectedUid,
            )
        assertTrue(tech.uid.contentEquals(expectedUid))
    }

    @Test
    fun testConnectAndClose() {
        val tech =
            PCSCVicinityTechnology(
                channel = FakePCSCChannel(),
                uid = byteArrayOf(),
            )
        tech.connect()
        assertTrue(tech.isConnected)
        tech.close()
        assertEquals(false, tech.isConnected)
    }
}
