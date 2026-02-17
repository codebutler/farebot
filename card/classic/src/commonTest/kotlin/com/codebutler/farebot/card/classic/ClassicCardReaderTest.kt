/*
 * ClassicCardReaderTest.kt
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

package com.codebutler.farebot.card.classic

import com.codebutler.farebot.card.CardLostException
import com.codebutler.farebot.card.classic.key.ClassicCardKeys
import com.codebutler.farebot.card.classic.raw.RawClassicSector
import com.codebutler.farebot.card.nfc.ClassicTechnology
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for ClassicCardReader retry logic, key tracking, and CardLostException handling.
 */
class ClassicCardReaderTest {
    private val testTagId = byteArrayOf(0x01, 0x02, 0x03, 0x04)

    /**
     * Mock ClassicTechnology that allows configuring authentication results and block read behavior.
     */
    private class MockClassicTechnology(
        override val sectorCount: Int = 1,
        private val blocksPerSector: Int = 4,
        private val authKeyAResult: (sectorIndex: Int, key: ByteArray) -> Boolean = { _, _ -> true },
        private val authKeyBResult: (sectorIndex: Int, key: ByteArray) -> Boolean = { _, _ -> false },
        private val readBlockResult: (blockIndex: Int) -> ByteArray = { ByteArray(16) },
    ) : ClassicTechnology {
        override val isConnected: Boolean = true

        val authKeyACalls = mutableListOf<Pair<Int, ByteArray>>()
        val authKeyBCalls = mutableListOf<Pair<Int, ByteArray>>()
        val readBlockCalls = mutableListOf<Int>()

        override suspend fun authenticateSectorWithKeyA(
            sectorIndex: Int,
            key: ByteArray,
        ): Boolean {
            authKeyACalls.add(sectorIndex to key)
            return authKeyAResult(sectorIndex, key)
        }

        override suspend fun authenticateSectorWithKeyB(
            sectorIndex: Int,
            key: ByteArray,
        ): Boolean {
            authKeyBCalls.add(sectorIndex to key)
            return authKeyBResult(sectorIndex, key)
        }

        override suspend fun readBlock(blockIndex: Int): ByteArray {
            readBlockCalls.add(blockIndex)
            return readBlockResult(blockIndex)
        }

        override fun sectorToBlock(sectorIndex: Int): Int = sectorIndex * blocksPerSector

        override fun getBlockCountInSector(sectorIndex: Int): Int = blocksPerSector

        override fun connect() {}

        override fun close() {}
    }

    @Test
    fun testNormalReadWithDefaultKey() =
        runTest {
            val blockData = ByteArray(16) { 0xAB.toByte() }
            val tech =
                MockClassicTechnology(
                    sectorCount = 1,
                    readBlockResult = { blockData },
                )

            val result = ClassicCardReader.readCard(testTagId, tech, null)
            val sectors = result.sectors()

            assertEquals(1, sectors.size)
            assertEquals(RawClassicSector.TYPE_DATA, sectors[0].type)
            assertEquals(4, sectors[0].blocks!!.size)
            assertTrue(sectors[0].blocks!![0].data.contentEquals(blockData))
        }

    @Test
    fun testUnauthorizedSectorWhenAuthFails() =
        runTest {
            val tech =
                MockClassicTechnology(
                    sectorCount = 1,
                    authKeyAResult = { _, _ -> false },
                    authKeyBResult = { _, _ -> false },
                )

            val result = ClassicCardReader.readCard(testTagId, tech, null)
            val sectors = result.sectors()

            assertEquals(1, sectors.size)
            assertEquals(RawClassicSector.TYPE_UNAUTHORIZED, sectors[0].type)
        }

    @Test
    fun testRetryOnSingleByteRead() =
        runTest {
            var readCount = 0
            val normalData = ByteArray(16) { 0xCC.toByte() }
            val tech =
                MockClassicTechnology(
                    sectorCount = 1,
                    blocksPerSector = 1,
                    readBlockResult = {
                        readCount++
                        if (readCount == 1) {
                            // First read returns single byte (the 0x04 error condition)
                            byteArrayOf(0x04)
                        } else {
                            normalData
                        }
                    },
                )

            val result = ClassicCardReader.readCard(testTagId, tech, null)
            val sectors = result.sectors()

            assertEquals(1, sectors.size)
            assertEquals(RawClassicSector.TYPE_DATA, sectors[0].type)
            // The block should contain the retried (normal) data
            assertTrue(sectors[0].blocks!![0].data.contentEquals(normalData))
            // readBlock called twice: initial + 1 retry
            assertEquals(2, tech.readBlockCalls.size)
            // Should have reauthenticated before retry
            assertTrue(tech.authKeyACalls.size > 1)
        }

    @Test
    fun testRetryExhaustedKeepsSingleByteData() =
        runTest {
            val singleByte = byteArrayOf(0x04)
            val tech =
                MockClassicTechnology(
                    sectorCount = 1,
                    blocksPerSector = 1,
                    readBlockResult = { singleByte },
                )

            val result = ClassicCardReader.readCard(testTagId, tech, null)
            val sectors = result.sectors()

            assertEquals(1, sectors.size)
            assertEquals(RawClassicSector.TYPE_DATA, sectors[0].type)
            // After 3 retries all returning single byte, the block still gets saved
            assertEquals(1, sectors[0].blocks!![0].data.size)
            // Initial read + 3 retries = 4 total reads
            assertEquals(4, tech.readBlockCalls.size)
        }

    @Test
    fun testCardLostExceptionReturnsPartialData() =
        runTest {
            var sectorReadCount = 0
            val tech =
                MockClassicTechnology(
                    sectorCount = 4,
                    blocksPerSector = 1,
                    readBlockResult = { blockIndex ->
                        sectorReadCount++
                        if (sectorReadCount > 2) {
                            throw CardLostException("removed")
                        }
                        ByteArray(16) { blockIndex.toByte() }
                    },
                )

            val result = ClassicCardReader.readCard(testTagId, tech, null)
            val sectors = result.sectors()

            // Should have partial data: 2 successful sectors + 1 invalid (where loss occurred)
            // Remaining sectors are not attempted
            assertTrue(sectors.size < 4)
            // The last sector should be marked invalid due to CardLostException
            val lastSector = sectors.last()
            assertEquals(RawClassicSector.TYPE_INVALID, lastSector.type)
            assertTrue(lastSector.errorMessage!!.contains("Tag was lost"))
        }

    @Test
    fun testKeyBFallbackAndRetryUsesKeyB() =
        runTest {
            val keyA = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06)
            val keyB = byteArrayOf(0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F)
            val cardKeys = ClassicCardKeys.fromProxmark3(keyA + keyB)

            var readCount = 0
            val normalData = ByteArray(16) { 0xDD.toByte() }

            val tech =
                MockClassicTechnology(
                    sectorCount = 1,
                    blocksPerSector = 1,
                    // All Key A attempts fail, Key B succeeds
                    authKeyAResult = { _, _ -> false },
                    authKeyBResult = { _, key -> key.contentEquals(keyB) },
                    readBlockResult = {
                        readCount++
                        if (readCount == 1) {
                            // Trigger retry
                            byteArrayOf(0x04)
                        } else {
                            normalData
                        }
                    },
                )

            val result = ClassicCardReader.readCard(testTagId, tech, cardKeys)
            val sectors = result.sectors()

            assertEquals(1, sectors.size)
            assertEquals(RawClassicSector.TYPE_DATA, sectors[0].type)
            assertTrue(sectors[0].blocks!![0].data.contentEquals(normalData))

            // Retry reauthentication should use Key B, not Key A
            // The last auth call on Key B should be the retry reauthentication
            assertTrue(tech.authKeyBCalls.size >= 2, "Expected at least 2 Key B auth calls (initial + retry)")
        }

    @Test
    fun testMultipleSectorsWithMixedAuth() =
        runTest {
            val tech =
                MockClassicTechnology(
                    sectorCount = 3,
                    blocksPerSector = 1,
                    authKeyAResult = { sectorIndex, _ ->
                        // Only sector 0 authenticates with default key
                        sectorIndex == 0
                    },
                    readBlockResult = { ByteArray(16) },
                )

            val result = ClassicCardReader.readCard(testTagId, tech, null)
            val sectors = result.sectors()

            assertEquals(3, sectors.size)
            // Sector 0: authenticated with default key → data
            assertEquals(RawClassicSector.TYPE_DATA, sectors[0].type)
            // Sectors 1, 2: auth failed → unauthorized
            assertEquals(RawClassicSector.TYPE_UNAUTHORIZED, sectors[1].type)
            assertEquals(RawClassicSector.TYPE_UNAUTHORIZED, sectors[2].type)
        }

    @Test
    fun testGlobalKeysUsedWhenCardKeysFail() =
        runTest {
            val globalKey =
                byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte())
            val blockData = ByteArray(16) { 0x42 }

            val tech =
                MockClassicTechnology(
                    sectorCount = 1,
                    blocksPerSector = 1,
                    authKeyAResult = { _, key ->
                        // Only the global key works
                        key.contentEquals(globalKey)
                    },
                    readBlockResult = { blockData },
                )

            val result = ClassicCardReader.readCard(testTagId, tech, null, globalKeys = listOf(globalKey))
            val sectors = result.sectors()

            assertEquals(1, sectors.size)
            assertEquals(RawClassicSector.TYPE_DATA, sectors[0].type)
            assertTrue(sectors[0].blocks!![0].data.contentEquals(blockData))
            // Default keys should have been tried and failed, then global key succeeded
            assertTrue(tech.authKeyACalls.any { it.second.contentEquals(globalKey) })
        }

    @Test
    fun testGenericExceptionCreatesInvalidSector() =
        runTest {
            val tech =
                MockClassicTechnology(
                    sectorCount = 2,
                    blocksPerSector = 1,
                    readBlockResult = { blockIndex ->
                        if (blockIndex == 0) {
                            ByteArray(16)
                        } else {
                            throw RuntimeException("I/O error")
                        }
                    },
                )

            val result = ClassicCardReader.readCard(testTagId, tech, null)
            val sectors = result.sectors()

            assertEquals(2, sectors.size)
            // Sector 0 reads fine
            assertEquals(RawClassicSector.TYPE_DATA, sectors[0].type)
            // Sector 1 hits exception → invalid (not a CardLostException, so reading continues)
            assertEquals(RawClassicSector.TYPE_INVALID, sectors[1].type)
            assertTrue(sectors[1].errorMessage!!.contains("I/O error"))
        }
}
