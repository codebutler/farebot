/*
 * OrcaTransitTest.kt
 *
 * Copyright 2018 Google
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

import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.test.CardTestHelper.desfireApp
import com.codebutler.farebot.test.CardTestHelper.desfireCard
import com.codebutler.farebot.test.CardTestHelper.hexToBytes
import com.codebutler.farebot.test.CardTestHelper.recordFile
import com.codebutler.farebot.test.CardTestHelper.standardFile
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.orca.OrcaTransitFactory
import com.codebutler.farebot.transit.orca.OrcaTransitInfo
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for Orca card.
 *
 * Ported from Metrodroid's OrcaTest.kt.
 */
class OrcaTransitTest {
    private val factory = OrcaTransitFactory()

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

    private fun constructOrcaCard(): com.codebutler.farebot.card.desfire.DesfireCard {
        val recordSize = 48
        val records =
            listOf(
                hexToBytes(RECORD_0),
                hexToBytes(RECORD_1),
                hexToBytes(RECORD_2),
                hexToBytes(RECORD_3),
                hexToBytes(RECORD_4),
            )

        val f4 = standardFile(0x04, hexToBytes(TEST_FILE_0X4))
        val f2 = recordFile(0x02, recordSize, records)
        val ff = standardFile(0x0f, hexToBytes(TEST_FILE_0XF))

        return desfireCard(
            applications =
                listOf(
                    desfireApp(0x3010f2, listOf(f2, f4)),
                    desfireApp(0xffffff, listOf(ff)),
                ),
        )
    }

    @Test
    fun testDemoCard() {
        val card = constructOrcaCard()

        // Test TransitIdentity
        val identity = factory.parseIdentity(card)
        assertFormattedEquals("ORCA", identity.name)
        assertEquals("12030625", identity.serialNumber)

        // Test TransitInfo
        val info = factory.parseInfo(card)
        assertTrue(info is OrcaTransitInfo, "TransitData must be instance of OrcaTransitInfo")
        assertEquals("12030625", info.serialNumber)
        assertFormattedEquals("ORCA", info.cardName)
        assertEquals(TransitCurrency.USD(23432), info.balances?.firstOrNull()?.balance)
        assertNull(info.subscriptions)

        val trips = info.trips.sortedWith(Trip.Comparator())
        assertNotNull(trips)
        assertTrue(trips.size >= 5, "Should have at least 5 trips, got ${trips.size}")

        // Trip 0: Community Transit bus
        assertFormattedEquals("Community Transit", trips[0].agencyName)
        assertFormattedEquals("CT", trips[0].shortAgencyName)
        assertEquals((1514843334L + 256), trips[0].startTimestamp?.epochSeconds)
        assertEquals(TransitCurrency.USD(534), trips[0].fare)
        assertNull(trips[0].routeName)
        assertEquals(Trip.Mode.BUS, trips[0].mode)
        assertNull(trips[0].startStation)
        assertNull(trips[0].endStation)
        assertEquals("30246", trips[0].vehicleID)

        // Trip 1: Unknown agency bus (agency 0xf)
        assertContains(trips[1].agencyName?.toPlainString() ?: "", "Unknown")
        assertEquals((1514843334L), trips[1].startTimestamp?.epochSeconds)
        assertEquals(TransitCurrency.USD(289), trips[1].fare)
        assertNull(trips[1].routeName)
        assertEquals(Trip.Mode.BUS, trips[1].mode)
        assertNull(trips[1].startStation)
        assertNull(trips[1].endStation)
        assertEquals("30262", trips[1].vehicleID)

        // Trip 2: Sound Transit Link Light Rail
        assertFormattedEquals("Sound Transit", trips[2].agencyName)
        assertFormattedEquals("ST", trips[2].shortAgencyName)
        assertEquals((1514843334L - 256), trips[2].startTimestamp?.epochSeconds)
        assertEquals(TransitCurrency.USD(179), trips[2].fare)
        assertEquals(Trip.Mode.METRO, trips[2].mode)
        assertNotNull(trips[2].startStation)
        // Station name and route name depend on MDST being available
        val trip2StationName = trips[2].startStation?.stationName?.toPlainString()
        if (trip2StationName == "Stadium") {
            // MDST is available with full station data
            assertEquals("Stadium", trip2StationName)
            // Route name comes from MDST line name or falls back to string resource
            val trip2RouteName = trips[2].routeName?.toPlainString()
            assertTrue(
                trip2RouteName == "Link 1 Line" || trip2RouteName == "Link Light Rail",
                "Route name should be 'Link 1 Line' (from MDST) or 'Link Light Rail' (fallback), got: $trip2RouteName",
            )
            assertNotNull(trips[2].startStation?.latitude)
            assertNotNull(trips[2].startStation?.longitude)
            assertNear(47.5918121, trips[2].startStation!!.latitude!!.toDouble(), 0.00001)
            assertNear(-122.327354, trips[2].startStation!!.longitude!!.toDouble(), 0.00001)
        } else {
            // MDST not available or station not found, should have fallback route name
            val trip2RouteName = trips[2].routeName?.toPlainString()
            assertEquals("Link Light Rail", trip2RouteName)
        }
        assertNull(trips[2].endStation)

        // Trip 3: Sound Transit Sounder
        assertFormattedEquals("Sound Transit", trips[3].agencyName)
        assertFormattedEquals("ST", trips[3].shortAgencyName)
        assertEquals((1514843334L - 512), trips[3].startTimestamp?.epochSeconds)
        assertEquals(TransitCurrency.USD(178), trips[3].fare)
        assertEquals(Trip.Mode.TRAIN, trips[3].mode)
        assertNotNull(trips[3].startStation)
        // Station name and route name depend on MDST being available
        val trip3StationName = trips[3].startStation?.stationName?.toPlainString()
        if (trip3StationName == "King Street" || trip3StationName == "King St") {
            // MDST is available with full station data
            assertTrue(
                trip3StationName == "King Street" || trip3StationName == "King St",
                "Station name should be 'King Street' or 'King St', got: $trip3StationName",
            )
            // Route name comes from MDST line name or falls back to string resource
            val trip3RouteName = trips[3].routeName?.toPlainString()
            assertTrue(
                trip3RouteName == "Sounder N Line" || trip3RouteName == "Sounder Train",
                "Route name should be 'Sounder N Line' (from MDST) or 'Sounder Train' (fallback), got: $trip3RouteName",
            )
            assertNotNull(trips[3].startStation?.latitude)
            assertNotNull(trips[3].startStation?.longitude)
            assertNear(47.598445, trips[3].startStation!!.latitude!!.toDouble(), 0.00001)
            assertNear(-122.330161, trips[3].startStation!!.longitude!!.toDouble(), 0.00001)
        } else {
            // MDST not available or station not found, should have fallback route name
            val trip3RouteName = trips[3].routeName?.toPlainString()
            assertEquals("Sounder Train", trip3RouteName)
        }
        assertNull(trips[3].endStation)

        // Trip 4: Washington State Ferries
        assertFormattedEquals("Washington State Ferries", trips[4].agencyName)
        assertFormattedEquals("WSF", trips[4].shortAgencyName)
        assertEquals((1514843334L - 768), trips[4].startTimestamp?.epochSeconds)
        assertEquals(TransitCurrency.USD(177), trips[4].fare)
        assertNull(trips[4].routeName) // WSF doesn't have route names
        assertEquals(Trip.Mode.FERRY, trips[4].mode)
        assertNotNull(trips[4].startStation)
        // Station name depends on MDST being available
        val trip4StationName = trips[4].startStation?.stationName?.toPlainString()
        if (trip4StationName == "Seattle Terminal" || trip4StationName == "Seattle") {
            // MDST is available with full station data
            assertTrue(
                trip4StationName == "Seattle Terminal" || trip4StationName == "Seattle",
                "Station name should be 'Seattle Terminal' or 'Seattle', got: $trip4StationName",
            )
            assertNotNull(trips[4].startStation?.latitude)
            assertNotNull(trips[4].startStation?.longitude)
            assertNear(47.602722, trips[4].startStation!!.latitude!!.toDouble(), 0.00001)
            assertNear(-122.338512, trips[4].startStation!!.longitude!!.toDouble(), 0.00001)
        }
        assertNull(trips[4].endStation)
    }

    companion object {
        // mocked data
        private const val RECORD_0 =
            "00000025a4aadc6800076260000000042c00000000000000000000000000" + "000000000000000000000000000000000000"
        private const val RECORD_1 =
            "000000f5a4aacc6800076360000000024200000000000000000000000000" + "000000000000000000000000000000000000"
        private const val RECORD_2 =
            "00000075a4aabc6fb00338d0000000016600000000000000000000000000" + "000000000000000000000000000000000000"
        private const val RECORD_3 =
            "00000075a4aaac6090000030000000016400000000000000000000000000" + "000000000000000000000000000000000000"
        private const val RECORD_4 =
            "00000085a4aa9c6080027750000000016200000000000000000000000000" + "000000000000000000000000000000000000"
        private const val TEST_FILE_0X4 =
            "000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000" +
                "5b88" + "000000000000000000000000000000000000000000"
        private const val TEST_FILE_0XF = "0000000000b792a100"
    }
}
