/*
 * FeliCaReaderTest.kt
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

package com.codebutler.farebot.card.felica

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [FeliCaReader], specifically the `onlyFirst` parameter
 * added as a workaround for an iOS CoreNFC multi-system bug.
 */
class FeliCaReaderTest {
    private val testTagId = ByteArray(8) { it.toByte() }
    private val testIdm = ByteArray(8) { (0x01 + it).toByte() }
    private val testPmm = ByteArray(8) { (0x10 + it).toByte() }
    private val testBlockData = ByteArray(16) { 0xAA.toByte() }

    /**
     * Fake adapter simulating a FeliCa tag with multiple system codes,
     * each having one service with one block.
     */
    private class FakeAdapter(
        private val idm: ByteArray,
        private val pmm: ByteArray,
        private val systemCodes: List<Int>,
        private val serviceCodes: Map<Int, List<Int>>,
        private val blockData: ByteArray,
    ) : FeliCaTagAdapter {
        val selectedSystems = mutableListOf<Int>()
        val readServiceCodes = mutableListOf<Int>()

        override fun getIDm(): ByteArray = idm

        override fun getSystemCodes(): List<Int> = systemCodes

        override fun selectSystem(systemCode: Int): ByteArray? {
            selectedSystems.add(systemCode)
            return pmm
        }

        override fun getServiceCodes(): List<Int> = serviceCodes[selectedSystems.last()] ?: emptyList()

        override fun readBlock(
            serviceCode: Int,
            blockAddr: Byte,
        ): ByteArray? {
            readServiceCodes.add(serviceCode)
            // Return data only for block 0; return null for block 1+ to end iteration
            return if (blockAddr.toInt() == 0) blockData else null
        }
    }

    @Test
    fun testDefaultReadsAllSystems() {
        val adapter =
            FakeAdapter(
                idm = testIdm,
                pmm = testPmm,
                systemCodes = listOf(0x0003, 0x8008),
                serviceCodes =
                    mapOf(
                        0x0003 to listOf(0x090F),
                        0x8008 to listOf(0x0117),
                    ),
                blockData = testBlockData,
            )

        val result = FeliCaReader.readTag(testTagId, adapter)

        assertEquals(2, result.systems.size)

        // Both systems should be fully read (not skipped)
        assertFalse(result.systems[0].skipped)
        assertFalse(result.systems[1].skipped)

        // Both systems should have services
        assertEquals(1, result.systems[0].services.size)
        assertEquals(1, result.systems[1].services.size)

        assertEquals(0x0003, result.systems[0].code)
        assertEquals(0x8008, result.systems[1].code)
    }

    @Test
    fun testOnlyFirstReadsFirstSystemOnly() {
        val adapter =
            FakeAdapter(
                idm = testIdm,
                pmm = testPmm,
                systemCodes = listOf(0x0003, 0x8008),
                serviceCodes =
                    mapOf(
                        0x0003 to listOf(0x090F),
                        0x8008 to listOf(0x0117),
                    ),
                blockData = testBlockData,
            )

        val result = FeliCaReader.readTag(testTagId, adapter, onlyFirst = true)

        assertEquals(2, result.systems.size)

        // First system should be fully read
        assertFalse(result.systems[0].skipped)
        assertEquals(1, result.systems[0].services.size)
        assertEquals(0x0003, result.systems[0].code)

        // Second system should be skipped with no services
        assertTrue(result.systems[1].skipped)
        assertEquals(0, result.systems[1].services.size)
        assertEquals(0x8008, result.systems[1].code)
    }

    @Test
    fun testOnlyFirstDoesNotCallAdapterForSkippedSystems() {
        val adapter =
            FakeAdapter(
                idm = testIdm,
                pmm = testPmm,
                systemCodes = listOf(0x0003, 0x8008, 0xFE00),
                serviceCodes =
                    mapOf(
                        0x0003 to listOf(0x090F),
                        0x8008 to listOf(0x0117),
                        0xFE00 to listOf(0x000B),
                    ),
                blockData = testBlockData,
            )

        FeliCaReader.readTag(testTagId, adapter, onlyFirst = true)

        // selectSystem is called for:
        // 1. PMm poll with first system code (0x0003)
        // 2. Select system 0x0003 for reading
        // 3. Re-select system 0x0003 before reading service 0x090F
        // It should NOT be called for systems 0x8008 or 0xFE00
        val selectCalls = adapter.selectedSystems
        assertTrue(selectCalls.none { it == 0x8008 })
        assertTrue(selectCalls.none { it == 0xFE00 })

        // readBlock should only be called for the first system's service
        assertTrue(adapter.readServiceCodes.all { it == 0x090F })
    }

    @Test
    fun testOnlyFirstWithSingleSystemBehavesLikeDefault() {
        val adapterDefault =
            FakeAdapter(
                idm = testIdm,
                pmm = testPmm,
                systemCodes = listOf(0x0003),
                serviceCodes = mapOf(0x0003 to listOf(0x090F)),
                blockData = testBlockData,
            )

        val adapterOnlyFirst =
            FakeAdapter(
                idm = testIdm,
                pmm = testPmm,
                systemCodes = listOf(0x0003),
                serviceCodes = mapOf(0x0003 to listOf(0x090F)),
                blockData = testBlockData,
            )

        val resultDefault = FeliCaReader.readTag(testTagId, adapterDefault)
        val resultOnlyFirst = FeliCaReader.readTag(testTagId, adapterOnlyFirst, onlyFirst = true)

        assertEquals(resultDefault.systems.size, resultOnlyFirst.systems.size)
        assertEquals(1, resultOnlyFirst.systems.size)
        assertFalse(resultOnlyFirst.systems[0].skipped)
        assertEquals(
            resultDefault.systems[0].services.size,
            resultOnlyFirst.systems[0].services.size,
        )
    }

    @Test
    fun testOnlyFirstWithThreeSystemsSkipsSecondAndThird() {
        val adapter =
            FakeAdapter(
                idm = testIdm,
                pmm = testPmm,
                systemCodes = listOf(0x0003, 0x8008, 0xFE00),
                serviceCodes =
                    mapOf(
                        0x0003 to listOf(0x090F),
                        0x8008 to listOf(0x0117),
                        0xFE00 to listOf(0x000B),
                    ),
                blockData = testBlockData,
            )

        val result = FeliCaReader.readTag(testTagId, adapter, onlyFirst = true)

        assertEquals(3, result.systems.size)

        // First system fully read
        assertFalse(result.systems[0].skipped)
        assertEquals(1, result.systems[0].services.size)

        // Second and third systems skipped
        assertTrue(result.systems[1].skipped)
        assertEquals(0, result.systems[1].services.size)

        assertTrue(result.systems[2].skipped)
        assertEquals(0, result.systems[2].services.size)
    }

    @Test
    fun testFelicaSystemSkippedFactory() {
        val system = FelicaSystem.skipped(0x8008)

        assertTrue(system.skipped)
        assertEquals(0x8008, system.code)
        assertEquals(0, system.services.size)
    }
}
