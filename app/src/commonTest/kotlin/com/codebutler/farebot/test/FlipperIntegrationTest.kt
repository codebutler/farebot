/*
 * FlipperIntegrationTest.kt
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

import com.codebutler.farebot.card.desfire.DesfireCard
import com.codebutler.farebot.card.felica.FelicaCard
import com.codebutler.farebot.shared.serialize.FlipperNfcParser
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.clipper.ClipperTransitFactory
import com.codebutler.farebot.transit.clipper.ClipperTransitInfo
import com.codebutler.farebot.transit.orca.OrcaTransitFactory
import com.codebutler.farebot.transit.orca.OrcaTransitInfo
import com.codebutler.farebot.transit.suica.SuicaTransitFactory
import com.codebutler.farebot.transit.suica.SuicaTransitInfo
import com.codebutler.farebot.base.util.FormattedString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Full pipeline integration tests: Flipper NFC dump -> raw card -> parsed card -> transit info.
 *
 * These tests load real Flipper Zero NFC card dumps and exercise the complete parsing pipeline,
 * asserting on exact trip data, balances, fares, timestamps, stations, and modes.
 */
class FlipperIntegrationTest {
    private fun loadFlipperDump(name: String): String {
        val bytes = loadTestResource("flipper/$name")
        assertNotNull(bytes, "Test resource not found: flipper/$name")
        return bytes.decodeToString()
    }

    // --- ORCA (DESFire) ---

    @Test
    fun testOrcaFromFlipper() {
        val data = loadFlipperDump("ORCA.nfc")
        val rawCard = FlipperNfcParser.parse(data)
        assertNotNull(rawCard, "Failed to parse ORCA Flipper dump")

        val card = rawCard.parse()
        assertTrue(card is DesfireCard, "Expected DesfireCard, got ${card::class.simpleName}")

        val factory = OrcaTransitFactory()
        assertTrue(factory.check(card), "ORCA factory should recognize this card")

        val identity = factory.parseIdentity(card)
        assertFormattedEquals("ORCA", identity.name)
        assertEquals("10043012", identity.serialNumber)

        val info = factory.parseInfo(card)
        assertNotNull(info, "Failed to parse ORCA transit info")
        assertTrue(info is OrcaTransitInfo)

        // Balance: $26.25 USD
        val balances = info.balances
        assertNotNull(balances)
        assertEquals(1, balances.size)
        assertEquals(TransitCurrency.USD(2625), balances[0].balance)

        // This dump has 0 trips in the history
        val trips = info.trips
        assertNotNull(trips)
        assertEquals(0, trips.size)

        assertNull(info.subscriptions)
    }

    // --- Clipper (DESFire) ---

    @Test
    fun testClipperFromFlipper() {
        val data = loadFlipperDump("Clipper.nfc")
        val rawCard = FlipperNfcParser.parse(data)
        assertNotNull(rawCard, "Failed to parse Clipper Flipper dump")

        val card = rawCard.parse()
        assertTrue(card is DesfireCard, "Expected DesfireCard, got ${card::class.simpleName}")

        val factory = ClipperTransitFactory()
        assertTrue(factory.check(card), "Clipper factory should recognize this card")

        val identity = factory.parseIdentity(card)
        assertFormattedEquals("Clipper", identity.name)
        assertEquals("1205019883", identity.serialNumber)

        val info = factory.parseInfo(card)
        assertNotNull(info, "Failed to parse Clipper transit info")
        assertTrue(info is ClipperTransitInfo)

        // Balance: $2.25 USD
        val balances = info.balances
        assertNotNull(balances)
        assertEquals(1, balances.size)
        assertEquals(TransitCurrency.USD(225), balances[0].balance)

        // 16 trips — all Muni (San Francisco Municipal)
        val trips = info.trips
        assertNotNull(trips)
        assertEquals(16, trips.size)

        assertNull(info.subscriptions)

        // Trip 0: Bus ride on Muni
        trips[0].let { t ->
            assertEquals(Trip.Mode.BUS, t.mode)
            assertEquals(TransitCurrency.USD(225), t.fare)
            assertEquals(Instant.parse("2017-03-28T23:18:27Z"), t.startTimestamp)
            assertNull(t.endTimestamp)
            assertFormattedEquals("San Francisco Municipal", t.agencyName)
            assertFormattedEquals("Muni", t.shortAgencyName)
            assertNull(t.routeName)
            assertNull(t.startStation)
            assertNull(t.endStation)
            assertNull(t.machineID)
            assertEquals("6705", t.vehicleID)
        }

        // Trip 1: Muni Metro at Powell
        trips[1].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.USD(225), t.fare)
            assertEquals(Instant.parse("2017-03-28T02:58:32Z"), t.startTimestamp)
            assertNull(t.endTimestamp)
            assertFormattedEquals("San Francisco Municipal", t.agencyName)
            assertFormattedEquals("Muni", t.shortAgencyName)
            assertFormattedEquals("Powell", t.startStation?.stationName)
            assertNull(t.endStation)
            assertNull(t.vehicleID)
        }

        // Trip 2: Muni Metro at Van Ness
        trips[2].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.USD(225), t.fare)
            assertEquals(Instant.parse("2017-03-28T01:22:17Z"), t.startTimestamp)
            assertFormattedEquals("Van Ness", t.startStation?.stationName)
        }

        // Trip 3: Muni Metro at Powell
        trips[3].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.USD(225), t.fare)
            assertEquals(Instant.parse("2017-03-27T01:49:56Z"), t.startTimestamp)
            assertFormattedEquals("Powell", t.startStation?.stationName)
        }

        // Trip 4: Muni Metro at Van Ness
        trips[4].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.USD(225), t.fare)
            assertEquals(Instant.parse("2017-03-27T00:15:46Z"), t.startTimestamp)
            assertFormattedEquals("Van Ness", t.startStation?.stationName)
        }

        // Trip 5: Muni Metro at Powell
        trips[5].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.USD(225), t.fare)
            assertEquals(Instant.parse("2017-03-25T05:50:32Z"), t.startTimestamp)
            assertFormattedEquals("Powell", t.startStation?.stationName)
        }

        // Trip 6: Muni Metro at Van Ness
        trips[6].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.USD(225), t.fare)
            assertEquals(Instant.parse("2017-03-25T02:58:08Z"), t.startTimestamp)
            assertFormattedEquals("Van Ness", t.startStation?.stationName)
        }

        // Trip 7: Muni Metro at Powell — $0 fare (transfer)
        trips[7].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.USD(0), t.fare)
            assertEquals(Instant.parse("2017-03-23T23:38:53Z"), t.startTimestamp)
            assertFormattedEquals("Powell", t.startStation?.stationName)
        }

        // Trip 8: Muni Metro at Van Ness
        trips[8].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.USD(225), t.fare)
            assertEquals(Instant.parse("2017-03-23T23:28:14Z"), t.startTimestamp)
            assertFormattedEquals("Van Ness", t.startStation?.stationName)
        }

        // Trip 9: Muni Metro at Powell — $0 fare
        trips[9].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.USD(0), t.fare)
            assertEquals(Instant.parse("2017-03-22T16:31:56Z"), t.startTimestamp)
            assertFormattedEquals("Powell", t.startStation?.stationName)
        }

        // Trip 10: Muni Metro at Van Ness
        trips[10].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.USD(225), t.fare)
            assertEquals(Instant.parse("2017-03-22T15:20:10Z"), t.startTimestamp)
            assertFormattedEquals("Van Ness", t.startStation?.stationName)
        }

        // Trip 11: Muni Metro at Castro
        trips[11].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.USD(225), t.fare)
            assertEquals(Instant.parse("2017-03-22T04:31:30Z"), t.startTimestamp)
            assertFormattedEquals("Castro", t.startStation?.stationName)
        }

        // Trip 12: Muni Metro at Van Ness
        trips[12].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.USD(225), t.fare)
            assertEquals(Instant.parse("2017-03-22T01:47:07Z"), t.startTimestamp)
            assertFormattedEquals("Van Ness", t.startStation?.stationName)
        }

        // Trip 13: Muni Metro at Van Ness
        trips[13].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.USD(225), t.fare)
            assertEquals(Instant.parse("2017-03-21T01:50:06Z"), t.startTimestamp)
            assertFormattedEquals("Van Ness", t.startStation?.stationName)
        }

        // Trip 14: Muni Metro at Powell
        trips[14].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.USD(225), t.fare)
            assertEquals(Instant.parse("2017-03-19T21:01:16Z"), t.startTimestamp)
            assertFormattedEquals("Powell", t.startStation?.stationName)
        }

        // Trip 15: Muni Metro at Van Ness
        trips[15].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.USD(225), t.fare)
            assertEquals(Instant.parse("2017-03-19T19:28:38Z"), t.startTimestamp)
            assertFormattedEquals("Van Ness", t.startStation?.stationName)
        }
    }

    // --- Suica (FeliCa) ---

    @Test
    fun testSuicaFromFlipper() {
        val data = loadFlipperDump("Suica.nfc")
        val rawCard = FlipperNfcParser.parse(data)
        assertNotNull(rawCard, "Failed to parse Suica Flipper dump")

        val card = rawCard.parse()
        assertTrue(card is FelicaCard, "Expected FelicaCard, got ${card::class.simpleName}")

        val factory = SuicaTransitFactory()
        assertTrue(factory.check(card), "Suica factory should recognize this card")

        val identity = factory.parseIdentity(card)
        assertFormattedEquals("Suica", identity.name)
        assertNull(identity.serialNumber)

        val info = factory.parseInfo(card)
        assertNotNull(info, "Failed to parse Suica transit info")
        assertTrue(info is SuicaTransitInfo)

        // Balance: 870 JPY
        val balances = info.balances
        assertNotNull(balances)
        assertEquals(1, balances.size)
        assertEquals(TransitCurrency.JPY(870), balances[0].balance)

        val trips = info.trips
        assertNotNull(trips)
        assertEquals(20, trips.size)

        assertNull(info.subscriptions)

        // Trip 0: Tokyu Toyoko — Shibuya to Toritsudaigaku, 0 JPY
        trips[0].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.JPY(0), t.fare)
            assertFormattedEquals("Tokyu", t.agencyName)
            assertFormattedEquals("Tōkyūtōyoko", t.routeName)
            assertFormattedEquals("Shibuya", t.startStation?.stationName)
            assertFormattedEquals("Toritsudaigaku", t.endStation?.stationName)
        }

        // Trip 1: Tokyu Toyoko — Toritsudaigaku to Shibuya, 150 JPY
        trips[1].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.JPY(150), t.fare)
            assertFormattedEquals("Tokyu", t.agencyName)
            assertFormattedEquals("Tōkyūtōyoko", t.routeName)
            assertFormattedEquals("Toritsudaigaku", t.startStation?.stationName)
            assertFormattedEquals("Shibuya", t.endStation?.stationName)
        }

        // Trip 2: JR East Yamate — Shibuya to Koenji, 160 JPY
        trips[2].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.JPY(160), t.fare)
            assertFormattedEquals("JR East", t.agencyName)
            assertFormattedEquals("Yamate", t.routeName)
            assertFormattedEquals("Shibuya", t.startStation?.stationName)
            assertFormattedEquals("Kōenji", t.endStation?.stationName)
        }

        // Trip 3: JR East Chuo — Koenji to Shinjuku, 150 JPY
        trips[3].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.JPY(150), t.fare)
            assertFormattedEquals("JR East", t.agencyName)
            assertFormattedEquals("Chūō", t.routeName)
            assertFormattedEquals("Kōenji", t.startStation?.stationName)
            assertFormattedEquals("Shinjuku", t.endStation?.stationName)
        }

        // Trip 4: JR East Chuo — Shinjuku to Koenji, 150 JPY
        trips[4].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.JPY(150), t.fare)
            assertFormattedEquals("JR East", t.agencyName)
            assertFormattedEquals("Chūō", t.routeName)
            assertFormattedEquals("Shinjuku", t.startStation?.stationName)
            assertFormattedEquals("Kōenji", t.endStation?.stationName)
        }

        // Trip 5: JR East Chuo — Koenji to Shinjuku, 150 JPY
        trips[5].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.JPY(150), t.fare)
            assertFormattedEquals("JR East", t.agencyName)
            assertFormattedEquals("Chūō", t.routeName)
            assertFormattedEquals("Kōenji", t.startStation?.stationName)
            assertFormattedEquals("Shinjuku", t.endStation?.stationName)
        }

        // Trip 6: Tokyo Metro Marunouchi — Tokyo, ticket machine, 110 JPY
        trips[6].let { t ->
            assertEquals(Trip.Mode.TICKET_MACHINE, t.mode)
            assertEquals(TransitCurrency.JPY(110), t.fare)
            assertFormattedEquals("Tokyo Metro", t.agencyName)
            assertFormattedEquals("#4 Marunouchi", t.routeName)
            assertFormattedEquals("Tōkyō", t.startStation?.stationName)
            assertNull(t.endStation)
        }

        // Trip 7: Ticket Machine Charge — -2000 JPY
        trips[7].let { t ->
            assertEquals(Trip.Mode.TICKET_MACHINE, t.mode)
            assertEquals(TransitCurrency.JPY(-2000), t.fare)
            assertNull(t.agencyName)
            assertFormattedEquals("Ticket Machine Charge", t.routeName)
            assertNull(t.startStation)
        }

        // Trip 8: Tokyo Metro Ginza — Aoyamaitchome to Jimbocho, 160 JPY
        trips[8].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.JPY(160), t.fare)
            assertFormattedEquals("Tokyo Metro", t.agencyName)
            assertFormattedEquals("#3 Ginza", t.routeName)
            assertFormattedEquals("Aoyamaitchōme", t.startStation?.stationName)
            assertFormattedEquals("Jinbōchō", t.endStation?.stationName)
        }

        // Trip 9: Vending Machine — 120 JPY
        trips[9].let { t ->
            assertEquals(Trip.Mode.VENDING_MACHINE, t.mode)
            assertEquals(TransitCurrency.JPY(120), t.fare)
            assertEquals(Instant.parse("2011-03-04T06:28:00Z"), t.startTimestamp)
            assertNull(t.agencyName)
            assertFormattedEquals("Vending Machine Merchandise", t.routeName)
            assertNull(t.startStation)
        }

        // Trip 10: Toei Sanda — Jimbocho to Iwamotomachi, 100 JPY
        trips[10].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.JPY(100), t.fare)
            assertFormattedEquals("Toei", t.agencyName)
            assertFormattedEquals("#6 Sanda", t.routeName)
            assertFormattedEquals("Jinbōchō", t.startStation?.stationName)
            assertFormattedEquals("Iwamotomachi", t.endStation?.stationName)
        }

        // Trip 11: JR East Sobu — Asakusabashi to Shibuya, 210 JPY
        trips[11].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.JPY(210), t.fare)
            assertFormattedEquals("JR East", t.agencyName)
            assertFormattedEquals("Sōbu", t.routeName)
            assertFormattedEquals("Asakusabashi", t.startStation?.stationName)
            assertFormattedEquals("Shibuya", t.endStation?.stationName)
        }

        // Trip 12: Toei Oedo — Aoyamaitchome to Shinjuku, 170 JPY
        trips[12].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.JPY(170), t.fare)
            assertFormattedEquals("Toei", t.agencyName)
            assertFormattedEquals("#12 Ōedo", t.routeName)
            assertFormattedEquals("Aoyamaitchōme", t.startStation?.stationName)
            assertFormattedEquals("Shinjuku", t.endStation?.stationName)
        }

        // Trip 13: Toei Shinjuku — Shinjuku to Roppongi, 210 JPY
        trips[13].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.JPY(210), t.fare)
            assertFormattedEquals("Toei", t.agencyName)
            assertFormattedEquals("#10 Shinjuku", t.routeName)
            assertFormattedEquals("Shinjuku", t.startStation?.stationName)
            assertFormattedEquals("Roppongi", t.endStation?.stationName)
        }

        // Trip 14: Tokyo Metro Ginza — Aoyamaitchome to Shibuya, 160 JPY
        trips[14].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.JPY(160), t.fare)
            assertFormattedEquals("Tokyo Metro", t.agencyName)
            assertFormattedEquals("#3 Ginza", t.routeName)
            assertFormattedEquals("Aoyamaitchōme", t.startStation?.stationName)
            assertFormattedEquals("Shibuya", t.endStation?.stationName)
        }

        // Trip 15: Tokyo Metro Ginza — Shibuya to Shinnakano, 190 JPY
        trips[15].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.JPY(190), t.fare)
            assertFormattedEquals("Tokyo Metro", t.agencyName)
            assertFormattedEquals("#3 Ginza", t.routeName)
            assertFormattedEquals("Shibuya", t.startStation?.stationName)
            assertFormattedEquals("Shinnakano", t.endStation?.stationName)
        }

        // Trip 16: Tokyo Metro Marunouchi — Shinnakano to Omotesando, 190 JPY
        trips[16].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.JPY(190), t.fare)
            assertFormattedEquals("Tokyo Metro", t.agencyName)
            assertFormattedEquals("#4 Marunouchi", t.routeName)
            assertFormattedEquals("Shinnakano", t.startStation?.stationName)
            assertFormattedEquals("Omotesandō", t.endStation?.stationName)
        }

        // Trip 17: Tokyo Metro Ginza — Aoyamaitchome to Ginza, 160 JPY
        trips[17].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.JPY(160), t.fare)
            assertFormattedEquals("Tokyo Metro", t.agencyName)
            assertFormattedEquals("#3 Ginza", t.routeName)
            assertFormattedEquals("Aoyamaitchōme", t.startStation?.stationName)
            assertFormattedEquals("Ginza", t.endStation?.stationName)
        }

        // Trip 18: Tokyo Metro Ginza — Ginza to Toranomon, 160 JPY
        trips[18].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.JPY(160), t.fare)
            assertFormattedEquals("Tokyo Metro", t.agencyName)
            assertFormattedEquals("#3 Ginza", t.routeName)
            assertFormattedEquals("Ginza", t.startStation?.stationName)
            assertFormattedEquals("Toranomon", t.endStation?.stationName)
            assertEquals(Instant.parse("2011-03-10T15:00:00Z"), t.startTimestamp)
            assertEquals(Instant.parse("2011-03-11T05:57:00Z"), t.endTimestamp)
        }

        // Trip 19: Tokyo Metro Ginza — Aoyamaitchome to Shibuya, 160 JPY (latest with precise timestamps)
        trips[19].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.JPY(160), t.fare)
            assertFormattedEquals("Tokyo Metro", t.agencyName)
            assertFormattedEquals("#3 Ginza", t.routeName)
            assertFormattedEquals("Aoyamaitchōme", t.startStation?.stationName)
            assertFormattedEquals("Shibuya", t.endStation?.stationName)
            assertEquals(Instant.parse("2011-03-12T03:42:00Z"), t.startTimestamp)
            assertEquals(Instant.parse("2011-03-12T03:52:00Z"), t.endTimestamp)
        }
    }

    // --- PASMO (FeliCa) ---

    @Test
    fun testPasmoFromFlipper() {
        val data = loadFlipperDump("PASMO.nfc")
        val rawCard = FlipperNfcParser.parse(data)
        assertNotNull(rawCard, "Failed to parse PASMO Flipper dump")

        val card = rawCard.parse()
        assertTrue(card is FelicaCard, "Expected FelicaCard, got ${card::class.simpleName}")

        val factory = SuicaTransitFactory()
        assertTrue(factory.check(card), "Suica factory should recognize PASMO card")

        val identity = factory.parseIdentity(card)
        assertFormattedEquals("PASMO", identity.name)
        assertNull(identity.serialNumber)

        val info = factory.parseInfo(card)
        assertNotNull(info, "Failed to parse PASMO transit info")
        assertTrue(info is SuicaTransitInfo)

        // Balance: 500 JPY
        val balances = info.balances
        assertNotNull(balances)
        assertEquals(1, balances.size)
        assertEquals(TransitCurrency.JPY(500), balances[0].balance)

        val trips = info.trips
        assertNotNull(trips)
        assertEquals(11, trips.size)

        assertNull(info.subscriptions)

        // Trip 0: New Issue (ticket machine), -500 JPY
        trips[0].let { t ->
            assertEquals(Trip.Mode.TICKET_MACHINE, t.mode)
            assertEquals(TransitCurrency.JPY(-500), t.fare)
            assertNull(t.agencyName)
            assertFormattedEquals("Ticket Machine New Issue", t.routeName)
            assertNull(t.startStation)
        }

        // Trip 1: Tokyo Metro Ginza — Shibuya to Aoyamaitchome, 160 JPY
        trips[1].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.JPY(160), t.fare)
            assertFormattedEquals("Tokyo Metro", t.agencyName)
            assertFormattedEquals("#3 Ginza", t.routeName)
            assertFormattedEquals("Shibuya", t.startStation?.stationName)
            assertFormattedEquals("Aoyamaitchōme", t.endStation?.stationName)
        }

        // Trip 2: Toei Oedo — Aoyamaitchome to Tsukijishijo, 100 JPY
        trips[2].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.JPY(100), t.fare)
            assertFormattedEquals("Toei", t.agencyName)
            assertFormattedEquals("#12 Ōedo", t.routeName)
            assertFormattedEquals("Aoyamaitchōme", t.startStation?.stationName)
            assertFormattedEquals("Tsukijiichiba", t.endStation?.stationName)
        }

        // Trip 3: Simple Deposit Machine Charge, -1000 JPY
        trips[3].let { t ->
            assertEquals(Trip.Mode.TICKET_MACHINE, t.mode)
            assertEquals(TransitCurrency.JPY(-1000), t.fare)
            assertNull(t.agencyName)
            assertFormattedEquals("Simple Deposit Machine Charge", t.routeName)
            assertNull(t.startStation)
        }

        // Trip 4: Toei Oedo — Tsukijishijo to Kuramae, 210 JPY
        trips[4].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.JPY(210), t.fare)
            assertFormattedEquals("Toei", t.agencyName)
            assertFormattedEquals("#12 Ōedo", t.routeName)
            assertFormattedEquals("Tsukijiichiba", t.startStation?.stationName)
            assertFormattedEquals("Kuramae", t.endStation?.stationName)
        }

        // Trip 5: Toei Asakusa — Asakusa to Shinbashi, 210 JPY
        trips[5].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.JPY(210), t.fare)
            assertFormattedEquals("Toei", t.agencyName)
            assertFormattedEquals("#1 Asakusa", t.routeName)
            assertFormattedEquals("Asakusa", t.startStation?.stationName)
            assertFormattedEquals("Shinbashi", t.endStation?.stationName)
        }

        // Trip 6: Yurikamome — Shinbashi to Oumi, 370 JPY (with end timestamp next day)
        trips[6].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.JPY(370), t.fare)
            assertFormattedEquals("Yurikamome", t.agencyName)
            assertFormattedEquals("Tokyo Waterfront New Transit", t.routeName)
            assertFormattedEquals("Shinbashi", t.startStation?.stationName)
            assertFormattedEquals("Oumi", t.endStation?.stationName)
            assertEquals(Instant.parse("2011-06-12T15:00:00Z"), t.startTimestamp)
            assertEquals(Instant.parse("2011-06-13T05:45:00Z"), t.endTimestamp)
        }

        // Trip 7: Fare Adjustment Machine Charge, -1000 JPY
        trips[7].let { t ->
            assertEquals(Trip.Mode.TICKET_MACHINE, t.mode)
            assertEquals(TransitCurrency.JPY(-1000), t.fare)
            assertNull(t.agencyName)
            assertFormattedEquals("Fare Adjustment Machine Charge", t.routeName)
            assertNull(t.startStation)
        }

        // Trip 8: TWR Rinkai — Tokyo Teleport to Shinjuku, 480 JPY
        trips[8].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.JPY(480), t.fare)
            assertFormattedEquals("Tokyo Waterfront Area Rapid Transit", t.agencyName)
            assertFormattedEquals("Rinkai", t.routeName)
            assertFormattedEquals("Tokyo Teleport", t.startStation?.stationName)
            assertFormattedEquals("Shinjuku", t.endStation?.stationName)
            assertEquals(Instant.parse("2011-06-13T07:37:00Z"), t.startTimestamp)
            assertEquals(Instant.parse("2011-06-13T08:19:00Z"), t.endTimestamp)
        }

        // Trip 9: POS purchase, 550 JPY
        trips[9].let { t ->
            assertEquals(Trip.Mode.POS, t.mode)
            assertEquals(TransitCurrency.JPY(550), t.fare)
            assertNull(t.agencyName)
            assertFormattedEquals("Point of Sale Terminal Merchandise", t.routeName)
            assertNull(t.startStation)
            assertEquals(Instant.parse("2011-06-14T06:39:00Z"), t.startTimestamp)
        }

        // Trip 10: POS purchase, 420 JPY
        trips[10].let { t ->
            assertEquals(Trip.Mode.POS, t.mode)
            assertEquals(TransitCurrency.JPY(420), t.fare)
            assertNull(t.agencyName)
            assertFormattedEquals("Point of Sale Terminal Merchandise", t.routeName)
            assertNull(t.startStation)
            assertEquals(Instant.parse("2011-06-14T06:59:00Z"), t.startTimestamp)
        }
    }

    // --- ICOCA (FeliCa) ---

    @Test
    fun testIcocaFromFlipper() {
        val data = loadFlipperDump("ICOCA.nfc")
        val rawCard = FlipperNfcParser.parse(data)
        assertNotNull(rawCard, "Failed to parse ICOCA Flipper dump")

        val card = rawCard.parse()
        assertTrue(card is FelicaCard, "Expected FelicaCard, got ${card::class.simpleName}")

        val factory = SuicaTransitFactory()
        assertTrue(factory.check(card), "Suica factory should recognize ICOCA card")

        val identity = factory.parseIdentity(card)
        assertFormattedEquals("ICOCA", identity.name)
        assertNull(identity.serialNumber)

        val info = factory.parseInfo(card)
        assertNotNull(info, "Failed to parse ICOCA transit info")
        assertTrue(info is SuicaTransitInfo)

        // Balance: 827 JPY
        val balances = info.balances
        assertNotNull(balances)
        assertEquals(1, balances.size)
        assertEquals(TransitCurrency.JPY(827), balances[0].balance)

        val trips = info.trips
        assertNotNull(trips)
        assertEquals(20, trips.size)

        assertNull(info.subscriptions)

        // Trip 0: Vending Machine, 0 JPY
        trips[0].let { t ->
            assertEquals(Trip.Mode.VENDING_MACHINE, t.mode)
            assertEquals(TransitCurrency.JPY(0), t.fare)
            assertNull(t.agencyName)
            assertFormattedEquals("Vending Machine Merchandise", t.routeName)
            assertNull(t.startStation)
            assertEquals(Instant.parse("2011-06-05T23:46:00Z"), t.startTimestamp)
        }

        // Trip 1: POS, 734 JPY
        trips[1].let { t ->
            assertEquals(Trip.Mode.POS, t.mode)
            assertEquals(TransitCurrency.JPY(734), t.fare)
            assertFormattedEquals("Point of Sale Terminal Merchandise", t.routeName)
            assertEquals(Instant.parse("2011-06-07T00:33:00Z"), t.startTimestamp)
        }

        // Trip 2: Ticket Machine Charge, -2000 JPY
        trips[2].let { t ->
            assertEquals(Trip.Mode.TICKET_MACHINE, t.mode)
            assertEquals(TransitCurrency.JPY(-2000), t.fare)
            assertFormattedEquals("Ticket Machine Charge", t.routeName)
        }

        // Trip 3: POS, 958 JPY
        trips[3].let { t ->
            assertEquals(Trip.Mode.POS, t.mode)
            assertEquals(TransitCurrency.JPY(958), t.fare)
            assertFormattedEquals("Point of Sale Terminal Merchandise", t.routeName)
            assertEquals(Instant.parse("2011-06-07T00:57:00Z"), t.startTimestamp)
        }

        // Trip 4: Keihan — Tofukuji to Demachiyanagi, 260 JPY
        trips[4].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.JPY(260), t.fare)
            assertFormattedEquals("Keihan Electric Railway", t.agencyName)
            assertFormattedEquals("Keihanhon", t.routeName)
            assertFormattedEquals("Tōfukuji", t.startStation?.stationName)
            assertFormattedEquals("Demachiyanagi", t.endStation?.stationName)
        }

        // Trip 5: Console 0x21 Charge, -1000 JPY
        trips[5].let { t ->
            assertEquals(Trip.Mode.TICKET_MACHINE, t.mode)
            assertEquals(TransitCurrency.JPY(-1000), t.fare)
            assertFormattedEquals("Console 0x21 Charge", t.routeName)
        }

        // Trip 6: Kyoto Subway Karasuma — Kyoto to Nijojomae, 250 JPY
        trips[6].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.JPY(250), t.fare)
            assertFormattedEquals("Kyoto Subway", t.agencyName)
            assertFormattedEquals("Karasuma", t.routeName)
            assertFormattedEquals("Kyōto", t.startStation?.stationName)
            assertFormattedEquals("Nijōjōmae", t.endStation?.stationName)
        }

        // Trip 7: Osaka Subway #1 — Shinosaka to Namba, 270 JPY
        trips[7].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.JPY(270), t.fare)
            assertFormattedEquals("Osaka Subway", t.agencyName)
            assertFormattedEquals("#1", t.routeName)
            assertFormattedEquals("Shinōsaka", t.startStation?.stationName)
            assertFormattedEquals("Nanba", t.endStation?.stationName)
        }

        // Trip 8: Osaka Subway #1 — Namba to Bentencho, 230 JPY
        trips[8].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.JPY(230), t.fare)
            assertFormattedEquals("Osaka Subway", t.agencyName)
            assertFormattedEquals("#1", t.routeName)
            assertFormattedEquals("Nanba", t.startStation?.stationName)
            assertFormattedEquals("Bentenchō", t.endStation?.stationName)
        }

        // Trip 9: POS, 700 JPY
        trips[9].let { t ->
            assertEquals(Trip.Mode.POS, t.mode)
            assertEquals(TransitCurrency.JPY(700), t.fare)
            assertFormattedEquals("Point of Sale Terminal Merchandise", t.routeName)
            assertEquals(Instant.parse("2011-06-08T07:35:00Z"), t.startTimestamp)
        }

        // Trip 10: Simple Deposit Machine Charge, -2000 JPY
        trips[10].let { t ->
            assertEquals(Trip.Mode.TICKET_MACHINE, t.mode)
            assertEquals(TransitCurrency.JPY(-2000), t.fare)
            assertFormattedEquals("Simple Deposit Machine Charge", t.routeName)
        }

        // Trip 11: JR West Osaka Loop — Bentencho to Palace of cherry, 170 JPY
        trips[11].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.JPY(170), t.fare)
            assertFormattedEquals("JR West", t.agencyName)
            assertFormattedEquals("Ōsaka Loop", t.routeName)
            assertFormattedEquals("Bentenchō", t.startStation?.stationName)
            assertFormattedEquals("Palace of cherry", t.endStation?.stationName)
        }

        // Trip 12: Osaka Subway #1 — Umeda to Shinsaibashi, 230 JPY
        trips[12].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.JPY(230), t.fare)
            assertFormattedEquals("Osaka Subway", t.agencyName)
            assertFormattedEquals("#1", t.routeName)
            assertFormattedEquals("Umeda", t.startStation?.stationName)
            assertFormattedEquals("Shinsaibashi", t.endStation?.stationName)
        }

        // Trip 13: Osaka Subway #1 — Yodoyabashi to Namba, 200 JPY
        trips[13].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.JPY(200), t.fare)
            assertFormattedEquals("Osaka Subway", t.agencyName)
            assertFormattedEquals("#1", t.routeName)
            assertFormattedEquals("Yodoyabashi", t.startStation?.stationName)
            assertFormattedEquals("Nanba", t.endStation?.stationName)
        }

        // Trip 14: Kintetsu Namba — Osakanamba to Kintetsunara, 540 JPY
        trips[14].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.JPY(540), t.fare)
            assertFormattedEquals("Kintetsu", t.agencyName)
            assertFormattedEquals("Nanba", t.routeName)
            assertFormattedEquals("Ōsakananba", t.startStation?.stationName)
            assertFormattedEquals("Kintetsunara", t.endStation?.stationName)
        }

        // Trip 15: Nara Kotsu bus — Nitta, 200 JPY
        trips[15].let { t ->
            assertEquals(Trip.Mode.BUS, t.mode)
            assertEquals(TransitCurrency.JPY(200), t.fare)
            assertFormattedEquals("Narakōtsū", t.agencyName)
            assertNull(t.routeName)
            assertFormattedEquals("Nitta", t.startStation?.stationName)
            assertNull(t.endStation)
        }

        // Trip 16: Vending Machine, 400 JPY
        trips[16].let { t ->
            assertEquals(Trip.Mode.VENDING_MACHINE, t.mode)
            assertEquals(TransitCurrency.JPY(400), t.fare)
            assertFormattedEquals("Vending Machine Merchandise", t.routeName)
            assertEquals(Instant.parse("2011-06-11T05:21:00Z"), t.startTimestamp)
        }

        // Trip 17: Vending Machine, 150 JPY
        trips[17].let { t ->
            assertEquals(Trip.Mode.VENDING_MACHINE, t.mode)
            assertEquals(TransitCurrency.JPY(150), t.fare)
            assertFormattedEquals("Vending Machine Merchandise", t.routeName)
            assertEquals(Instant.parse("2011-06-11T07:32:00Z"), t.startTimestamp)
        }

        // Trip 18: Vending Machine, 100 JPY
        trips[18].let { t ->
            assertEquals(Trip.Mode.VENDING_MACHINE, t.mode)
            assertEquals(TransitCurrency.JPY(100), t.fare)
            assertFormattedEquals("Vending Machine Merchandise", t.routeName)
            assertEquals(Instant.parse("2011-06-14T03:19:00Z"), t.startTimestamp)
        }

        // Trip 19: Kyoto Subway Tozai — Higashiyama to Kyoto, 260 JPY (most recent, 2018)
        trips[19].let { t ->
            assertEquals(Trip.Mode.METRO, t.mode)
            assertEquals(TransitCurrency.JPY(260), t.fare)
            assertFormattedEquals("Kyoto Subway", t.agencyName)
            assertFormattedEquals("Tōzai", t.routeName)
            assertFormattedEquals("Higashiyama", t.startStation?.stationName)
            assertFormattedEquals("Kyōto", t.endStation?.stationName)
            assertEquals(Instant.parse("2018-09-17T00:11:00Z"), t.startTimestamp)
            assertEquals(Instant.parse("2018-09-17T00:29:00Z"), t.endTimestamp)
        }
    }
}
