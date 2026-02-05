/*
 * RawLevelTest.kt
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

package com.codebutler.farebot.shared.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RawLevelTest {

    @Test
    fun testFromString_none() {
        assertEquals(RawLevel.NONE, RawLevel.fromString("NONE"))
    }

    @Test
    fun testFromString_unknownOnly() {
        assertEquals(RawLevel.UNKNOWN_ONLY, RawLevel.fromString("UNKNOWN_ONLY"))
    }

    @Test
    fun testFromString_all() {
        assertEquals(RawLevel.ALL, RawLevel.fromString("ALL"))
    }

    @Test
    fun testFromString_invalid() {
        assertNull(RawLevel.fromString("INVALID"))
        assertNull(RawLevel.fromString(""))
        assertNull(RawLevel.fromString("none"))  // case-sensitive
    }

    @Test
    fun testToString() {
        assertEquals("NONE", RawLevel.NONE.toString())
        assertEquals("UNKNOWN_ONLY", RawLevel.UNKNOWN_ONLY.toString())
        assertEquals("ALL", RawLevel.ALL.toString())
    }
}
