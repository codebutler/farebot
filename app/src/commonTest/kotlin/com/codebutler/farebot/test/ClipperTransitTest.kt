/*
 * ClipperTransitTest.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2024 Eric Butler <eric@codebutler.com>
 * Copyright 2017-2018 Michael Farrell <micolous+git@gmail.com>
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

import com.codebutler.farebot.card.desfire.DesfireCard
import com.codebutler.farebot.test.CardTestHelper.desfireApp
import com.codebutler.farebot.test.CardTestHelper.desfireCard
import com.codebutler.farebot.test.CardTestHelper.hexToBytes
import com.codebutler.farebot.test.CardTestHelper.standardFile
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.clipper.ClipperTransitFactory
import com.codebutler.farebot.transit.clipper.ClipperTransitInfo
import com.codebutler.farebot.transit.clipper.ClipperTrip
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for Clipper card.
 *
 * Ported from Metrodroid's ClipperTest.kt.
 */
class ClipperTransitTest {
    private val factory = ClipperTransitFactory()

    private fun assertNear(
        expected: Double,
        actual: Double,
        epsilon: Double,
    ) {
        assertTrue(
            abs(expected - actual) < epsilon,
            "Expected $expected but got $actual (difference > $epsilon)",
        )
    }

    private fun constructClipperCard(): DesfireCard {
        // Construct a card to hold the data.
        val f2 = standardFile(0x02, hexToBytes(TEST_FILE_0X2))
        val f4 = standardFile(0x04, hexToBytes(TEST_FILE_0X4))
        val f8 = standardFile(0x08, hexToBytes(TEST_FILE_0X8))
        val fe = standardFile(0x0e, hexToBytes(TEST_FILE_0XE))

        return desfireCard(
            applications =
                listOf(
                    desfireApp(APP_ID, listOf(f2, f4, f8, fe)),
                ),
        )
    }

    @Test
    fun testClipperCheck() {
        val card =
            desfireCard(
                applications =
                    listOf(
                        desfireApp(0x9011f2, listOf(standardFile(0x08, ByteArray(32)))),
                    ),
            )
        assertTrue(factory.check(card))
    }

    @Test
    fun testClipperCheckNegative() {
        val card =
            desfireCard(
                applications =
                    listOf(
                        desfireApp(0x123456, listOf(standardFile(0x01, ByteArray(32)))),
                    ),
            )
        assertFalse(factory.check(card))
    }

    @Test
    fun testClipperTripModeDetection_BART() {
        // BART with transportCode 0x6f -> METRO
        val trip =
            ClipperTrip
                .builder()
                .agency(0x04) // AGENCY_BART
                .transportCode(0x6f)
                .build()
        assertEquals(Trip.Mode.METRO, trip.mode)
    }

    @Test
    fun testClipperTripModeDetection_MuniLightRail() {
        // Muni with transportCode 0x62 -> TRAM (default)
        val trip =
            ClipperTrip
                .builder()
                .agency(0x12) // AGENCY_MUNI
                .transportCode(0x62)
                .build()
        assertEquals(Trip.Mode.TRAM, trip.mode)
    }

    @Test
    fun testClipperTripModeDetection_Caltrain() {
        // Caltrain with transportCode 0x62 -> TRAIN
        val trip =
            ClipperTrip
                .builder()
                .agency(0x06) // AGENCY_CALTRAIN
                .transportCode(0x62)
                .build()
        assertEquals(Trip.Mode.TRAIN, trip.mode)
    }

    @Test
    fun testClipperTripModeDetection_SMART() {
        // SMART with transportCode 0x62 -> TRAIN
        val trip =
            ClipperTrip
                .builder()
                .agency(0x0c) // AGENCY_SMART
                .transportCode(0x62)
                .build()
        assertEquals(Trip.Mode.TRAIN, trip.mode)
    }

    @Test
    fun testClipperTripModeDetection_GGFerry() {
        // GG Ferry with transportCode 0x62 -> FERRY
        val trip =
            ClipperTrip
                .builder()
                .agency(0x19) // AGENCY_GG_FERRY
                .transportCode(0x62)
                .build()
        assertEquals(Trip.Mode.FERRY, trip.mode)
    }

    @Test
    fun testClipperTripModeDetection_SFBayFerry() {
        // SF Bay Ferry with transportCode 0x62 -> FERRY
        val trip =
            ClipperTrip
                .builder()
                .agency(0x1b) // AGENCY_SF_BAY_FERRY
                .transportCode(0x62)
                .build()
        assertEquals(Trip.Mode.FERRY, trip.mode)
    }

    @Test
    fun testClipperTripModeDetection_Bus() {
        val trip =
            ClipperTrip
                .builder()
                .agency(0x01) // AGENCY_ACTRAN
                .transportCode(0x61)
                .build()
        assertEquals(Trip.Mode.BUS, trip.mode)
    }

    @Test
    fun testClipperTripModeDetection_Unknown() {
        val trip =
            ClipperTrip
                .builder()
                .agency(0x04) // AGENCY_BART
                .transportCode(0xFF)
                .build()
        assertEquals(Trip.Mode.OTHER, trip.mode)
    }

    @Test
    fun testClipperTripFareCurrency() {
        val trip =
            ClipperTrip
                .builder()
                .agency(0x04)
                .fare(350)
                .build()
        val fareStr = trip.fare?.formatCurrencyString() ?: ""
        // Should format as USD
        assertTrue(
            fareStr.contains("3.50") || fareStr.contains("3,50"),
            "Fare should be $3.50, got: $fareStr",
        )
    }

    @Test
    fun testClipperTripWithBalance() {
        val trip =
            ClipperTrip
                .builder()
                .agency(0x04)
                .fare(200)
                .balance(1000)
                .build()
        val updated = trip.withBalance(500)
        assertEquals(500L, updated.getBalance())
    }

    @Test
    fun testDemoCard() =
        runBlocking {
            assertEquals(32 * 2, REFILL.length)

            // This is mocked-up data, probably has a wrong checksum.
            val card = constructClipperCard()

            // Test TransitIdentity
            val identity = factory.parseIdentity(card)
            assertEquals("Clipper", identity.name.resolveAsync())
            assertEquals("572691763", identity.serialNumber)

            val info = factory.parseInfo(card)
            assertTrue(info is ClipperTransitInfo, "TransitData must be instance of ClipperTransitInfo")

            assertEquals("572691763", info.serialNumber)
            assertEquals("Clipper", info.cardName.resolveAsync())
            assertEquals(TransitCurrency.USD(30583), info.balances?.firstOrNull()?.balance)
            assertNull(info.subscriptions)

            val trips = info.trips
            assertNotNull(trips)
            // Note: FareBot doesn't include refills in trips list (unlike Metrodroid)
            // So we only have the BART trip here
            assertTrue(trips.isNotEmpty(), "Should have at least 1 trip")

            // Find the BART trip
            var bartTrip = trips.first()
            for (trip in trips) {
                val agency = trip.agencyName?.resolveAsync()
                val shortAgency = trip.shortAgencyName?.resolveAsync()
                if (agency?.contains("BART") == true || shortAgency == "BART") {
                    bartTrip = trip
                    break
                }
            }

            // BART trip verification
            assertEquals(Trip.Mode.METRO, bartTrip.mode)
            assertEquals(TransitCurrency.USD(630), bartTrip.fare)

            // Verify timestamp - 1521320320 seconds Unix time
            assertNotNull(bartTrip.startTimestamp)
            assertEquals(1521320320L, bartTrip.startTimestamp!!.epochSeconds)

            // Verify station names if MDST is available
            if (bartTrip.startStation != null) {
                val startStationName = bartTrip.startStation?.displayName?.resolveAsync() ?: ""
                val endStationName = bartTrip.endStation?.displayName?.resolveAsync() ?: ""
                // These may be resolved names from MDST, or hex placeholders if not available
                assertTrue(startStationName.isNotEmpty(), "Start station should have a name")
                if (startStationName == "Powell Street") {
                    // MDST is available, verify coordinates
                    assertNotNull(bartTrip.startStation?.latitude)
                    assertNotNull(bartTrip.startStation?.longitude)
                    assertNear(37.78447, bartTrip.startStation!!.latitude!!.toDouble(), 0.001)
                    assertNear(-122.40797, bartTrip.startStation!!.longitude!!.toDouble(), 0.001)
                }
                if (endStationName == "Dublin / Pleasanton") {
                    assertNotNull(bartTrip.endStation?.latitude)
                    assertNotNull(bartTrip.endStation?.longitude)
                    assertNear(37.70169, bartTrip.endStation!!.latitude!!.toDouble(), 0.001)
                    assertNear(-121.89918, bartTrip.endStation!!.longitude!!.toDouble(), 0.001)
                }
            }
        }

    @Test
    fun testVehicleNumbers() {
        // Test null vehicle number (0)
        val trip0 =
            ClipperTrip
                .builder()
                .agency(0x12) // Muni
                .vehicleNum(0)
                .build()
        assertNull(trip0.vehicleID)

        // Test null vehicle number (0xffff)
        val tripFfff =
            ClipperTrip
                .builder()
                .agency(0x12)
                .vehicleNum(0xffff)
                .build()
        assertNull(tripFfff.vehicleID)

        // Test regular vehicle number
        val trip1058 =
            ClipperTrip
                .builder()
                .agency(0x12)
                .vehicleNum(1058)
                .build()
        assertEquals("1058", trip1058.vehicleID)

        // Test regular vehicle number
        val trip1525 =
            ClipperTrip
                .builder()
                .agency(0x12)
                .vehicleNum(1525)
                .build()
        assertEquals("1525", trip1525.vehicleID)

        // Test LRV4 Muni vehicle numbers (5 digits, encoded as number*10 + letter)
        // 2010A = 20100 + 1 - 1 = 20101? No, the encoding is: number/10 gives the vehicle, %10 gives letter offset
        // 20101: 20101/10 = 2010, 20101%10 = 1, letter = 9+1 = A (in hex, 10 = A)
        val trip2010A =
            ClipperTrip
                .builder()
                .agency(0x12)
                .vehicleNum(20101)
                .build()
        assertEquals("2010A", trip2010A.vehicleID)

        // 2061B = vehicle/10 = 2061, letter offset = 2 -> 9+2 = B (11 in hex = B)
        val trip2061B =
            ClipperTrip
                .builder()
                .agency(0x12)
                .vehicleNum(20612)
                .build()
        assertEquals("2061B", trip2061B.vehicleID)
    }

    @Test
    fun testHumanReadableRouteID() {
        // Golden Gate Ferry should display route ID in hex
        val ggFerryTrip =
            ClipperTrip
                .builder()
                .agency(0x19) // AGENCY_GG_FERRY
                .route(0x1234)
                .build()
        assertEquals("0x1234", ggFerryTrip.humanReadableRouteID)

        // Other agencies should not have humanReadableRouteID
        val bartTrip =
            ClipperTrip
                .builder()
                .agency(0x04) // AGENCY_BART
                .route(0x5678)
                .build()
        assertNull(bartTrip.humanReadableRouteID)

        val muniTrip =
            ClipperTrip
                .builder()
                .agency(0x12) // AGENCY_MUNI
                .route(0xABCD)
                .build()
        assertNull(muniTrip.humanReadableRouteID)
    }

    @Test
    fun testBalanceExpiry() {
        // Create a card with expiry data in file 0x01
        // Expiry is stored as days since Clipper epoch at offset 8 (2 bytes)
        // Let's use 45000 days from Clipper epoch (around year 2023)
        val expiryDays = 45000
        val expiryBytes =
            ByteArray(10).also {
                // Put expiry days at offset 8-9 (big endian)
                it[8] = ((expiryDays shr 8) and 0xFF).toByte()
                it[9] = (expiryDays and 0xFF).toByte()
            }

        val f1 = standardFile(0x01, expiryBytes)
        val f2 = standardFile(0x02, hexToBytes(TEST_FILE_0X2))
        val f4 = standardFile(0x04, hexToBytes(TEST_FILE_0X4))
        val f8 = standardFile(0x08, hexToBytes(TEST_FILE_0X8))
        val fe = standardFile(0x0e, hexToBytes(TEST_FILE_0XE))

        val card =
            desfireCard(
                applications =
                    listOf(
                        desfireApp(APP_ID, listOf(f1, f2, f4, f8, fe)),
                    ),
            )

        val info = factory.parseInfo(card)
        val balances = info.balances
        assertNotNull(balances, "Balances should not be null")
        assertTrue(balances.isNotEmpty(), "Should have at least one balance")
        assertNotNull(balances[0].validTo, "Balance should have an expiry date")
    }

    companion object {
        private const val APP_ID = 0x9011f2

        // mocked data from Metrodroid test
        private const val REFILL = "000002cfde440000781234560000138800000000000000000000000000000000"
        private const val TRIP = "000000040000027600000000de580000de58100000080027000000000000006f"
        private const val TEST_FILE_0X2 = "0000000000000000000000000000000000007777"
        private const val TEST_FILE_0X4 = REFILL
        private const val TEST_FILE_0X8 = "0022229533"
        private const val TEST_FILE_0XE = TRIP
    }
}
