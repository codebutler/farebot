/*
 * TestAssetLoader.kt
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

import com.codebutler.farebot.card.Card
import com.codebutler.farebot.card.RawCard
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.raw.RawClassicBlock
import com.codebutler.farebot.card.classic.raw.RawClassicCard
import com.codebutler.farebot.card.classic.raw.RawClassicSector
import com.codebutler.farebot.shared.serialize.CardImporter
import com.codebutler.farebot.shared.serialize.ImportResult
import com.codebutler.farebot.shared.serialize.KotlinxCardSerializer
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitInfo
import kotlin.time.Instant
import kotlinx.serialization.json.Json
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Platform-specific function to load a test resource as bytes.
 * The path is relative to the test resources root (e.g., "easycard/deadbeef.mfc").
 */
expect fun loadTestResource(path: String): ByteArray?

/**
 * Utility for loading card dump files from test resources.
 *
 * Supports:
 * - .mfc files: MIFARE Classic binary dumps (like from MIFARE Classic Tool app)
 * - .json files: FareBot JSON card exports
 */
object TestAssetLoader {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val serializer = KotlinxCardSerializer(json)

    /**
     * Loads a JSON card dump and deserializes it to a RawCard.
     *
     * @param resourcePath Path to the JSON file relative to test resources
     * @return The deserialized RawCard
     * @throws AssertionError if the file is not found
     */
    fun loadJsonCard(resourcePath: String): RawCard<*> {
        val bytes = loadTestResource(resourcePath)
        assertNotNull(bytes, "Test resource not found: $resourcePath")
        val jsonString = bytes.decodeToString()
        return serializer.deserialize(jsonString)
    }

    /**
     * Loads a Metrodroid JSON card dump and converts it to a RawCard.
     * Handles the Metrodroid format (with mifareDesfire, mifareUltralight, etc. keys)
     * by using CardImporter for format conversion.
     *
     * @param resourcePath Path to the JSON file relative to test resources
     * @return The deserialized RawCard
     * @throws AssertionError if the file is not found or import fails
     */
    fun loadMetrodroidJsonCard(resourcePath: String): RawCard<*> {
        val bytes = loadTestResource(resourcePath)
        assertNotNull(bytes, "Test resource not found: $resourcePath")
        val jsonString = bytes.decodeToString()
        val importer = CardImporter.create(KotlinxCardSerializer(json))
        val result = importer.importCards(jsonString)
        assertTrue(result is ImportResult.Success, "Failed to import card from $resourcePath: $result")
        return (result as ImportResult.Success).cards.first()
    }

    /**
     * Loads a .mfc (MIFARE Classic binary dump) file and converts it to a RawClassicCard.
     *
     * The .mfc format is a raw binary dump of all sectors:
     * - Sectors 0-31: 4 blocks x 16 bytes = 64 bytes per sector
     * - Sectors 32-39: 16 blocks x 16 bytes = 256 bytes per sector
     *
     * @param resourcePath Path to the .mfc file relative to test resources
     * @param scannedAt Optional timestamp for when the card was scanned
     * @return The RawClassicCard representation
     * @throws AssertionError if the file is not found
     */
    fun loadMfcCard(
        resourcePath: String,
        scannedAt: Instant = TEST_TIMESTAMP
    ): RawClassicCard {
        val bytes = loadTestResource(resourcePath)
        assertNotNull(bytes, "Test resource not found: $resourcePath")
        return parseMfcBytes(bytes, scannedAt)
    }

    /**
     * Parses raw .mfc bytes into a RawClassicCard.
     */
    private fun parseMfcBytes(bytes: ByteArray, scannedAt: Instant): RawClassicCard {
        val sectors = mutableListOf<RawClassicSector>()
        var offset = 0
        var sectorNum = 0

        while (offset < bytes.size) {
            // Sectors 0-31 have 4 blocks, sectors 32-39 have 16 blocks
            val blockCount = if (sectorNum >= 32) 16 else 4
            val sectorSize = blockCount * 16

            if (offset + sectorSize > bytes.size) {
                // Incomplete sector at end of file - stop here
                break
            }

            val sectorBytes = bytes.copyOfRange(offset, offset + sectorSize)
            val blocks = (0 until blockCount).map { blockIndex ->
                val blockStart = blockIndex * 16
                val blockData = sectorBytes.copyOfRange(blockStart, blockStart + 16)
                RawClassicBlock.create(blockIndex, blockData)
            }

            sectors.add(RawClassicSector.createData(sectorNum, blocks))
            offset += sectorSize
            sectorNum++
        }

        // Extract UID from block 0
        val tagId = extractUidFromBlock0(sectors.firstOrNull())

        // Fill remaining sectors as unauthorized based on detected card size
        val maxSector = when {
            sectorNum <= 16 -> 15  // 1K card
            sectorNum <= 32 -> 31  // 2K card
            else -> 39             // 4K card
        }

        while (sectors.size <= maxSector) {
            sectors.add(RawClassicSector.createUnauthorized(sectors.size))
        }

        return RawClassicCard.create(tagId, scannedAt, sectors)
    }

    /**
     * Extracts the UID from block 0 of a Classic card.
     * Standard cards have 4-byte UIDs, some have 7-byte UIDs.
     */
    private fun extractUidFromBlock0(sector0: RawClassicSector?): ByteArray {
        if (sector0 == null || sector0.blocks.isNullOrEmpty()) {
            return byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
        }

        val block0 = sector0.blocks!![0].data

        // Check for 7-byte UID (starts with 0x04 and has specific pattern)
        return if (block0[0] == 0x04.toByte() &&
            (block0.getUShort(8) == 0x0400.toUShort() || block0.getUShort(8) == 0x4400.toUShort())
        ) {
            block0.copyOfRange(0, 7)
        } else {
            block0.copyOfRange(0, 4)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun ByteArray.getUShort(offset: Int): UShort {
        return ((this[offset].toInt() and 0xFF) shl 8 or (this[offset + 1].toInt() and 0xFF)).toUShort()
    }
}

/**
 * Default timestamp used for test cards.
 */
val TEST_TIMESTAMP: Instant = Instant.fromEpochSeconds(1609459200) // 2021-01-01T00:00:00Z

/**
 * Base class for tests that load card dumps from test resources.
 */
abstract class CardDumpTest {

    /**
     * Loads an .mfc file and parses it using the given transit factory.
     */
    inline fun <reified T : TransitInfo> loadAndParseMfc(
        path: String,
        factory: TransitFactory<ClassicCard, T>,
        scannedAt: Instant = TEST_TIMESTAMP
    ): T {
        val rawCard = TestAssetLoader.loadMfcCard(path, scannedAt)
        val card = rawCard.parse()
        assertTrue(factory.check(card), "Card did not match factory: ${factory::class.simpleName}")
        val transitInfo = factory.parseInfo(card)
        assertNotNull(transitInfo, "Failed to parse transit info")
        assertTrue(transitInfo is T, "Transit info is not of expected type")
        return transitInfo
    }

    /**
     * Loads an .mfc file and returns the parsed ClassicCard.
     */
    fun loadMfcCard(
        path: String,
        scannedAt: Instant = TEST_TIMESTAMP
    ): ClassicCard {
        return TestAssetLoader.loadMfcCard(path, scannedAt).parse()
    }

    /**
     * Loads a JSON card dump and parses it using the given transit factory.
     */
    inline fun <reified C : Card, reified T : TransitInfo> loadAndParseJson(
        path: String,
        factory: TransitFactory<C, T>
    ): T {
        val rawCard = TestAssetLoader.loadJsonCard(path)
        @Suppress("UNCHECKED_CAST")
        val card = rawCard.parse() as C
        assertTrue(factory.check(card), "Card did not match factory: ${factory::class.simpleName}")
        val transitInfo = factory.parseInfo(card)
        assertNotNull(transitInfo, "Failed to parse transit info")
        assertTrue(transitInfo is T, "Transit info is not of expected type")
        return transitInfo
    }

    /**
     * Loads a Metrodroid JSON card dump and parses it using the given transit factory.
     */
    inline fun <reified C : Card, reified T : TransitInfo> loadAndParseMetrodroidJson(
        path: String,
        factory: TransitFactory<C, T>
    ): Pair<C, T> {
        val rawCard = TestAssetLoader.loadMetrodroidJsonCard(path)
        @Suppress("UNCHECKED_CAST")
        val card = rawCard.parse() as C
        assertTrue(factory.check(card), "Card did not match factory: ${factory::class.simpleName}")
        val transitInfo = factory.parseInfo(card)
        assertNotNull(transitInfo, "Failed to parse transit info")
        assertTrue(transitInfo is T, "Transit info is not of expected type")
        return Pair(card, transitInfo)
    }
}
