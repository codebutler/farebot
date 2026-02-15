/*
 * MdstStationTableReaderTest.kt
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.base.mdst

import farebot.base.generated.resources.Res
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for MdstStationTableReader and MdstStationLookup (MdST file format).
 *
 * Ported from Metrodroid's StationTableReaderTest.kt.
 *
 * Note: Some tests from the original cannot be directly ported because they rely on:
 * - Locale/language settings (setLocale)
 * - Preferences (showRawStationIds, showBothLocalAndEnglish)
 * - Transit-specific classes (EasyCardTransaction, AdelaideTransaction, etc.)
 * - Suica database with localised names
 *
 * This test focuses on the core MDST parsing functionality that can be tested without
 * those dependencies.
 *
 * Tests that require MDST files from bundled resources will fail if the resources
 * are not available in the test environment.
 */
class MdstStationTableReaderTest {
    // Test constants matching SEQ Go database
    private companion object {
        const val SEQ_GO_STR = "seq_go"
        const val DOMESTIC_AIRPORT = 9
        const val AMIIBO_STR = "amiibo"
    }

    private fun requireMdstFile(dbName: String): MdstStationTableReader =
        MdstStationTableReader.getReader(dbName)
            ?: throw AssertionError("MDST file '$dbName' not available")

    /**
     * Tests that the SEQ Go database can be loaded and stations can be looked up.
     */
    @Test
    fun testSeqGoDatabase() {
        requireMdstFile(SEQ_GO_STR)

        val station = MdstStationLookup.getStation(SEQ_GO_STR, DOMESTIC_AIRPORT)
        assertNotNull(station, "Station should be found in SEQ Go database")
        assertEquals(
            "Domestic Airport",
            station.stationName,
            "Station name should be 'Domestic Airport'",
        )
    }

    /**
     * Tests parsing MDST file directly from filesystem.
     */
    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testMdstProtobufParsing() {
        val bytes = runBlocking { Res.readBytes("files/seq_go.mdst") }

        val reader = MdstStationTableReader.fromByteArray(bytes)
        assertNotNull(reader.notice, "License notice should exist")

        val station = reader.getStationById(9)
        assertNotNull(station, "Station 9 should be found")
        assertEquals(9, station.id)
        assertEquals("Domestic Airport", station.name.english)
    }

    /**
     * Tests Suica rail station lookup - the real user-reported bug.
     * Shinjuku station: area=0, line=37, station=10 -> stationId = (0 shl 16) or (37 shl 8) or 10 = 9482
     */
    @Test
    fun testSuicaRailStationLookup() {
        val bytes = runBlocking { Res.readBytes("files/suica_rail.mdst") }

        val reader = MdstStationTableReader.fromByteArray(bytes)

        // Shinjuku: area=0, line=37 (0x25), station=10 (0x0a)
        // stationId = (0 shl 16) or (37 shl 8) or 10 = 9482
        val shinjukuId = (0 shl 16) or (37 shl 8) or 10
        val shinjuku = reader.getStationById(shinjukuId)
        assertNotNull(shinjuku, "Shinjuku should be found (id=$shinjukuId)")
        println("Shinjuku: english=${shinjuku.name.english}, local=${shinjuku.name.local}")
        assertTrue(
            shinjuku.name.english.contains("Shinjuku") || shinjuku.name.local.contains("新宿"),
            "Station should be Shinjuku, got: ${shinjuku.name}",
        )
    }

    /**
     * Tests Suica bus station lookup.
     */
    @Test
    fun testSuicaBusStationLookup() {
        val bytes = runBlocking { Res.readBytes("files/suica_bus.mdst") }

        // Just verify it parses without error and can look up stations
        val reader = MdstStationTableReader.fromByteArray(bytes)
        assertNotNull(reader)
    }

    /**
     * Tests behavior when database is not found.
     */
    @Test
    fun testDbNotFound() {
        verifyNonExistentDb("nonexistent")
    }

    /**
     * Tests behavior when empty database name is used.
     */
    @Test
    fun testDbEmpty() {
        verifyNonExistentDb("")
    }

    private fun verifyNonExistentDb(dbName: String) {
        // Station lookup should return null for non-existent database
        val station = MdstStationLookup.getStation(dbName, 3)
        assertNull(station, "Station should be null for non-existent database")

        // Operator lookup should return null
        val operator = MdstStationLookup.getOperatorName(dbName, 3)
        assertNull(operator, "Operator should be null for non-existent database")

        // Line lookup should return null
        val line = MdstStationLookup.getLineName(dbName, 7)
        assertNull(line, "Line should be null for non-existent database")

        // Mode lookup should return null
        val mode = MdstStationLookup.getLineMode(dbName, 9)
        assertNull(mode, "Line mode should be null for non-existent database")
    }

    /**
     * Tests that the license notice can be retrieved from the database.
     */
    @Test
    fun testLicenseNotice() {
        val reader = requireMdstFile(SEQ_GO_STR)

        val notice = reader.notice
        assertNotNull(notice, "License notice should not be null")
        assertTrue(
            notice.contains("Translink"),
            "License notice should mention Translink",
        )
    }

    /**
     * Tests that stations without location data return no coordinates.
     */
    @Test
    fun testNoLocation() {
        requireMdstFile(AMIIBO_STR)

        // Amiibo database has entries without location data
        val station = MdstStationLookup.getStation(AMIIBO_STR, 2)
        assertNotNull(station, "Station should be found in Amiibo database")
        assertEquals("Peach", station.stationName, "Station name should be 'Peach'")
        // Amiibo entries don't have location data (lat/lon should be 0,0 which means no location)
        assertFalse(station.hasLocation, "Amiibo entry should not have location")
    }

    /**
     * Tests operator name lookup.
     */
    @Test
    fun testOperator() {
        requireMdstFile(AMIIBO_STR)

        val operatorName = MdstStationLookup.getOperatorName(AMIIBO_STR, 1)
        assertNotNull(operatorName, "Operator should be found")
        assertEquals("Super Mario Bros.", operatorName, "Operator name should match")

        // Unknown operator should return null
        val unknownOperator = MdstStationLookup.getOperatorName(AMIIBO_STR, 0x77)
        assertNull(unknownOperator, "Unknown operator should return null")
    }

    /**
     * Tests that the MdST header validation works correctly.
     */
    @Test
    fun testInvalidHeader() {
        // Test with data that's too small
        var exception: Exception? = null
        try {
            MdstStationTableReader.fromByteArray(ByteArray(4))
        } catch (e: MdstStationTableReader.InvalidHeaderException) {
            exception = e
        }
        assertNotNull(exception, "Should throw exception for small data")
        assertTrue(
            exception.message!!.contains("too small"),
            "Exception message should mention size",
        )

        // Test with wrong magic
        exception = null
        try {
            MdstStationTableReader.fromByteArray(ByteArray(16))
        } catch (e: MdstStationTableReader.InvalidHeaderException) {
            exception = e
        }
        assertNotNull(exception, "Should throw exception for wrong magic")
        assertTrue(
            exception.message!!.contains("magic"),
            "Exception message should mention magic",
        )
    }

    /**
     * Tests that unsupported version numbers are rejected.
     */
    @Test
    fun testInvalidVersion() {
        // Construct a buffer with correct magic but wrong version
        val data = ByteArray(16)
        // "MdST" magic
        data[0] = 0x4d // 'M'
        data[1] = 0x64 // 'd'
        data[2] = 0x53 // 'S'
        data[3] = 0x54 // 'T'
        // Version 99 (big-endian)
        data[4] = 0x00
        data[5] = 0x00
        data[6] = 0x00
        data[7] = 99.toByte()

        var exception: Exception? = null
        try {
            MdstStationTableReader.fromByteArray(data)
        } catch (e: MdstStationTableReader.InvalidHeaderException) {
            exception = e
        }
        assertNotNull(exception, "Should throw exception for wrong version")
        assertTrue(
            exception.message!!.contains("version") || exception.message!!.contains("99"),
            "Exception message should mention version",
        )
    }

    /**
     * Tests location coordinate handling.
     * Stations with (0,0) coordinates should report no location.
     * Stations with non-zero coordinates should report having location.
     */
    @Test
    fun testLocationHandling() {
        requireMdstFile(SEQ_GO_STR)

        // Test a station that has coordinates (Domestic Airport in SEQ Go)
        val stationWithLocation = MdstStationLookup.getStation(SEQ_GO_STR, DOMESTIC_AIRPORT)
        assertNotNull(stationWithLocation)
        // SEQ Go Domestic Airport has coordinates
        assertTrue(
            stationWithLocation.hasLocation,
            "SEQ Go Domestic Airport should have location data",
        )
        assertTrue(
            stationWithLocation.latitude != 0f || stationWithLocation.longitude != 0f,
            "Location should have non-zero coordinates",
        )
    }

    /**
     * Tests that station IDs not in the database return null.
     */
    @Test
    fun testUnknownStation() {
        requireMdstFile(SEQ_GO_STR)

        val station = MdstStationLookup.getStation(SEQ_GO_STR, 99999)
        assertNull(station, "Unknown station ID should return null")
    }

    /**
     * Tests line name lookup.
     */
    @Test
    fun testLineName() {
        requireMdstFile(SEQ_GO_STR)

        // The SEQ Go database may not have line names in a simple format we can test,
        // but we can at least verify the lookup doesn't crash and returns null for unknown
        val unknownLine = MdstStationLookup.getLineName(SEQ_GO_STR, 99999)
        assertNull(unknownLine, "Unknown line ID should return null")
    }

    /**
     * Tests that getting the reader for the same database returns cached instance.
     */
    @Test
    fun testReaderCaching() {
        val reader1 = requireMdstFile(SEQ_GO_STR)

        val reader2 = MdstStationTableReader.getReader(SEQ_GO_STR)
        assertNotNull(reader2)
        // Both should be the same instance due to caching
        assertTrue(reader1 === reader2, "Reader should be cached")
    }

    /**
     * Tests MdstStationResult.hasLocation property.
     */
    @Test
    fun testMdstStationResultHasLocation() {
        // Test with (0,0) - no location
        val noLocation =
            MdstStationResult(
                stationName = "Test",
                shortStationName = null,
                companyName = null,
                lineNames = emptyList(),
                latitude = 0f,
                longitude = 0f,
            )
        assertFalse(noLocation.hasLocation, "(0,0) should report no location")

        // Test with non-zero latitude
        val hasLatitude =
            MdstStationResult(
                stationName = "Test",
                shortStationName = null,
                companyName = null,
                lineNames = emptyList(),
                latitude = 1.0f,
                longitude = 0f,
            )
        assertTrue(hasLatitude.hasLocation, "Non-zero latitude should have location")

        // Test with non-zero longitude
        val hasLongitude =
            MdstStationResult(
                stationName = "Test",
                shortStationName = null,
                companyName = null,
                lineNames = emptyList(),
                latitude = 0f,
                longitude = 1.0f,
            )
        assertTrue(hasLongitude.hasLocation, "Non-zero longitude should have location")

        // Test with both non-zero
        val hasBoth =
            MdstStationResult(
                stationName = "Test",
                shortStationName = null,
                companyName = null,
                lineNames = emptyList(),
                latitude = -33.8688f,
                longitude = 151.2093f,
            )
        assertTrue(hasBoth.hasLocation, "Non-zero lat/lon should have location")
    }

    /**
     * Tests TransportType enum values match expected order.
     */
    @Test
    fun testTransportTypeEnum() {
        // Verify the transport types are in the expected order for protobuf compatibility
        assertEquals(0, TransportType.UNKNOWN.ordinal)
        assertEquals(1, TransportType.BUS.ordinal)
        assertEquals(2, TransportType.TRAIN.ordinal)
        assertEquals(3, TransportType.TRAM.ordinal)
        assertEquals(4, TransportType.METRO.ordinal)
        assertEquals(5, TransportType.FERRY.ordinal)
        assertEquals(6, TransportType.TICKET_MACHINE.ordinal)
        assertEquals(7, TransportType.VENDING_MACHINE.ordinal)
        assertEquals(8, TransportType.POS.ordinal)
        assertEquals(9, TransportType.OTHER.ordinal)
        assertEquals(10, TransportType.BANNED.ordinal)
        assertEquals(11, TransportType.TROLLEYBUS.ordinal)
        assertEquals(12, TransportType.TOLL_ROAD.ordinal)
        assertEquals(13, TransportType.MONORAIL.ordinal)
    }

    // ==================== Locale-based Name Selection Tests ====================

    /**
     * Tests that shouldUseLocalName returns true for exact language match.
     */
    @Test
    fun testShouldUseLocalNameExactMatch() {
        // Japanese device with Japanese local language
        assertTrue(MdstStationLookup.shouldUseLocalName("ja", listOf("ja")))
        // Chinese device with Chinese local language
        assertTrue(MdstStationLookup.shouldUseLocalName("zh", listOf("zh")))
        // Case insensitive match
        assertTrue(MdstStationLookup.shouldUseLocalName("JA", listOf("ja")))
        assertTrue(MdstStationLookup.shouldUseLocalName("ja", listOf("JA")))
    }

    /**
     * Tests that shouldUseLocalName returns false when language doesn't match.
     */
    @Test
    fun testShouldUseLocalNameNoMatch() {
        // English device with Japanese local language - should use English
        assertFalse(MdstStationLookup.shouldUseLocalName("en", listOf("ja")))
        // French device with Chinese local language - should use English
        assertFalse(MdstStationLookup.shouldUseLocalName("fr", listOf("zh")))
        // Empty local languages list
        assertFalse(MdstStationLookup.shouldUseLocalName("en", emptyList()))
    }

    /**
     * Tests that shouldUseLocalName handles prefix matching (ja matches ja-JP).
     */
    @Test
    fun testShouldUseLocalNamePrefixMatch() {
        // Device has "ja", database has "ja-JP"
        assertTrue(MdstStationLookup.shouldUseLocalName("ja", listOf("ja-JP")))
        // Device has "zh", database has "zh-TW"
        assertTrue(MdstStationLookup.shouldUseLocalName("zh", listOf("zh-TW")))
        // Device has "ja-JP", database has "ja"
        assertTrue(MdstStationLookup.shouldUseLocalName("ja-JP", listOf("ja")))
    }

    /**
     * Tests that shouldUseLocalName handles multiple local languages.
     */
    @Test
    fun testShouldUseLocalNameMultipleLanguages() {
        // Database supports both Japanese and Chinese
        val localLanguages = listOf("ja", "zh")
        assertTrue(MdstStationLookup.shouldUseLocalName("ja", localLanguages))
        assertTrue(MdstStationLookup.shouldUseLocalName("zh", localLanguages))
        assertFalse(MdstStationLookup.shouldUseLocalName("en", localLanguages))
    }

    /**
     * Tests selectName prefers English when device language doesn't match local languages.
     */
    @Test
    fun testSelectNamePrefersEnglishForNonLocalDevice() {
        val names =
            Names(
                english = "Tokyo Station",
                local = "\u6771\u4eac\u99c5",
                englishShort = "Tokyo",
                localShort = "\u6771\u4eac",
            )
        val localLanguages = listOf("ja")

        // English device should see English name
        assertEquals("Tokyo Station", MdstStationLookup.selectName(names, localLanguages, "en", false))
        assertEquals("Tokyo", MdstStationLookup.selectName(names, localLanguages, "en", true))
    }

    /**
     * Tests selectName prefers local name when device language matches.
     */
    @Test
    fun testSelectNamePrefersLocalForMatchingDevice() {
        val names =
            Names(
                english = "Tokyo Station",
                local = "\u6771\u4eac\u99c5",
                englishShort = "Tokyo",
                localShort = "\u6771\u4eac",
            )
        val localLanguages = listOf("ja")

        // Japanese device should see Japanese name
        assertEquals("\u6771\u4eac\u99c5", MdstStationLookup.selectName(names, localLanguages, "ja", false))
        assertEquals("\u6771\u4eac", MdstStationLookup.selectName(names, localLanguages, "ja", true))
    }

    /**
     * Tests selectName falls back to English when local name is empty.
     */
    @Test
    fun testSelectNameFallsBackToEnglishWhenLocalEmpty() {
        val names =
            Names(
                english = "Central Station",
                local = "",
                englishShort = "Central",
                localShort = "",
            )
        val localLanguages = listOf("ja")

        // Even with Japanese device, should fall back to English since local is empty
        assertEquals("Central Station", MdstStationLookup.selectName(names, localLanguages, "ja", false))
        assertEquals("Central", MdstStationLookup.selectName(names, localLanguages, "ja", true))
    }

    /**
     * Tests selectName falls back to local when English is empty.
     */
    @Test
    fun testSelectNameFallsBackToLocalWhenEnglishEmpty() {
        val names =
            Names(
                english = "",
                local = "\u6771\u4eac\u99c5",
                englishShort = "",
                localShort = "\u6771\u4eac",
            )
        val localLanguages = listOf("ja")

        // English device should fall back to local name since English is empty
        assertEquals("\u6771\u4eac\u99c5", MdstStationLookup.selectName(names, localLanguages, "en", false))
        assertEquals("\u6771\u4eac", MdstStationLookup.selectName(names, localLanguages, "en", true))
    }

    /**
     * Tests selectName returns null when both names are empty.
     */
    @Test
    fun testSelectNameReturnsNullWhenBothEmpty() {
        val names = Names(english = "", local = "", englishShort = "", localShort = "")
        val localLanguages = listOf("ja")

        assertNull(MdstStationLookup.selectName(names, localLanguages, "en", false))
        assertNull(MdstStationLookup.selectName(names, localLanguages, "ja", false))
    }

    /**
     * Tests selectName returns null for null input.
     */
    @Test
    fun testSelectNameReturnsNullForNullInput() {
        assertNull(MdstStationLookup.selectName(null, listOf("ja"), "en", false))
    }

    /**
     * Tests that EasyCard (Taiwan/zh-TW) shows Chinese names for Chinese devices.
     */
    @Test
    fun testChineseLocaleForEasyCard() {
        // EasyCard uses zh-TW as local language
        val localLanguages = listOf("zh-TW")
        val names =
            Names(
                english = "Taipei Main Station",
                local = "\u53f0\u5317\u8eca\u7ad9",
                englishShort = "Taipei",
                localShort = "\u53f0\u5317",
            )

        // Traditional Chinese device (zh-TW) should see Chinese
        assertEquals("\u53f0\u5317\u8eca\u7ad9", MdstStationLookup.selectName(names, localLanguages, "zh-TW", false))
        // Just "zh" should also match (prefix match: zh-TW starts with zh)
        assertEquals("\u53f0\u5317\u8eca\u7ad9", MdstStationLookup.selectName(names, localLanguages, "zh", false))
        // English device should see English
        assertEquals("Taipei Main Station", MdstStationLookup.selectName(names, localLanguages, "en", false))
        // Simplified Chinese device (zh-CN) does NOT match zh-TW (different regional variants)
        // This is by design - zh-CN and zh-TW are distinct locales
        assertEquals("Taipei Main Station", MdstStationLookup.selectName(names, localLanguages, "zh-CN", false))
    }

    /**
     * Tests that databases with just base language code work for regional variants.
     */
    @Test
    fun testBaseLanguageMatchesRegionalVariants() {
        // Database specifies just "zh" as local language
        val localLanguages = listOf("zh")
        val names =
            Names(
                english = "Beijing Station",
                local = "\u5317\u4eac\u7ad9",
                englishShort = "Beijing",
                localShort = "\u5317\u4eac",
            )

        // Any Chinese device should match "zh"
        assertEquals("\u5317\u4eac\u7ad9", MdstStationLookup.selectName(names, localLanguages, "zh", false))
        assertEquals("\u5317\u4eac\u7ad9", MdstStationLookup.selectName(names, localLanguages, "zh-CN", false))
        assertEquals("\u5317\u4eac\u7ad9", MdstStationLookup.selectName(names, localLanguages, "zh-TW", false))
        assertEquals("\u5317\u4eac\u7ad9", MdstStationLookup.selectName(names, localLanguages, "zh-HK", false))
        // English device should see English
        assertEquals("Beijing Station", MdstStationLookup.selectName(names, localLanguages, "en", false))
    }
}
