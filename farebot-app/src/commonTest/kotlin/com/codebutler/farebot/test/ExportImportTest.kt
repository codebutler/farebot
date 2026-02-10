/*
 * ExportImportTest.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2024 Eric Butler <eric@codebutler.com>
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

import com.codebutler.farebot.shared.serialize.ExportFormat
import com.codebutler.farebot.shared.serialize.ExportHelper
import com.codebutler.farebot.shared.serialize.ExportMetadata
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class ExportImportTest {

    @Test
    fun testExportFormatFromExtension() {
        assertEquals(ExportFormat.JSON, ExportFormat.fromExtension("json"))
        assertEquals(ExportFormat.JSON, ExportFormat.fromExtension("JSON"))
        assertEquals(ExportFormat.XML, ExportFormat.fromExtension("xml"))
        assertEquals(ExportFormat.XML, ExportFormat.fromExtension("XML"))
        assertEquals(null, ExportFormat.fromExtension("txt"))
    }

    @Test
    fun testExportFormatFromMimeType() {
        assertEquals(ExportFormat.JSON, ExportFormat.fromMimeType("application/json"))
        assertEquals(ExportFormat.XML, ExportFormat.fromMimeType("application/xml"))
        assertEquals(null, ExportFormat.fromMimeType("text/plain"))
    }

    @Test
    fun testMakeFilename() {
        val tagId = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val scannedAt = Instant.fromEpochMilliseconds(1700000000000L) // 2023-11-14

        val filename = ExportHelper.makeFilename(tagId, scannedAt, ExportFormat.JSON)
        assertTrue(filename.startsWith("FareBot-01020304-"))
        assertTrue(filename.endsWith(".json"))

        val xmlFilename = ExportHelper.makeFilename(tagId, scannedAt, ExportFormat.XML)
        assertTrue(xmlFilename.endsWith(".xml"))
    }

    @Test
    fun testMakeFilenameWithGeneration() {
        val tagId = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
        val scannedAt = Instant.fromEpochMilliseconds(1700000000000L)

        val filename0 = ExportHelper.makeFilename(tagId, scannedAt, ExportFormat.JSON, 0)
        val filename1 = ExportHelper.makeFilename(tagId, scannedAt, ExportFormat.JSON, 1)
        val filename2 = ExportHelper.makeFilename(tagId, scannedAt, ExportFormat.JSON, 2)

        // First generation should not have number
        assertTrue(!filename0.contains("-0."))
        // Subsequent generations should have number
        assertTrue(filename1.contains("-1."))
        assertTrue(filename2.contains("-2."))
    }

    @Test
    fun testMakeBulkExportFilename() {
        val timestamp = Instant.fromEpochMilliseconds(1700000000000L)
        val filename = ExportHelper.makeBulkExportFilename(ExportFormat.JSON, timestamp)

        assertTrue(filename.startsWith("farebot-export-"))
        assertTrue(filename.endsWith(".json"))
    }

    @Test
    fun testGetExtension() {
        assertEquals("json", ExportHelper.getExtension("test.json"))
        assertEquals("xml", ExportHelper.getExtension("test.xml"))
        assertEquals("json", ExportHelper.getExtension("farebot-export-20231114.json"))
        assertEquals(null, ExportHelper.getExtension("noextension"))
    }

    @Test
    fun testGetFormatFromFilename() {
        assertEquals(ExportFormat.JSON, ExportHelper.getFormatFromFilename("test.json"))
        assertEquals(ExportFormat.XML, ExportHelper.getFormatFromFilename("test.xml"))
        assertEquals(null, ExportHelper.getFormatFromFilename("test.txt"))
    }

    @Test
    fun testExportMetadata() {
        val metadata = ExportMetadata.create(versionCode = 42, versionName = "1.2.3")

        assertEquals("FareBot", metadata.appName)
        assertEquals(42, metadata.versionCode)
        assertEquals("1.2.3", metadata.versionName)
        assertEquals(1, metadata.formatVersion)
        assertNotNull(metadata.exportedAt)
    }
}
