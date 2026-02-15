/*
 * OpalTransitTest.kt
 *
 * Copyright 2017-2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright (C) 2024 Eric Butler <eric@codebutler.com>
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

import com.codebutler.farebot.test.CardTestHelper.desfireApp
import com.codebutler.farebot.test.CardTestHelper.desfireCard
import com.codebutler.farebot.test.CardTestHelper.hexToBytes
import com.codebutler.farebot.test.CardTestHelper.standardFile
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.opal.OpalData
import com.codebutler.farebot.transit.opal.OpalTransitFactory
import com.codebutler.farebot.transit.opal.OpalTransitInfo
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class OpalTransitTest {
    private val factory = OpalTransitFactory()

    private fun createOpalCard(fileData: ByteArray) =
        desfireCard(
            applications =
                listOf(
                    desfireApp(0x314553, listOf(standardFile(0x07, fileData))),
                ),
        )

    @Test
    fun testOpalCheck() {
        val card = createOpalCard(ByteArray(16))
        assertTrue(factory.check(card))
    }

    @Test
    fun testOpalCheckNegative() {
        val card =
            desfireCard(
                applications =
                    listOf(
                        desfireApp(0x123456, listOf(standardFile(0x01, ByteArray(16)))),
                    ),
            )
        assertTrue(!factory.check(card))
    }

    @Test
    fun testOpalParseIdentity() =
        runTest {
            // Construct a 16-byte Opal file.
            // After reverseBuffer(0..5), bits 4..8 = lastDigit, bits 8..40 = serialNumber.
            // We'll construct raw data that when reversed gives us known values.
            // The data is stored LSB-first in the file, reversed for parsing.
            // For simplicity, use a data blob that produces known serial.
            val data = ByteArray(16)
            // After reverseBuffer(0,5), we need:
            // bits[4..8] = lastDigit (4 bits), bits[8..40] = serialNumber (32 bits)
            // Let's set bytes after reversal: byte0 has bits[0..7], byte1 has bits[8..15], etc.
            // lastDigit in bits[4..7] of byte0
            // serialNumber in bytes 1-4

            // Before reversal (bytes 0-4 reversed):
            // After reversal byte[0] = original byte[4], byte[1] = original byte[3], etc.
            // Set original bytes so reversed gives: 0x05 (lastDigit=0, upper nibble=0), then serial=1 in bytes 1-4
            data[4] = 0x50 // after reverse -> byte[0] = 0x50 -> lastDigit (bits 4-7) = 5
            data[3] = 0x00
            data[2] = 0x00
            data[1] = 0x00
            data[0] = 0x01 // after reverse -> byte[4] = 0x01

            val card = createOpalCard(data)
            val identity = factory.parseIdentity(card)
            assertEquals("Opal", identity.name.resolveAsync())
        }

    @Test
    fun testOpalCardName() {
        assertEquals("Opal", OpalTransitInfo.NAME)
    }

    @Test
    fun testOpalBalanceCurrencyIsAUD() {
        // Build a valid 16-byte Opal file with a known balance.
        // After reverseBuffer(0, 16), bit fields are extracted.
        // bits[54..75] = rawBalance (21 bits)
        // Let's construct data where balance = 500 cents ($5.00 AUD).
        // This requires careful bit manipulation. For a simpler approach,
        // construct OpalTransitInfo directly.
        val info =
            OpalTransitInfo(
                serialNumber = "3085 2200 0000 0015",
                balanceValue = 500, // 500 cents = $5.00
                checksum = 0,
                weeklyTrips = 0,
                autoTopup = false,
                lastTransaction = 0x01, // tap on
                lastTransactionMode = 0x00, // rail
                minute = 0,
                day = 0,
                lastTransactionNumber = 0,
            )
        val balanceStr = info.formatBalanceString()
        // Should contain AUD formatting, not USD
        assertTrue(
            balanceStr.contains("5.00") || balanceStr.contains("5,00"),
            "Balance should format as $5.00 AUD, got: $balanceStr",
        )
        assertTrue(!balanceStr.contains("USD"), "Balance should not contain USD, got: $balanceStr")
    }

    /**
     * Test demo card parsing.
     * Ported from Metrodroid's OpalTest.testDemoCard().
     */
    @Test
    fun testDemoCard() =
        runTest {
            // This is mocked-up data, probably has a wrong checksum.
            val card = createOpalCard(hexToBytes("87d61200e004002a0014cc44a4133930"))

            // Test TransitIdentity
            val identity = factory.parseIdentity(card)
            assertNotNull(identity)
            assertEquals(OpalTransitInfo.NAME, identity.name.resolveAsync())
            assertEquals("3085 2200 1234 5670", identity.serialNumber)

            // Test TransitInfo
            val info = factory.parseInfo(card)
            assertTrue(info is OpalTransitInfo, "TransitData must be instance of OpalTransitInfo")

            assertEquals("3085 2200 1234 5670", info.serialNumber)
            assertEquals(TransitCurrency.AUD(336), info.balances?.first()?.balance)
            assertEquals(0, info.subscriptions?.size ?: 0)

            // 2015-10-05 09:06 UTC+11 = 2015-10-04 22:06 UTC
            val expectedTime = Instant.parse("2015-10-04T22:06:00Z")
            assertEquals(expectedTime, info.lastTransactionTime)
            assertEquals(OpalData.MODE_BUS, info.lastTransactionMode)
            assertEquals(OpalData.ACTION_JOURNEY_COMPLETED_DISTANCE, info.lastTransaction)
            assertEquals(39, info.lastTransactionNumber)
            assertEquals(1, info.weeklyTrips)

            // Last transaction exposed as trip via OpalTrip
            val trips = info.trips
            assertNotNull(trips)
            assertEquals(1, trips.size)
            assertEquals(Trip.Mode.BUS, trips[0].mode)
            assertEquals(expectedTime, trips[0].startTimestamp)
            assertNotNull(trips[0].routeName, "OpalTrip should have a route name (action description)")
        }

    /**
     * Test daylight savings time transitions.
     * Ported from Metrodroid's OpalTest.testDaylightSavings().
     *
     * Sydney's DST transition in 2018 was at 2018-04-01 03:00 AEDT (UTC+11),
     * when clocks moved back to 2018-04-01 02:00 AEST (UTC+10).
     *
     * The Opal card stores times in UTC, not local time. This test verifies
     * that timestamps around DST boundaries are parsed correctly.
     */
    @Test
    fun testDaylightSavings() {
        // This is all mocked-up data, probably has a wrong checksum.

        // 2018-03-31 09:00 AEDT (UTC+11)
        // = 2018-03-30 22:00 UTC
        var card = createOpalCard(hexToBytes("85D25E07230520A70044DA380419FFFF"))
        var info = factory.parseInfo(card) as OpalTransitInfo
        var expectedTime = Instant.parse("2018-03-30T22:00:00Z")
        assertEquals(
            expectedTime,
            info.lastTransactionTime,
            "Time before DST transition should be 2018-03-30 22:00 UTC",
        )

        // DST transition is at 2018-04-01 03:00 AEDT -> 02:00 AEST

        // 2018-04-01 09:00 AEST (UTC+10)
        // = 2018-03-31 23:00 UTC
        card = createOpalCard(hexToBytes("85D25E07430520A70048DA380419FFFF"))
        info = factory.parseInfo(card) as OpalTransitInfo
        expectedTime = Instant.parse("2018-03-31T23:00:00Z")
        assertEquals(
            expectedTime,
            info.lastTransactionTime,
            "Time after DST transition should be 2018-03-31 23:00 UTC",
        )
    }

    /**
     * Helper to format an Instant as ISO date-time string for debugging.
     */
    private fun Instant.isoDateTimeFormat(): String {
        val local = this.toLocalDateTime(TimeZone.UTC)
        return "${local.year}-${(local.month.ordinal + 1).toString().padStart(
            2,
            '0',
        )}-${local.day.toString().padStart(2, '0')} " +
            "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
    }
}
