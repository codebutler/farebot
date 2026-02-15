/*
 * RawClassicSectorTest.kt
 *
 * Copyright 2025 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.card.classic.raw

import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.card.classic.InvalidClassicSector
import com.codebutler.farebot.card.classic.UnauthorizedClassicSector
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalStdlibApi::class)
class RawClassicSectorTest {
    private val testKeyA = "A0A1A2A3A4A5".hexToByteArray()
    private val testKeyB = "B0B1B2B3B4B5".hexToByteArray()

    private fun createTestBlocks(): List<RawClassicBlock> =
        listOf(
            RawClassicBlock.create(0, ByteArray(16)),
            RawClassicBlock.create(1, ByteArray(16)),
            RawClassicBlock.create(2, ByteArray(16)),
            RawClassicBlock.create(3, ByteArray(16)),
        )

    @Test
    fun testCreateDataWithKeys() {
        val sector = RawClassicSector.createData(0, createTestBlocks(), testKeyA, testKeyB)

        assertEquals(RawClassicSector.TYPE_DATA, sector.type)
        assertEquals(0, sector.index)
        val blocks = sector.blocks
        assertNotNull(blocks)
        assertEquals(4, blocks.size)
        assertContentEquals(testKeyA, sector.keyA)
        assertContentEquals(testKeyB, sector.keyB)
        assertNull(sector.errorMessage)
    }

    @Test
    fun testCreateDataWithOnlyKeyA() {
        val sector = RawClassicSector.createData(1, createTestBlocks(), keyA = testKeyA)

        assertContentEquals(testKeyA, sector.keyA)
        assertNull(sector.keyB)
    }

    @Test
    fun testCreateDataWithOnlyKeyB() {
        val sector = RawClassicSector.createData(2, createTestBlocks(), keyB = testKeyB)

        assertNull(sector.keyA)
        assertContentEquals(testKeyB, sector.keyB)
    }

    @Test
    fun testCreateDataWithoutKeys() {
        val sector = RawClassicSector.createData(0, createTestBlocks())

        assertEquals(RawClassicSector.TYPE_DATA, sector.type)
        assertNull(sector.keyA)
        assertNull(sector.keyB)
    }

    @Test
    fun testCreateUnauthorizedWithKeys() {
        val sector = RawClassicSector.createUnauthorized(5, testKeyA, testKeyB)

        assertEquals(RawClassicSector.TYPE_UNAUTHORIZED, sector.type)
        assertEquals(5, sector.index)
        assertNull(sector.blocks)
        assertContentEquals(testKeyA, sector.keyA)
        assertContentEquals(testKeyB, sector.keyB)
        assertNull(sector.errorMessage)
    }

    @Test
    fun testCreateUnauthorizedWithoutKeys() {
        val sector = RawClassicSector.createUnauthorized(3)

        assertEquals(RawClassicSector.TYPE_UNAUTHORIZED, sector.type)
        assertNull(sector.keyA)
        assertNull(sector.keyB)
    }

    @Test
    fun testCreateInvalidWithKeys() {
        val sector = RawClassicSector.createInvalid(7, "Read error", testKeyA, testKeyB)

        assertEquals(RawClassicSector.TYPE_INVALID, sector.type)
        assertEquals(7, sector.index)
        assertNull(sector.blocks)
        assertContentEquals(testKeyA, sector.keyA)
        assertContentEquals(testKeyB, sector.keyB)
        assertEquals("Read error", sector.errorMessage)
    }

    @Test
    fun testCreateInvalidWithoutKeys() {
        val sector = RawClassicSector.createInvalid(4, "Timeout")

        assertEquals(RawClassicSector.TYPE_INVALID, sector.type)
        assertNull(sector.keyA)
        assertNull(sector.keyB)
        assertEquals("Timeout", sector.errorMessage)
    }

    @Test
    fun testParseDataSectorWithKeys() {
        val sector = RawClassicSector.createData(0, createTestBlocks(), testKeyA, testKeyB)
        val parsed = sector.parse()

        assertTrue(parsed is DataClassicSector)
        assertEquals(0, parsed.index)
        assertEquals(4, parsed.blocks.size)
    }

    @Test
    fun testParseUnauthorizedSectorWithKeys() {
        val sector = RawClassicSector.createUnauthorized(5, testKeyA, testKeyB)
        val parsed = sector.parse()

        assertTrue(parsed is UnauthorizedClassicSector)
        assertEquals(5, parsed.index)
    }

    @Test
    fun testParseInvalidSectorWithKeys() {
        val sector = RawClassicSector.createInvalid(7, "Read error", testKeyA, testKeyB)
        val parsed = sector.parse()

        assertTrue(parsed is InvalidClassicSector)
        assertEquals(7, parsed.index)
    }

    @Test
    fun testKeyDefaultKey() {
        // Verify that the default MIFARE key (FF FF FF FF FF FF) can be stored
        val defaultKey = "FFFFFFFFFFFF".hexToByteArray()
        val sector = RawClassicSector.createData(0, createTestBlocks(), keyA = defaultKey)

        assertContentEquals(defaultKey, sector.keyA)
        assertNull(sector.keyB)
    }
}
