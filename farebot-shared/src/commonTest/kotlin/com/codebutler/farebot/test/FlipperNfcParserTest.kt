/*
 * FlipperNfcParserTest.kt
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

package com.codebutler.farebot.test

import com.codebutler.farebot.card.classic.raw.RawClassicCard
import com.codebutler.farebot.card.classic.raw.RawClassicSector
import com.codebutler.farebot.card.ultralight.raw.RawUltralightCard
import com.codebutler.farebot.shared.serialize.FlipperNfcParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FlipperNfcParserTest {

    @Test
    fun testIsFlipperFormat_valid() {
        assertTrue(FlipperNfcParser.isFlipperFormat("Filetype: Flipper NFC device\nVersion: 4"))
    }

    @Test
    fun testIsFlipperFormat_withLeadingWhitespace() {
        assertTrue(FlipperNfcParser.isFlipperFormat("  Filetype: Flipper NFC device\nVersion: 4"))
    }

    @Test
    fun testIsFlipperFormat_jsonNotMatched() {
        assertFalse(FlipperNfcParser.isFlipperFormat("""{"cardType": "MifareClassic"}"""))
    }

    @Test
    fun testIsFlipperFormat_xmlNotMatched() {
        assertFalse(FlipperNfcParser.isFlipperFormat("<?xml version=\"1.0\"?>"))
    }

    @Test
    fun testParseClassic1K() {
        val dump = buildString {
            appendLine("Filetype: Flipper NFC device")
            appendLine("Version: 4")
            appendLine("Device type: Mifare Classic")
            appendLine("UID: BA E2 7C 9D")
            appendLine("ATQA: 00 02")
            appendLine("SAK: 18")
            appendLine("Mifare Classic type: 1K")
            appendLine("Data format version: 2")
            // 16 sectors * 4 blocks = 64 blocks
            for (block in 0 until 64) {
                if (block == 0) {
                    appendLine("Block 0: BA E2 7C 9D B9 18 02 00 46 44 53 37 30 56 30 31")
                } else {
                    appendLine("Block $block: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00")
                }
            }
        }

        val result = FlipperNfcParser.parse(dump)
        assertNotNull(result)
        assertIs<RawClassicCard>(result)

        // Verify UID
        assertEquals(0xBA.toByte(), result.tagId()[0])
        assertEquals(0xE2.toByte(), result.tagId()[1])
        assertEquals(0x7C.toByte(), result.tagId()[2])
        assertEquals(0x9D.toByte(), result.tagId()[3])

        // Verify sectors
        val sectors = result.sectors()
        assertEquals(16, sectors.size)

        // Verify first block data
        val firstSector = sectors[0]
        assertEquals(RawClassicSector.TYPE_DATA, firstSector.type)
        assertNotNull(firstSector.blocks)
        assertEquals(4, firstSector.blocks!!.size)
        assertEquals(0xBA.toByte(), firstSector.blocks!![0].data[0])
    }

    @Test
    fun testParseClassic4K() {
        val dump = buildString {
            appendLine("Filetype: Flipper NFC device")
            appendLine("Version: 4")
            appendLine("Device type: Mifare Classic")
            appendLine("UID: 01 02 03 04")
            appendLine("ATQA: 00 02")
            appendLine("SAK: 18")
            appendLine("Mifare Classic type: 4K")
            appendLine("Data format version: 2")
            // Sectors 0-31: 4 blocks each = 128 blocks
            for (block in 0 until 128) {
                appendLine("Block $block: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00")
            }
            // Sectors 32-39: 16 blocks each = 128 blocks
            for (block in 128 until 256) {
                appendLine("Block $block: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00")
            }
        }

        val result = FlipperNfcParser.parse(dump)
        assertNotNull(result)
        assertIs<RawClassicCard>(result)

        val sectors = result.sectors()
        assertEquals(40, sectors.size)

        // Verify extended sectors (32-39) have 16 blocks
        for (sectorIndex in 32 until 40) {
            val sector = sectors[sectorIndex]
            assertEquals(RawClassicSector.TYPE_DATA, sector.type)
            assertNotNull(sector.blocks)
            assertEquals(16, sector.blocks!!.size)
        }
    }

    @Test
    fun testParseClassicUnauthorizedSectors() {
        val dump = buildString {
            appendLine("Filetype: Flipper NFC device")
            appendLine("Version: 4")
            appendLine("Device type: Mifare Classic")
            appendLine("UID: 01 02 03 04")
            appendLine("ATQA: 00 02")
            appendLine("SAK: 08")
            appendLine("Mifare Classic type: 1K")
            appendLine("Data format version: 2")
            // Sector 0: readable
            appendLine("Block 0: 01 02 03 04 B9 18 02 00 46 44 53 37 30 56 30 31")
            appendLine("Block 1: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00")
            appendLine("Block 2: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00")
            appendLine("Block 3: 00 00 00 00 00 00 FF 07 80 69 FF FF FF FF FF FF")
            // Sectors 1-15: all unread
            for (block in 4 until 64) {
                appendLine("Block $block: ?? ?? ?? ?? ?? ?? ?? ?? ?? ?? ?? ?? ?? ?? ?? ??")
            }
        }

        val result = FlipperNfcParser.parse(dump)
        assertNotNull(result)
        assertIs<RawClassicCard>(result)

        val sectors = result.sectors()
        assertEquals(16, sectors.size)

        // Sector 0 should be data
        assertEquals(RawClassicSector.TYPE_DATA, sectors[0].type)

        // Sectors 1-15 should be unauthorized
        for (i in 1 until 16) {
            assertEquals(RawClassicSector.TYPE_UNAUTHORIZED, sectors[i].type)
        }
    }

    @Test
    fun testParseClassicMixedUnreadBytes() {
        val dump = buildString {
            appendLine("Filetype: Flipper NFC device")
            appendLine("Version: 4")
            appendLine("Device type: Mifare Classic")
            appendLine("UID: 01 02 03 04")
            appendLine("ATQA: 00 02")
            appendLine("SAK: 08")
            appendLine("Mifare Classic type: 1K")
            appendLine("Data format version: 2")
            // Sector 0: block with mixed ?? bytes
            appendLine("Block 0: 01 02 ?? 04 ?? 18 02 00 46 44 53 37 30 56 30 31")
            appendLine("Block 1: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00")
            appendLine("Block 2: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00")
            appendLine("Block 3: 00 00 00 00 00 00 FF 07 80 69 FF FF FF FF FF FF")
            // Rest: all unread
            for (block in 4 until 64) {
                appendLine("Block $block: ?? ?? ?? ?? ?? ?? ?? ?? ?? ?? ?? ?? ?? ?? ?? ??")
            }
        }

        val result = FlipperNfcParser.parse(dump)
        assertNotNull(result)
        assertIs<RawClassicCard>(result)

        val sectors = result.sectors()
        // Sector 0 has readable blocks, so it should be data
        assertEquals(RawClassicSector.TYPE_DATA, sectors[0].type)

        // Verify ?? bytes become 0x00
        val block0 = sectors[0].blocks!![0]
        assertEquals(0x01.toByte(), block0.data[0])
        assertEquals(0x02.toByte(), block0.data[1])
        assertEquals(0x00.toByte(), block0.data[2]) // was ??
        assertEquals(0x04.toByte(), block0.data[3])
        assertEquals(0x00.toByte(), block0.data[4]) // was ??
    }

    @Test
    fun testParseUltralight() {
        val dump = buildString {
            appendLine("Filetype: Flipper NFC device")
            appendLine("Version: 4")
            appendLine("Device type: NTAG/Ultralight")
            appendLine("UID: 04 A1 B2 C3 D4 E5 F6")
            appendLine("ATQA: 44 00")
            appendLine("SAK: 00")
            appendLine("NTAG/Ultralight type: NTAG213")
            appendLine("Data format version: 2")
            appendLine("Signature: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00")
            appendLine("Mifare version: 00 04 04 02 01 00 0F 03")
            appendLine("Counter 0: 0")
            appendLine("Tearing 0: 00")
            appendLine("Counter 1: 0")
            appendLine("Tearing 1: 00")
            appendLine("Counter 2: 0")
            appendLine("Tearing 2: 00")
            appendLine("Pages total: 45")
            for (page in 0 until 45) {
                when (page) {
                    0 -> appendLine("Page 0: 04 A1 B2 C3")
                    1 -> appendLine("Page 1: D4 E5 F6 80")
                    else -> appendLine("Page $page: 00 00 00 00")
                }
            }
        }

        val result = FlipperNfcParser.parse(dump)
        assertNotNull(result)
        assertIs<RawUltralightCard>(result)

        // Verify UID
        assertEquals(0x04.toByte(), result.tagId()[0])
        assertEquals(0xA1.toByte(), result.tagId()[1])

        // Verify pages
        assertEquals(45, result.pages.size)
        assertEquals(0, result.pages[0].index)
        assertEquals(0x04.toByte(), result.pages[0].data[0])

        // Verify type (NTAG213 = 2)
        assertEquals(2, result.ultralightType)
    }

    @Test
    fun testParseUnsupportedDeviceType() {
        val dump = buildString {
            appendLine("Filetype: Flipper NFC device")
            appendLine("Version: 4")
            appendLine("Device type: Mifare DESFire")
            appendLine("UID: 01 02 03 04 05 06 07")
        }

        val result = FlipperNfcParser.parse(dump)
        assertNull(result)
    }

    @Test
    fun testParseMalformedInput() {
        assertNull(FlipperNfcParser.parse(""))
        assertNull(FlipperNfcParser.parse("just some random text"))
        assertNull(FlipperNfcParser.parse("Filetype: Flipper NFC device\n"))
    }

    @Test
    fun testParseMissingUID() {
        val dump = buildString {
            appendLine("Filetype: Flipper NFC device")
            appendLine("Version: 4")
            appendLine("Device type: Mifare Classic")
            appendLine("Mifare Classic type: 1K")
        }
        assertNull(FlipperNfcParser.parse(dump))
    }
}
