/*
 * ISO7816CardTest.kt
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

package com.codebutler.farebot.card.iso7816

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Tests for ISO7816 card structure and parsing.
 *
 * Ported from Metrodroid's ISO7816Test.kt
 */
class ISO7816CardTest {
    private val testTime = Instant.fromEpochMilliseconds(1264982400000)
    private val testTagId = byteArrayOf(0x01, 0x02, 0x03, 0x04)

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testCardCreation() {
        val appName = "A000000004101001".hexToByteArray() // Sample Mastercard AID
        val app =
            ISO7816Application.create(
                appName = appName,
                type = "emv",
            )

        val card = ISO7816Card.create(testTagId, testTime, listOf(app))

        assertEquals(1, card.applications.size)
        assertNotNull(card.getApplication("emv"))
        assertNull(card.getApplication("calypso"))
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testCardWithMultipleApplications() {
        val emvApp =
            ISO7816Application.create(
                appName = "A000000004101001".hexToByteArray(),
                type = "emv",
            )
        val calypsoApp =
            ISO7816Application.create(
                appName = "315449432E494341".hexToByteArray(),
                type = "calypso",
            )
        val androidHceApp =
            ISO7816Application.create(
                appName = null,
                type = "androidhce",
            )

        val card = ISO7816Card.create(testTagId, testTime, listOf(emvApp, calypsoApp, androidHceApp))

        assertEquals(3, card.applications.size)
        assertNotNull(card.getApplication("emv"))
        assertNotNull(card.getApplication("calypso"))
        assertNotNull(card.getApplication("androidhce"))
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testGetApplicationByName() {
        val appName = "A000000004101001".hexToByteArray()
        val app =
            ISO7816Application.create(
                appName = appName,
                type = "emv",
            )

        val card = ISO7816Card.create(testTagId, testTime, listOf(app))

        val foundApp = card.getApplicationByName(appName)
        assertNotNull(foundApp)
        assertEquals("emv", foundApp.type)
    }

    @Test
    fun testEmptyCard() {
        val card = ISO7816Card.create(testTagId, testTime, emptyList())

        assertEquals(0, card.applications.size)
        assertNull(card.getApplication("emv"))
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testApplicationWithFiles() {
        val file1 =
            ISO7816File.create(
                binaryData = "hello world".encodeToByteArray(),
            )
        val file2 =
            ISO7816File.create(
                records =
                    mapOf(
                        1 to "record1".encodeToByteArray(),
                        2 to "record2".encodeToByteArray(),
                    ),
            )

        val app =
            ISO7816Application.create(
                appName = "A000000004101001".hexToByteArray(),
                files =
                    mapOf(
                        "3F00:0001" to file1,
                        "3F00:0002" to file2,
                    ),
                type = "test",
            )

        val card = ISO7816Card.create(testTagId, testTime, listOf(app))

        val retrievedApp = card.getApplication("test")
        assertNotNull(retrievedApp)

        val retrievedFile1 = retrievedApp.getFile("3F00:0001")
        assertNotNull(retrievedFile1)
        assertTrue(retrievedFile1.binaryData.contentEquals("hello world".encodeToByteArray()))

        val retrievedFile2 = retrievedApp.getFile("3F00:0002")
        assertNotNull(retrievedFile2)
        assertEquals(2, retrievedFile2.records.size)
        assertTrue(retrievedFile2.getRecord(1).contentEquals("record1".encodeToByteArray()))
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testApplicationWithSfiFiles() {
        // SFI (Short File Identifier) is used in Calypso and other cards
        val ticketingEnvFile =
            ISO7816File.create(
                records =
                    mapOf(
                        1 to "environment_data".encodeToByteArray(),
                    ),
            )
        val ticketingContractFile =
            ISO7816File.create(
                records =
                    mapOf(
                        1 to "contract1".encodeToByteArray(),
                        2 to "contract2".encodeToByteArray(),
                    ),
            )

        val app =
            ISO7816Application.create(
                appName = "315449432E494341".hexToByteArray(),
                sfiFiles =
                    mapOf(
                        0x07 to ticketingEnvFile, // Ticketing Environment
                        0x09 to ticketingContractFile, // Contracts
                    ),
                type = "calypso",
            )

        val card = ISO7816Card.create(testTagId, testTime, listOf(app))

        val retrievedApp = card.getApplication("calypso")
        assertNotNull(retrievedApp)
        assertEquals(2, retrievedApp.sfiFiles.size)

        val envFile = retrievedApp.getSfiFile(0x07)
        assertNotNull(envFile)
        assertEquals(1, envFile.records.size)

        val contractFile = retrievedApp.getSfiFile(0x09)
        assertNotNull(contractFile)
        assertEquals(2, contractFile.records.size)
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testFileRecordList() {
        // Records may not be stored in order, but recordList should return them sorted
        val file =
            ISO7816File.create(
                records =
                    mapOf(
                        5 to "record5".encodeToByteArray(),
                        2 to "record2".encodeToByteArray(),
                        8 to "record8".encodeToByteArray(),
                        1 to "record1".encodeToByteArray(),
                    ),
            )

        val recordList = file.recordList
        assertEquals(4, recordList.size)
        assertTrue(recordList[0].contentEquals("record1".encodeToByteArray()))
        assertTrue(recordList[1].contentEquals("record2".encodeToByteArray()))
        assertTrue(recordList[2].contentEquals("record5".encodeToByteArray()))
        assertTrue(recordList[3].contentEquals("record8".encodeToByteArray()))
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testApplicationWithFci() {
        // FCI (File Control Information) is returned when selecting an application
        val fci = "6F1A840E315449432E49434180014F8702FF00A50CC0000000000000000000".hexToByteArray()
        val app =
            ISO7816Application.create(
                appName = "315449432E494341".hexToByteArray(),
                appFci = fci,
                type = "calypso",
            )

        val card = ISO7816Card.create(testTagId, testTime, listOf(app))

        val retrievedApp = card.getApplication("calypso")
        assertNotNull(retrievedApp)
        assertNotNull(retrievedApp.appFci)
        assertTrue(retrievedApp.appFci.contentEquals(fci))
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testFileWithFci() {
        // File FCI contains information about the file structure
        val fileFci = "6207820200118306020200000000".hexToByteArray()
        val file =
            ISO7816File.create(
                binaryData = ByteArray(32),
                fci = fileFci,
            )

        assertNotNull(file.fci)
        assertTrue(file.fci.contentEquals(fileFci))
    }

    @Test
    fun testAndroidHceApplication() {
        // Android HCE apps may not have an AID
        val app =
            ISO7816Application.create(
                appName = null,
                type = "androidhce",
            )

        assertEquals("androidhce", app.type)
        assertNull(app.appName)
    }
}
