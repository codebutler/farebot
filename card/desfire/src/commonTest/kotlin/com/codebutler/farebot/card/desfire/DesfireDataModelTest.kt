/*
 * DesfireDataModelTest.kt
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

package com.codebutler.farebot.card.desfire

import com.codebutler.farebot.card.desfire.raw.RawDesfireApplication
import com.codebutler.farebot.card.desfire.raw.RawDesfireCard
import com.codebutler.farebot.card.desfire.raw.RawDesfireFile
import com.codebutler.farebot.card.desfire.raw.RawDesfireFileSettings
import com.codebutler.farebot.card.desfire.raw.RawDesfireManufacturingData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Tests for DESFire data model changes in PR #218:
 * - Nullable fileSettings on RawDesfireFile / DesfireFile
 * - authLog and dirListLocked on RawDesfireApplication / DesfireApplication
 * - appListLocked on RawDesfireCard / DesfireCard
 */
class DesfireDataModelTest {
    private val tagId = byteArrayOf(0x01, 0x02, 0x03, 0x04)
    private val scannedAt = Instant.fromEpochMilliseconds(1264982400000)
    private val emptyManufData = RawDesfireManufacturingData.create(ByteArray(28))

    private val standardFileSettings =
        byteArrayOf(
            0x00, // STANDARD_DATA_FILE
            0x00, // commSetting
            0x00,
            0x00, // accessRights
            0x05,
            0x00,
            0x00, // fileSize = 5 (little endian)
        )

    // -- Nullable fileSettings --

    @Test
    fun testRawDesfireFileWithNullSettingsCreatesSuccessfully() {
        val file = RawDesfireFile.create(0x01, null, byteArrayOf(0x01, 0x02))
        assertEquals(0x01, file.fileId())
        assertNull(file.fileSettings())
        assertTrue(file.fileData()!!.contentEquals(byteArrayOf(0x01, 0x02)))
    }

    @Test
    fun testUnauthorizedFileWithNullSettings() {
        val file = RawDesfireFile.createUnauthorized(0x02, null, "Access denied")
        assertEquals(0x02, file.fileId())
        assertNull(file.fileSettings())
        assertNull(file.fileData())
        assertEquals(RawDesfireFile.Error.TYPE_UNAUTHORIZED, file.error()!!.type())
    }

    @Test
    fun testInvalidFileWithNullSettings() {
        val file = RawDesfireFile.createInvalid(0x03, null, "Read error")
        assertEquals(0x03, file.fileId())
        assertNull(file.fileSettings())
        assertEquals(RawDesfireFile.Error.TYPE_INVALID, file.error()!!.type())
    }

    @Test
    fun testUnauthorizedFileWithNullSettingsParses() {
        val file = RawDesfireFile.createUnauthorized(0x02, null, "Access denied")
        val parsed = file.parse()
        assertIs<UnauthorizedDesfireFile>(parsed)
        assertEquals(0x02, parsed.id)
        assertNull(parsed.fileSettings)
        assertEquals("Access denied", parsed.errorMessage)
    }

    @Test
    fun testInvalidFileWithNullSettingsParses() {
        val file = RawDesfireFile.createInvalid(0x03, null, "Read error")
        val parsed = file.parse()
        assertIs<InvalidDesfireFile>(parsed)
        assertEquals(0x03, parsed.id)
        assertNull(parsed.fileSettings)
        assertEquals("Read error", parsed.errorMessage)
    }

    @Test
    fun testUnauthorizedFileWithSettingsParses() {
        val settings = RawDesfireFileSettings.create(standardFileSettings)
        val file = RawDesfireFile.createUnauthorized(0x01, settings, "Permission denied")
        val parsed = file.parse()
        assertIs<UnauthorizedDesfireFile>(parsed)
        assertEquals(0x01, parsed.id)
        assertEquals("Permission denied", parsed.errorMessage)
    }

    @Test
    fun testStandardFileWithSettingsParses() {
        val settings = RawDesfireFileSettings.create(standardFileSettings)
        val file = RawDesfireFile.create(0x01, settings, byteArrayOf(0x68, 0x65, 0x6C, 0x6C, 0x6F))
        val parsed = file.parse()
        assertIs<StandardDesfireFile>(parsed)
        assertEquals(0x01, parsed.id)
    }

    // -- authLog and dirListLocked on RawDesfireApplication --

    @Test
    fun testRawDesfireApplicationDefaultValues() {
        val app = RawDesfireApplication.create(0x123456, emptyList())
        assertEquals(0x123456, app.appId())
        assertTrue(app.authLog.isEmpty())
        assertFalse(app.dirListLocked)
    }

    @Test
    fun testRawDesfireApplicationWithAuthLog() {
        val authEntry =
            DesfireAuthLog(
                keyId = 0,
                challenge = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08),
                response = byteArrayOf(0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18),
                confirm = byteArrayOf(0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28),
            )
        val app =
            RawDesfireApplication.create(
                0x123456,
                emptyList(),
                authLog = listOf(authEntry),
                dirListLocked = true,
            )

        assertEquals(1, app.authLog.size)
        assertEquals(0, app.authLog[0].keyId)
        assertTrue(app.dirListLocked)
    }

    @Test
    fun testRawDesfireApplicationParsePropagatesToDesfireApplication() {
        val authEntry =
            DesfireAuthLog(
                keyId = 3,
                challenge = byteArrayOf(0x01),
                response = byteArrayOf(0x02),
                confirm = byteArrayOf(0x03),
            )
        val settings = RawDesfireFileSettings.create(standardFileSettings)
        val file = RawDesfireFile.create(0x01, settings, byteArrayOf(0x68, 0x65, 0x6C, 0x6C, 0x6F))
        val rawApp =
            RawDesfireApplication.create(
                0xABCDEF,
                listOf(file),
                authLog = listOf(authEntry),
                dirListLocked = true,
            )

        val parsed = rawApp.parse()
        assertEquals(0xABCDEF, parsed.id)
        assertEquals(1, parsed.files.size)
        assertEquals(1, parsed.authLog.size)
        assertEquals(3, parsed.authLog[0].keyId)
        assertTrue(parsed.dirListLocked)
    }

    // -- appListLocked on RawDesfireCard --

    @Test
    fun testRawDesfireCardDefaultAppListLocked() {
        val card = RawDesfireCard.create(tagId, scannedAt, emptyList(), emptyManufData)
        assertFalse(card.appListLocked)
    }

    @Test
    fun testRawDesfireCardWithAppListLocked() {
        val card = RawDesfireCard.create(tagId, scannedAt, emptyList(), emptyManufData, appListLocked = true)
        assertTrue(card.appListLocked)
    }

    @Test
    fun testRawDesfireCardParsePropagatesToDesfireCard() {
        val card = RawDesfireCard.create(tagId, scannedAt, emptyList(), emptyManufData, appListLocked = true)
        val parsed = card.parse()
        assertTrue(parsed.appListLocked)
        assertTrue(parsed.applications.isEmpty())
    }

    // -- DesfireApplication.getFile --

    @Test
    fun testDesfireApplicationGetFile() {
        val settings = RawDesfireFileSettings.create(standardFileSettings)
        val file = RawDesfireFile.create(0x05, settings, byteArrayOf(0x01))
        val rawApp = RawDesfireApplication.create(0x100, listOf(file))
        val app = rawApp.parse()

        val found = app.getFile(0x05)
        assertEquals(0x05, found?.id)

        val notFound = app.getFile(0x99)
        assertNull(notFound)
    }

    // -- Card with unauthorized + null-settings files is marked unauthorized --

    @Test
    fun testCardWithOnlyUnauthorizedNullSettingsFilesIsUnauthorized() {
        val unauthorizedApp =
            RawDesfireApplication.create(
                0x1234,
                listOf(
                    RawDesfireFile.createUnauthorized(0x01, null, "Access denied"),
                    RawDesfireFile.createUnauthorized(0x02, null, "Permission denied"),
                ),
            )
        val card = RawDesfireCard.create(tagId, scannedAt, listOf(unauthorizedApp), emptyManufData)
        assertTrue(card.isUnauthorized())
    }

    @Test
    fun testCardWithMixedFilesNotUnauthorized() {
        val settings = RawDesfireFileSettings.create(standardFileSettings)
        val mixedApp =
            RawDesfireApplication.create(
                0x1234,
                listOf(
                    RawDesfireFile.create(0x01, null, byteArrayOf(0x01)), // readable, null settings
                    RawDesfireFile.createUnauthorized(0x02, settings, "Access denied"),
                ),
            )
        val card = RawDesfireCard.create(tagId, scannedAt, listOf(mixedApp), emptyManufData)
        assertFalse(card.isUnauthorized())
    }

    // -- Exception type hierarchy --

    @Test
    fun testPermissionDeniedExtendsUnauthorized() {
        val ex = PermissionDeniedException()
        assertIs<UnauthorizedException>(ex)
    }

    @Test
    fun testNotFoundExceptionIsNotUnauthorized() {
        val ex: Exception = NotFoundException("not found")
        assertFalse(ex is UnauthorizedException)
    }
}
