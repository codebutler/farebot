/*
 * ClassicCardKeysTest.kt
 *
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.card.classic.key

import com.codebutler.farebot.card.CardType
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for MIFARE Classic key handling.
 *
 * Based on Metrodroid's ImportKeysTest.kt but adapted for FareBot's
 * simpler key infrastructure.
 */
@OptIn(ExperimentalStdlibApi::class)
class ClassicCardKeysTest {
    @Test
    fun testStaticKeys() {
        // Test well-known static keys
        val defaultKey = ClassicStaticKeys.KEY_DEFAULT
        assertEquals(6, defaultKey.size, "Default key should be 6 bytes")
        assertTrue(defaultKey.all { it == 0xFF.toByte() }, "Default key should be all 0xFF")

        val zeroKey = ClassicStaticKeys.KEY_ZERO
        assertEquals(6, zeroKey.size, "Zero key should be 6 bytes")
        assertTrue(zeroKey.all { it == 0x00.toByte() }, "Zero key should be all 0x00")

        val madKey = ClassicStaticKeys.KEY_MAD
        assertEquals(6, madKey.size, "MAD key should be 6 bytes")
        assertContentEquals("A0A1A2A3A4A5".hexToByteArray(), madKey, "MAD key should match expected value")

        val ndefKey = ClassicStaticKeys.KEY_NFC_FORUM
        assertEquals(6, ndefKey.size, "NDEF key should be 6 bytes")
        assertContentEquals("D3F7D3F7D3F7".hexToByteArray(), ndefKey, "NDEF key should match expected value")
    }

    @Test
    fun testWellKnownKeys() {
        val wellKnownKeys = ClassicStaticKeys.getWellKnownKeys()
        assertEquals(4, wellKnownKeys.size, "Should have 4 well-known keys")

        // Verify all keys are 6 bytes
        for (key in wellKnownKeys) {
            assertEquals(6, key.size, "Each well-known key should be 6 bytes")
        }
    }

    @Test
    fun testDefaultKeysForSectorCount() {
        val keys = ClassicStaticKeys.defaultKeysForSectorCount(16)

        assertEquals(CardType.MifareClassic, keys.cardType(), "Card type should be MifareClassic")
        assertEquals(16, keys.keys.size, "Should have 16 sectors")

        // Verify all sectors have default keys
        for (sectorNum in 0 until 16) {
            val sectorKey = keys.keyForSector(sectorNum)
            assertNotNull(sectorKey, "Sector $sectorNum should have a key")
            assertContentEquals(ClassicStaticKeys.KEY_DEFAULT, sectorKey.keyA, "KeyA should be default")
            assertContentEquals(ClassicStaticKeys.KEY_DEFAULT, sectorKey.keyB, "KeyB should be default")
        }

        // Verify out of range returns null
        assertNull(keys.keyForSector(16), "Sector 16 should be out of range for 16-sector card")
        assertNull(keys.keyForSector(100), "Sector 100 should be out of range")
    }

    @Test
    fun testFromProxmark3() {
        // Create a proxmark3 binary key dump
        // Format: [KeyA sector 0][KeyA sector 1]...[KeyB sector 0][KeyB sector 1]...
        // Each key is 6 bytes
        val numSectors = 4
        val keyDump = ByteArray(numSectors * 6 * 2)

        // Set up test keys
        val keyA0 = "000000000000".hexToByteArray() // Null key
        val keyA1 = "FFFFFFFFFFFF".hexToByteArray() // Default key
        val keyA2 = "A0A1A2A3A4A5".hexToByteArray() // MAD key
        val keyA3 = "D3F7D3F7D3F7".hexToByteArray() // NDEF key

        val keyB0 = "112233445566".hexToByteArray()
        val keyB1 = "AABBCCDDEEFF".hexToByteArray()
        val keyB2 = "010203040506".hexToByteArray()
        val keyB3 = "FEFDFCFBFAF9".hexToByteArray()

        // Fill KeyA section (first half)
        keyA0.copyInto(keyDump, 0 * 6)
        keyA1.copyInto(keyDump, 1 * 6)
        keyA2.copyInto(keyDump, 2 * 6)
        keyA3.copyInto(keyDump, 3 * 6)

        // Fill KeyB section (second half)
        keyB0.copyInto(keyDump, 4 * 6)
        keyB1.copyInto(keyDump, 5 * 6)
        keyB2.copyInto(keyDump, 6 * 6)
        keyB3.copyInto(keyDump, 7 * 6)

        val keys = ClassicCardKeys.fromProxmark3(keyDump)

        assertEquals(CardType.MifareClassic, keys.cardType(), "Card type should be MifareClassic")
        assertEquals(4, keys.keys.size, "Should have 4 sectors")

        // Verify sector 0
        val key0 = keys.keyForSector(0)
        assertNotNull(key0)
        assertContentEquals(keyA0, key0.keyA, "Sector 0 KeyA should match")
        assertContentEquals(keyB0, key0.keyB, "Sector 0 KeyB should match")

        // Verify sector 1
        val key1 = keys.keyForSector(1)
        assertNotNull(key1)
        assertContentEquals(keyA1, key1.keyA, "Sector 1 KeyA should be default key")
        assertContentEquals(keyB1, key1.keyB, "Sector 1 KeyB should match")

        // Verify sector 2
        val key2 = keys.keyForSector(2)
        assertNotNull(key2)
        assertContentEquals(keyA2, key2.keyA, "Sector 2 KeyA should be MAD key")
        assertContentEquals(keyB2, key2.keyB, "Sector 2 KeyB should match")

        // Verify sector 3
        val key3 = keys.keyForSector(3)
        assertNotNull(key3)
        assertContentEquals(keyA3, key3.keyA, "Sector 3 KeyA should be NDEF key")
        assertContentEquals(keyB3, key3.keyB, "Sector 3 KeyB should match")
    }

    @Test
    fun testFromProxmark3_16Sectors() {
        // Test with a full 16-sector (1K) card dump
        val numSectors = 16
        val keyDump = ByteArray(numSectors * 6 * 2)

        // Fill with default keys
        val defaultKey = "FFFFFFFFFFFF".hexToByteArray()
        for (i in 0 until numSectors) {
            defaultKey.copyInto(keyDump, i * 6) // KeyA
            defaultKey.copyInto(keyDump, (i + numSectors) * 6) // KeyB
        }

        val keys = ClassicCardKeys.fromProxmark3(keyDump)
        assertEquals(16, keys.keys.size, "Should have 16 sectors for 1K card")

        // Verify all sectors
        for (sectorNum in 0 until 16) {
            val key = keys.keyForSector(sectorNum)
            assertNotNull(key)
            assertContentEquals(defaultKey, key.keyA, "Sector $sectorNum KeyA should be default")
            assertContentEquals(defaultKey, key.keyB, "Sector $sectorNum KeyB should be default")
        }
    }

    @Test
    fun testSectorKeyCreate() {
        val keyA = "010203040506".hexToByteArray()
        val keyB = "FFEEDDCCBBAA".hexToByteArray()

        val sectorKey = ClassicSectorKey.create(keyA, keyB)

        assertContentEquals(keyA, sectorKey.keyA, "KeyA should match")
        assertContentEquals(keyB, sectorKey.keyB, "KeyB should match")
    }

    @Test
    fun testKeyForSectorOutOfRange() {
        val keys = ClassicStaticKeys.defaultKeysForSectorCount(16)

        // Valid range: 0-15
        assertNotNull(keys.keyForSector(0), "Sector 0 should be valid")
        assertNotNull(keys.keyForSector(15), "Sector 15 should be valid")

        // Invalid range (greater than max)
        assertNull(keys.keyForSector(16), "Sector 16 should be out of range")
        assertNull(keys.keyForSector(100), "Sector 100 should be out of range")
    }
}
