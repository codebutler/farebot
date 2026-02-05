/*
 * ClassicCardTest.kt
 *
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
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

package com.codebutler.farebot.card.classic

import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for Classic card structure and parsing.
 *
 * Ported from Metrodroid's ClassicCardTest.kt and ClassicReaderTest.kt
 */
class ClassicCardTest {

    private val testTime = Instant.fromEpochMilliseconds(1264982400000)
    private val testTagId = byteArrayOf(0x01, 0x02, 0x03, 0x04)

    @Test
    fun testEmptySectorListCreatesValidCard() {
        val card = ClassicCard.create(testTagId, testTime, emptyList())
        assertEquals(0, card.sectors.size)
        assertNull(card.manufacturingInfo)
    }

    @Test
    fun testManufacturingInfoParsedFromSector0() {
        // Create sector 0 with valid manufacturer data in block 0
        // Block 0 layout: UID (4 bytes) + BCC + SAK + ATQA (2 bytes) + manufacturer data (8 bytes)
        val manufacturerBlock = ByteArray(16).also {
            it[0] = 0x01  // UID byte 1
            it[1] = 0x02  // UID byte 2
            it[2] = 0x03  // UID byte 3
            it[3] = 0x04  // UID byte 4
            it[4] = 0x04  // BCC (XOR of UID bytes)
            it[5] = 0x08  // SAK
            it[6] = 0x04  // ATQA byte 1
            it[7] = 0x00  // ATQA byte 2
            // Manufacturer data bytes 8-15
        }

        val blocks = listOf(
            ClassicBlock.create(ClassicBlock.TYPE_MANUFACTURER, 0, manufacturerBlock),
            ClassicBlock.create(ClassicBlock.TYPE_DATA, 1, ByteArray(16)),
            ClassicBlock.create(ClassicBlock.TYPE_DATA, 2, ByteArray(16)),
            ClassicBlock.create(ClassicBlock.TYPE_TRAILER, 3, ByteArray(16))
        )

        val sector0 = DataClassicSector(0, blocks)
        val card = ClassicCard.create(testTagId, testTime, listOf(sector0))

        assertEquals(1, card.sectors.size)
        assertNotNull(card.manufacturingInfo)
    }

    @Test
    fun testUnauthorizedSectorHasNoManufacturingInfo() {
        val sector0 = UnauthorizedClassicSector.create(0)
        val card = ClassicCard.create(testTagId, testTime, listOf(sector0))

        assertEquals(1, card.sectors.size)
        assertNull(card.manufacturingInfo)
    }

    @Test
    fun testInvalidSectorHasNoManufacturingInfo() {
        val sector0 = InvalidClassicSector.create(0, "Read error")
        val card = ClassicCard.create(testTagId, testTime, listOf(sector0))

        assertEquals(1, card.sectors.size)
        assertNull(card.manufacturingInfo)
    }

    @Test
    fun testGetSector() {
        val sectors = (0 until 16).map { index ->
            if (index == 0) {
                val blocks = (0 until 4).map { blockIndex ->
                    val type = when (blockIndex) {
                        0 -> ClassicBlock.TYPE_MANUFACTURER
                        3 -> ClassicBlock.TYPE_TRAILER
                        else -> ClassicBlock.TYPE_DATA
                    }
                    ClassicBlock.create(type, blockIndex, ByteArray(16))
                }
                DataClassicSector(index, blocks)
            } else {
                UnauthorizedClassicSector.create(index)
            }
        }

        val card = ClassicCard.create(testTagId, testTime, sectors)

        assertEquals(16, card.sectors.size)
        assertTrue(card.getSector(0) is DataClassicSector)
        assertTrue(card.getSector(1) is UnauthorizedClassicSector)
    }

    @Test
    fun testDataClassicSectorReadBlocks() {
        val block0Data = ByteArray(16) { 0x00 }
        val block1Data = ByteArray(16) { 0x11 }
        val block2Data = ByteArray(16) { 0x22 }
        val trailerData = ByteArray(16) { 0xFF.toByte() }

        val blocks = listOf(
            ClassicBlock.create(ClassicBlock.TYPE_DATA, 0, block0Data),
            ClassicBlock.create(ClassicBlock.TYPE_DATA, 1, block1Data),
            ClassicBlock.create(ClassicBlock.TYPE_DATA, 2, block2Data),
            ClassicBlock.create(ClassicBlock.TYPE_TRAILER, 3, trailerData)
        )

        val sector = DataClassicSector(0, blocks)

        // Read single block
        val singleBlock = sector.readBlocks(0, 1)
        assertEquals(16, singleBlock.size)
        assertTrue(singleBlock.all { it == 0x00.toByte() })

        // Read multiple blocks
        val multipleBlocks = sector.readBlocks(1, 2)
        assertEquals(32, multipleBlocks.size)
        assertTrue(multipleBlocks.slice(0 until 16).all { it == 0x11.toByte() })
        assertTrue(multipleBlocks.slice(16 until 32).all { it == 0x22.toByte() })
    }

    @Test
    fun testDataClassicSectorGetBlock() {
        val block0Data = ByteArray(16) { (it + 1).toByte() }
        val blocks = listOf(
            ClassicBlock.create(ClassicBlock.TYPE_DATA, 0, block0Data),
            ClassicBlock.create(ClassicBlock.TYPE_DATA, 1, ByteArray(16)),
            ClassicBlock.create(ClassicBlock.TYPE_DATA, 2, ByteArray(16)),
            ClassicBlock.create(ClassicBlock.TYPE_TRAILER, 3, ByteArray(16))
        )

        val sector = DataClassicSector(0, blocks)
        val block = sector.getBlock(0)

        assertEquals(0, block.index)
        assertEquals(ClassicBlock.TYPE_DATA, block.type)
        assertTrue(block.data.contentEquals(block0Data))
    }

    @Test
    fun testClassicBlockIsEmpty() {
        val allZeroBlock = ClassicBlock.create(ClassicBlock.TYPE_DATA, 0, ByteArray(16) { 0x00 })
        assertTrue(allZeroBlock.isEmpty)

        val allFFBlock = ClassicBlock.create(ClassicBlock.TYPE_DATA, 0, ByteArray(16) { 0xFF.toByte() })
        assertTrue(allFFBlock.isEmpty)

        val mixedBlock = ClassicBlock.create(ClassicBlock.TYPE_DATA, 0, ByteArray(16) { it.toByte() })
        assertTrue(!mixedBlock.isEmpty)
    }

    @Test
    fun testAccessBitsParsing() {
        // Create a sector with a valid trailer block containing access bits
        // Trailer layout: KeyA (6 bytes) + Access bits (4 bytes) + KeyB (6 bytes)
        val trailerData = ByteArray(16).also {
            // KeyA (bytes 0-5)
            for (i in 0..5) it[i] = 0xFF.toByte()
            // Access bits (bytes 6-9)
            // Standard access bits: FF 07 80 69
            it[6] = 0xFF.toByte()
            it[7] = 0x07
            it[8] = 0x80.toByte()
            it[9] = 0x69
            // KeyB (bytes 10-15)
            for (i in 10..15) it[i] = 0xFF.toByte()
        }

        val blocks = listOf(
            ClassicBlock.create(ClassicBlock.TYPE_DATA, 0, ByteArray(16)),
            ClassicBlock.create(ClassicBlock.TYPE_DATA, 1, ByteArray(16)),
            ClassicBlock.create(ClassicBlock.TYPE_DATA, 2, ByteArray(16)),
            ClassicBlock.create(ClassicBlock.TYPE_TRAILER, 3, trailerData)
        )

        val sector = DataClassicSector(0, blocks)
        val accessBits = sector.accessBits

        assertNotNull(accessBits)
        // With standard transport configuration access bits:
        // C1=0, C2=0, C3=0 for blocks 0,1,2 (data blocks)
        // C1=0, C2=0, C3=1 for block 3 (trailer) - default config
        // Test that we can query slot access
        val slot0Access = accessBits.getSlot(0)
        val slot1Access = accessBits.getSlot(1)
        val slot2Access = accessBits.getSlot(2)
        val trailerAccess = accessBits.getSlot(3)

        // Data blocks should be readable with Key A in default config
        assertTrue(accessBits.isDataBlockReadable(0, useKeyB = false))
        assertTrue(accessBits.isDataBlockReadable(1, useKeyB = false))
        assertTrue(accessBits.isDataBlockReadable(2, useKeyB = false))
    }

    @Test
    fun testSectorWithNoTrailerHasNoAccessBits() {
        // Sector with no trailer block
        val blocks = listOf(
            ClassicBlock.create(ClassicBlock.TYPE_DATA, 0, ByteArray(16)),
            ClassicBlock.create(ClassicBlock.TYPE_DATA, 1, ByteArray(16)),
            ClassicBlock.create(ClassicBlock.TYPE_DATA, 2, ByteArray(16))
        )

        val sector = DataClassicSector(0, blocks)
        assertNull(sector.accessBits)
    }

    @Test
    fun testLargeSectorStructure() {
        // MIFARE Classic 4K has sectors 32-39 with 16 blocks each
        val blockCount = 16
        val blocks = (0 until blockCount).map { blockIndex ->
            val type = when (blockIndex) {
                blockCount - 1 -> ClassicBlock.TYPE_TRAILER
                else -> ClassicBlock.TYPE_DATA
            }
            ClassicBlock.create(type, blockIndex, ByteArray(16) { blockIndex.toByte() })
        }

        val largeSector = DataClassicSector(32, blocks)

        assertEquals(32, largeSector.index)
        assertEquals(16, largeSector.blocks.size)

        // Can read all data blocks
        val allData = largeSector.readBlocks(0, blockCount - 1)
        assertEquals((blockCount - 1) * 16, allData.size)
    }

    @Test
    fun testClassicCardPartialRead() {
        val sectors = (0 until 16).map { index ->
            DataClassicSector(
                index,
                listOf(
                    ClassicBlock.create(ClassicBlock.TYPE_DATA, 0, ByteArray(16)),
                    ClassicBlock.create(ClassicBlock.TYPE_DATA, 1, ByteArray(16)),
                    ClassicBlock.create(ClassicBlock.TYPE_DATA, 2, ByteArray(16)),
                    ClassicBlock.create(ClassicBlock.TYPE_TRAILER, 3, ByteArray(16))
                )
            )
        }

        // Test with isPartialRead = true
        val partialCard = ClassicCard(testTagId, testTime, sectors, isPartialRead = true)
        assertTrue(partialCard.isPartialRead)

        // Test with isPartialRead = false (default)
        val fullCard = ClassicCard(testTagId, testTime, sectors)
        assertTrue(!fullCard.isPartialRead)
    }
}
