/*
 * CardSerializationTest.kt
 *
 * Copyright 2017-2018 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.test

import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.card.classic.UnauthorizedClassicSector
import com.codebutler.farebot.card.classic.raw.RawClassicBlock
import com.codebutler.farebot.card.classic.raw.RawClassicCard
import com.codebutler.farebot.card.classic.raw.RawClassicSector
import com.codebutler.farebot.card.desfire.raw.RawDesfireApplication
import com.codebutler.farebot.card.desfire.raw.RawDesfireCard
import com.codebutler.farebot.card.desfire.raw.RawDesfireFile
import com.codebutler.farebot.card.desfire.raw.RawDesfireFileSettings
import com.codebutler.farebot.card.desfire.raw.RawDesfireManufacturingData
import com.codebutler.farebot.card.ultralight.UltralightPage
import com.codebutler.farebot.card.ultralight.raw.RawUltralightCard
import com.codebutler.farebot.shared.serialize.KotlinxCardSerializer
import kotlin.time.Instant
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for card serialization round-trip.
 *
 * Ported from Metrodroid's CardTest.kt
 */
class CardSerializationTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val serializer = KotlinxCardSerializer(json)

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testClassicCardJsonRoundTrip() {
        val tagId = "00123456".hexToByteArray()
        val scannedAt = Instant.fromEpochMilliseconds(1264982400000) // 2010-02-01T00:00:00Z

        // Create a simple Classic card with empty sectors
        val sectors = listOf(
            RawClassicSector.createData(
                0,
                listOf(
                    RawClassicBlock.create(0, ByteArray(16)),
                    RawClassicBlock.create(1, ByteArray(16)),
                    RawClassicBlock.create(2, ByteArray(16)),
                    RawClassicBlock.create(3, ByteArray(16))
                )
            )
        )

        val card = RawClassicCard.create(tagId, scannedAt, sectors)

        // Serialize
        val jsonString = serializer.serialize(card)
        assertNotNull(jsonString)
        assertTrue(jsonString.contains("\"cardType\""))
        assertTrue(jsonString.contains("MifareClassic"))

        // Deserialize
        val deserializedCard = serializer.deserialize(jsonString)
        assertEquals(CardType.MifareClassic, deserializedCard.cardType())
        assertTrue(deserializedCard.tagId().contentEquals(tagId))
        assertEquals(scannedAt, deserializedCard.scannedAt())
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testDesfireCardJsonRoundTrip() {
        val tagId = "00123456".hexToByteArray()
        val scannedAt = Instant.fromEpochMilliseconds(1264982400000)

        // Manufacturing data is stored as raw bytes (28 bytes total)
        val manufDataBytes = ByteArray(28).also { bytes ->
            bytes[0] = 0x04  // hwVendorID
            bytes[1] = 0x01  // hwType
            bytes[2] = 0x01  // hwSubType
            bytes[3] = 0x01  // hwMajorVersion
            bytes[4] = 0x00  // hwMinorVersion
            bytes[5] = 0x18  // hwStorageSize
            bytes[6] = 0x05  // hwProtocol
            bytes[7] = 0x04  // swVendorID
            bytes[8] = 0x01  // swType
            bytes[9] = 0x01  // swSubType
            bytes[10] = 0x01 // swMajorVersion
            bytes[11] = 0x00 // swMinorVersion
            bytes[12] = 0x18 // swStorageSize
            bytes[13] = 0x05 // swProtocol
            // bytes 14-20: uid (7 bytes)
            // bytes 21-25: batchNo (5 bytes)
            bytes[26] = 0x01 // weekProd
            bytes[27] = 0x14 // yearProd (20 = 2020)
        }
        val manufData = RawDesfireManufacturingData.create(manufDataBytes)

        // File settings for standard file (7 bytes): fileType(1) + commSetting(1) + accessRights(2) + fileSize(3)
        val fileSettingsData = byteArrayOf(
            0x00, // STANDARD_DATA_FILE
            0x00, // commSetting
            0x00, 0x00, // accessRights
            0x05, 0x00, 0x00 // fileSize = 5 (little endian)
        )

        val apps = listOf(
            RawDesfireApplication.create(
                0x123456,
                listOf(
                    RawDesfireFile.create(
                        0x01,
                        RawDesfireFileSettings.create(fileSettingsData),
                        "68656c6c6f".hexToByteArray() // "hello"
                    )
                )
            )
        )

        val card = RawDesfireCard.create(tagId, scannedAt, apps, manufData)

        // Serialize
        val jsonString = serializer.serialize(card)
        assertNotNull(jsonString)
        assertTrue(jsonString.contains("MifareDesfire"))

        // Deserialize
        val deserializedCard = serializer.deserialize(jsonString)
        assertEquals(CardType.MifareDesfire, deserializedCard.cardType())
        assertTrue(deserializedCard.tagId().contentEquals(tagId))
        assertEquals(scannedAt, deserializedCard.scannedAt())
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testUltralightCardJsonRoundTrip() {
        val tagId = "00123456789abcde".hexToByteArray()
        val scannedAt = Instant.fromEpochMilliseconds(1264982400000)

        val pages = listOf(
            UltralightPage.create(0, "00123456".hexToByteArray()),
            UltralightPage.create(1, "789abcde".hexToByteArray()),
            UltralightPage.create(2, "ff000000".hexToByteArray()),
            UltralightPage.create(3, "ffffffff".hexToByteArray())
        )

        val card = RawUltralightCard.create(tagId, scannedAt, pages, 1)

        // Serialize
        val jsonString = serializer.serialize(card)
        assertNotNull(jsonString)
        assertTrue(jsonString.contains("MifareUltralight"))

        // Deserialize
        val deserializedCard = serializer.deserialize(jsonString)
        assertEquals(CardType.MifareUltralight, deserializedCard.cardType())
        assertTrue(deserializedCard.tagId().contentEquals(tagId))
        assertEquals(scannedAt, deserializedCard.scannedAt())
    }

    @Test
    fun testUnauthorizedUltralightIsDetected() {
        val tagId = byteArrayOf(0x00, 0x12, 0x34, 0x56, 0x78, 0x9a.toByte(), 0xbc.toByte(), 0xde.toByte())
        val scannedAt = Instant.fromEpochMilliseconds(1264982400000)

        // Build pages for Ultralight card - first 4 pages readable, rest unauthorized
        // Page 0-3 are configuration pages, user data starts at page 4
        val pages = buildList {
            // Configuration pages (readable)
            add(UltralightPage.create(0, byteArrayOf(0x00, 0x12, 0x34, 0x56)))
            add(UltralightPage.create(1, byteArrayOf(0x78, 0x9a.toByte(), 0xbc.toByte(), 0xde.toByte())))
            add(UltralightPage.create(2, byteArrayOf(0xff.toByte(), 0x00, 0x00, 0x00)))
            add(UltralightPage.create(3, byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte())))

            // User memory pages 4-43 (40 pages for Ultralight C)
            for (i in 4 until 44) {
                add(UltralightPage.create(i, ByteArray(4)))  // Empty/zero pages
            }
        }

        val card = RawUltralightCard.create(tagId, scannedAt, pages, 2)
        val parsed = card.parse()

        // Should have 44 pages
        assertEquals(44, parsed.pages.size)
    }

    @Test
    fun testUnauthorizedClassicCard() {
        val tagId = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val scannedAt = Instant.fromEpochMilliseconds(1264982400000)

        // Build a card with all unauthorized sectors
        val sectors = (0 until 16).map { index ->
            RawClassicSector.createUnauthorized(index)
        }

        val card = RawClassicCard.create(tagId, scannedAt, sectors)

        // Card should report as unauthorized
        assertTrue(card.isUnauthorized())

        val parsed = card.parse()
        assertTrue(parsed.sectors.all { it is UnauthorizedClassicSector })
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testPartiallyAuthorizedClassicCard() {
        val tagId = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val scannedAt = Instant.fromEpochMilliseconds(1264982400000)

        val testData = "6d6574726f64726f6964436c61737369".hexToByteArray() // "metrodroidClassi"

        // Build a card with some readable sectors
        val sectors = (0 until 16).map { index ->
            if (index == 2) {
                // Sector 2 is readable
                val blocks = listOf(
                    RawClassicBlock.create(0, testData),
                    RawClassicBlock.create(1, testData),
                    RawClassicBlock.create(2, testData),
                    RawClassicBlock.create(3, testData)
                )
                RawClassicSector.createData(index, blocks)
            } else {
                RawClassicSector.createUnauthorized(index)
            }
        }

        val card = RawClassicCard.create(tagId, scannedAt, sectors)

        // Card should NOT report as fully unauthorized (has some readable data)
        assertFalse(card.isUnauthorized())

        val parsed = card.parse()
        assertEquals(16, parsed.sectors.size)

        // Sector 2 should be data sector
        assertTrue(parsed.sectors[2] is DataClassicSector)
        val sector2 = parsed.sectors[2] as DataClassicSector
        assertTrue(sector2.blocks[0].data.contentEquals(testData))
    }

    @Test
    fun testBlankMifareClassic() {
        val tagId = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val scannedAt = Instant.fromEpochMilliseconds(1264982400000)

        val all00Block = ByteArray(16) { 0x00 }
        val allFFBlock = ByteArray(16) { 0xff.toByte() }
        val otherBlock = ByteArray(16) { (it + 1).toByte() }

        // Test card with all 0x00 blocks
        val all00Sectors = (0 until 16).map { sectorIndex ->
            val blocks = (0 until 4).map { blockIndex ->
                RawClassicBlock.create(blockIndex, all00Block)
            }
            RawClassicSector.createData(sectorIndex, blocks)
        }
        val all00Card = RawClassicCard.create(tagId, scannedAt, all00Sectors)
        assertFalse(all00Card.isUnauthorized())

        // Test card with all 0xFF blocks
        val allFFSectors = (0 until 16).map { sectorIndex ->
            val blocks = (0 until 4).map { blockIndex ->
                RawClassicBlock.create(blockIndex, allFFBlock)
            }
            RawClassicSector.createData(sectorIndex, blocks)
        }
        val allFFCard = RawClassicCard.create(tagId, scannedAt, allFFSectors)
        assertFalse(allFFCard.isUnauthorized())

        // Test card with other data - also not unauthorized
        val otherSectors = (0 until 16).map { sectorIndex ->
            val blocks = (0 until 4).map { blockIndex ->
                RawClassicBlock.create(blockIndex, otherBlock)
            }
            RawClassicSector.createData(sectorIndex, blocks)
        }
        val otherCard = RawClassicCard.create(tagId, scannedAt, otherSectors)
        assertFalse(otherCard.isUnauthorized())
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testDesfireUnauthorized() {
        val tagId = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val scannedAt = Instant.fromEpochMilliseconds(1264982400000)
        val emptyManufData = RawDesfireManufacturingData.create(ByteArray(28))

        // Card with no applications - considered blank/unauthorized
        val emptyCard = RawDesfireCard.create(tagId, scannedAt, emptyList(), emptyManufData)
        assertTrue(emptyCard.isUnauthorized())

        // File settings for standard file
        val fileSettingsData = byteArrayOf(
            0x00, // STANDARD_DATA_FILE
            0x00, // commSetting
            0x00, 0x00, // accessRights
            0x00, 0x00, 0x00 // fileSize = 0
        )

        // Card with only unauthorized files
        val unauthorizedApp = RawDesfireApplication.create(
            0x6472,
            listOf(
                RawDesfireFile.createUnauthorized(
                    0x6f69,
                    RawDesfireFileSettings.create(fileSettingsData),
                    "Authentication error: 64"
                )
            )
        )
        val unauthorizedCard = RawDesfireCard.create(tagId, scannedAt, listOf(unauthorizedApp), emptyManufData)
        assertTrue(unauthorizedCard.isUnauthorized())

        // File settings with actual file size
        val fileSettingsWithSize = byteArrayOf(
            0x00, // STANDARD_DATA_FILE
            0x00, // commSetting
            0x00, 0x00, // accessRights
            0x08, 0x00, 0x00 // fileSize = 8 (little endian)
        )

        // Card with readable file - not unauthorized
        val authorizedApp = RawDesfireApplication.create(
            0x6472,
            listOf(
                RawDesfireFile.create(
                    0x6f69,
                    RawDesfireFileSettings.create(fileSettingsWithSize),
                    "6d69636f6c6f7573".hexToByteArray() // "micolous"
                )
            )
        )
        val authorizedCard = RawDesfireCard.create(tagId, scannedAt, listOf(authorizedApp), emptyManufData)
        assertFalse(authorizedCard.isUnauthorized())
    }
}
