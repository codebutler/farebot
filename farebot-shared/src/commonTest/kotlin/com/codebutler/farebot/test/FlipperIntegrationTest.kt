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
import com.codebutler.farebot.transit.clipper.ClipperTransitFactory
import com.codebutler.farebot.transit.clipper.ClipperTransitInfo
import com.codebutler.farebot.transit.orca.OrcaTransitFactory
import com.codebutler.farebot.transit.orca.OrcaTransitInfo
import com.codebutler.farebot.transit.suica.SuicaTransitFactory
import com.codebutler.farebot.transit.suica.SuicaTransitInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Full pipeline integration tests: Flipper NFC dump -> raw card -> parsed card -> transit info.
 *
 * These tests load real Flipper Zero NFC card dumps and exercise the complete parsing pipeline.
 */
class FlipperIntegrationTest {

    private val stringResource = TestStringResource()

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

        val factory = OrcaTransitFactory(stringResource)
        assertTrue(factory.check(card), "ORCA factory should recognize this card")

        val identity = factory.parseIdentity(card)
        assertEquals("ORCA", identity.name)

        val info = factory.parseInfo(card)
        assertNotNull(info, "Failed to parse ORCA transit info")
        assertTrue(info is OrcaTransitInfo)

        // Verify balance is present
        val balances = info.balances
        assertNotNull(balances, "ORCA balances should not be null")
        assertTrue(balances.isNotEmpty(), "ORCA should have at least one balance")

        // Serial number should be parsed
        assertNotNull(identity.serialNumber, "ORCA serial should not be null")
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
        assertEquals("Clipper", identity.name)

        val info = factory.parseInfo(card)
        assertNotNull(info, "Failed to parse Clipper transit info")
        assertTrue(info is ClipperTransitInfo)

        // Verify balance is present
        val balances = info.balances
        assertNotNull(balances, "Clipper balances should not be null")
        assertTrue(balances.isNotEmpty(), "Clipper should have at least one balance")

        // Serial number should be parsed from file 0x08
        assertNotNull(identity.serialNumber, "Clipper serial should not be null")

        // File 0x0e is a standard file with 512 bytes of trip data (16 records of 32 bytes)
        val trips = info.trips
        assertNotNull(trips, "Clipper trips should not be null")
        assertTrue(trips.isNotEmpty(), "Clipper should have at least one trip from file 0x0e")
    }

    // --- Suica (FeliCa) ---

    @Test
    fun testSuicaFromFlipper() {
        val data = loadFlipperDump("Suica.nfc")
        val rawCard = FlipperNfcParser.parse(data)
        assertNotNull(rawCard, "Failed to parse Suica Flipper dump")

        val card = rawCard.parse()
        assertTrue(card is FelicaCard, "Expected FelicaCard, got ${card::class.simpleName}")

        val factory = SuicaTransitFactory(stringResource)
        assertTrue(factory.check(card), "Suica factory should recognize this card")

        val identity = factory.parseIdentity(card)
        assertEquals("Suica", identity.name)

        val info = factory.parseInfo(card)
        assertNotNull(info, "Failed to parse Suica transit info")
        assertTrue(info is SuicaTransitInfo)

        // Suica has trip history in service 0x090F
        val trips = info.trips
        assertNotNull(trips, "Suica trips should not be null")
        assertTrue(trips.isNotEmpty(), "Suica should have trips from service 0x090F")

        // The dump has 20 blocks (indices 00-13) in service 090F
        // Many should parse as valid trips
        assertTrue(trips.size >= 5, "Expected at least 5 Suica trips, got ${trips.size}")
    }

    // --- PASMO (FeliCa) ---

    @Test
    fun testPasmoFromFlipper() {
        val data = loadFlipperDump("PASMO.nfc")
        val rawCard = FlipperNfcParser.parse(data)
        assertNotNull(rawCard, "Failed to parse PASMO Flipper dump")

        val card = rawCard.parse()
        assertTrue(card is FelicaCard, "Expected FelicaCard, got ${card::class.simpleName}")

        val factory = SuicaTransitFactory(stringResource)
        assertTrue(factory.check(card), "Suica factory should recognize PASMO card")

        val identity = factory.parseIdentity(card)
        assertEquals("PASMO", identity.name)

        val info = factory.parseInfo(card)
        assertNotNull(info, "Failed to parse PASMO transit info")
        assertTrue(info is SuicaTransitInfo)

        val trips = info.trips
        assertNotNull(trips, "PASMO trips should not be null")
        assertTrue(trips.isNotEmpty(), "PASMO should have trips")
    }

    // --- ICOCA (FeliCa) ---

    @Test
    fun testIcocaFromFlipper() {
        val data = loadFlipperDump("ICOCA.nfc")
        val rawCard = FlipperNfcParser.parse(data)
        assertNotNull(rawCard, "Failed to parse ICOCA Flipper dump")

        val card = rawCard.parse()
        assertTrue(card is FelicaCard, "Expected FelicaCard, got ${card::class.simpleName}")

        val factory = SuicaTransitFactory(stringResource)
        assertTrue(factory.check(card), "Suica factory should recognize ICOCA card")

        val identity = factory.parseIdentity(card)
        assertEquals("ICOCA", identity.name)

        val info = factory.parseInfo(card)
        assertNotNull(info, "Failed to parse ICOCA transit info")
        assertTrue(info is SuicaTransitInfo)

        val trips = info.trips
        assertNotNull(trips, "ICOCA trips should not be null")
        assertTrue(trips.isNotEmpty(), "ICOCA should have trips")
    }
}
