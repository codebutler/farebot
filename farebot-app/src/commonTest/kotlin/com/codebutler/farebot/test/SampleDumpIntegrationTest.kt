/*
 * SampleDumpIntegrationTest.kt
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

import com.codebutler.farebot.card.cepas.CEPASCard
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.desfire.DesfireCard
import com.codebutler.farebot.card.iso7816.ISO7816Card
import com.codebutler.farebot.card.ultralight.UltralightCard
import com.codebutler.farebot.shared.serialize.CardImporter
import com.codebutler.farebot.shared.serialize.ImportResult
import com.codebutler.farebot.shared.serialize.KotlinxCardSerializer
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.easycard.EasyCardTransitFactory
import com.codebutler.farebot.transit.easycard.EasyCardTransitInfo
import com.codebutler.farebot.transit.ezlink.EZLinkTransitFactory
import com.codebutler.farebot.transit.ezlink.EZLinkTransitInfo
import com.codebutler.farebot.transit.lax_tap.LaxTapTransitFactory
import com.codebutler.farebot.transit.lax_tap.LaxTapTransitInfo
import com.codebutler.farebot.transit.msp_goto.MspGotoTransitFactory
import com.codebutler.farebot.transit.msp_goto.MspGotoTransitInfo
import com.codebutler.farebot.transit.myki.MykiTransitFactory
import com.codebutler.farebot.transit.myki.MykiTransitInfo
import com.codebutler.farebot.transit.seq_go.SeqGoTransitFactory
import com.codebutler.farebot.transit.seq_go.SeqGoTransitInfo
import com.codebutler.farebot.transit.yvr_compass.CompassUltralightTransitInfo
import com.codebutler.farebot.transit.hsl.HSLTransitFactory
import com.codebutler.farebot.transit.hsl.HSLTransitInfo
import com.codebutler.farebot.transit.hsl.HSLUltralightTransitFactory
import com.codebutler.farebot.transit.hsl.HSLUltralightTransitInfo
import com.codebutler.farebot.transit.calypso.mobib.MobibTransitInfo
import com.codebutler.farebot.card.felica.FelicaCard
import com.codebutler.farebot.transit.bilhete_unico.BilheteUnicoSPTransitFactory
import com.codebutler.farebot.transit.bilhete_unico.BilheteUnicoSPTransitInfo
import com.codebutler.farebot.transit.octopus.OctopusTransitFactory
import com.codebutler.farebot.transit.octopus.OctopusTransitInfo
import com.codebutler.farebot.transit.opal.OpalTransitFactory
import com.codebutler.farebot.transit.opal.OpalTransitInfo
import com.codebutler.farebot.transit.serialonly.HoloTransitFactory
import com.codebutler.farebot.transit.serialonly.HoloTransitInfo
import com.codebutler.farebot.transit.serialonly.SerialOnlyTransitInfo
import com.codebutler.farebot.transit.serialonly.TrimetHopTransitFactory
import com.codebutler.farebot.transit.serialonly.TrimetHopTransitInfo
import com.codebutler.farebot.transit.tmoney.TMoneyTransitFactory
import com.codebutler.farebot.transit.tmoney.TMoneyTransitInfo
import com.codebutler.farebot.transit.troika.TroikaUltralightTransitFactory
import com.codebutler.farebot.transit.troika.TroikaUltralightTransitInfo
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests that load sample dump files (Metrodroid JSON format) and exercise
 * the full parsing pipeline: JSON dump -> CardImporter -> raw card -> parsed card -> transit info.
 *
 * Each test verifies that:
 * 1. The dump file can be loaded and parsed by CardImporter
 * 2. The transit factory recognizes the card
 * 3. Identity (name, serial number) is parsed correctly
 * 4. Transit info (balances, trips, subscriptions) matches expected values
 *
 * These sample files are the same ones used on the Explore screen.
 */
class SampleDumpIntegrationTest : CardDumpTest() {

    private val stringResource = TestStringResource()

    // --- Opal (DESFire) ---
    // Source: Metrodroid test asset opal-transit-litter.json
    // Card: Opal, Sydney, Australia
    // Balance: -$1.82 AUD, 2 weekly trips, serial: 3085 2200 7856 2242

    @Test
    fun testOpalDump() {
        val factory = OpalTransitFactory(stringResource)
        val (card, info) = loadAndParseMetrodroidJson<DesfireCard, OpalTransitInfo>(
            "opal/Opal.json", factory
        )

        val identity = factory.parseIdentity(card)
        assertEquals("Opal", identity.name)
        assertEquals("3085 2200 7856 2242", identity.serialNumber)

        // Balance: -$1.82 AUD (-182 cents)
        val balances = info.balances
        assertNotNull(balances)
        assertEquals(1, balances.size)
        assertEquals(TransitCurrency.AUD(-182), balances[0].balance)

        assertEquals(2, info.weeklyTrips)

        // Last transaction is now exposed as a Trip (OpalTrip)
        val trips = info.trips
        assertNotNull(trips)
        assertEquals(1, trips.size)
        assertEquals(Trip.Mode.TRAIN, trips[0].mode)
        assertNotNull(trips[0].startTimestamp)
    }

    // --- HSL v2 (DESFire) ---
    // Source: Metrodroid test asset hslv2.json
    // Card: HSL, Helsinki, Finland
    // Balance: €0.40, 2 trips, 2 subscriptions, serial: 924620 0011 2345 6789

    @Test
    fun testHSLv2Dump() {
        val factory = HSLTransitFactory(stringResource)
        val (card, info) = loadAndParseMetrodroidJson<DesfireCard, HSLTransitInfo>(
            "hsl/HSLv2.json", factory
        )

        val identity = factory.parseIdentity(card)
        assertEquals("HSL", identity.name)
        assertEquals("924620 0011 2345 6789", identity.serialNumber)

        // Balance: €0.40 (40 EUR cents)
        val balances = info.balances
        assertNotNull(balances)
        assertEquals(1, balances.size)
        assertEquals(TransitCurrency.EUR(40), balances[0].balance)

        // 2 trips
        val trips = info.trips
        assertNotNull(trips)
        assertEquals(2, trips.size)

        // 2 subscriptions
        val subs = info.subscriptions
        assertNotNull(subs)
        assertEquals(2, subs.size)
    }

    // --- HSL Ultralight ---
    // Source: Metrodroid test asset hslul.json
    // Card: HSL Ultralight, Helsinki, Finland
    // 1 trip, 1 subscription, serial: 924621 0011 2376 7806

    @Test
    fun testHSLUltralightDump() {
        val factory = HSLUltralightTransitFactory()
        val (card, info) = loadAndParseMetrodroidJson<UltralightCard, HSLUltralightTransitInfo>(
            "hsl/HSL_UL.json", factory
        )

        val identity = factory.parseIdentity(card)
        assertEquals("HSL Ultralight", identity.name)

        // 1 trip
        val trips = info.trips
        assertNotNull(trips)
        assertTrue(trips.isNotEmpty(), "Should have at least one trip")

        // 1 subscription
        val subs = info.subscriptions
        assertNotNull(subs)
        assertTrue(subs.isNotEmpty(), "Should have at least one subscription")
    }

    // --- Troika UL (Ultralight) ---
    // Source: Metrodroid test asset troikaul.json
    // Card: Troika Ultralight, Moscow, Russia
    // 1 trip (bus), 1 subscription, serial: 0305 419 896

    @Test
    fun testTroikaUltralightDump() {
        val factory = TroikaUltralightTransitFactory()
        val (card, info) = loadAndParseMetrodroidJson<UltralightCard, TroikaUltralightTransitInfo>(
            "troika/TroikaUL.json", factory
        )

        val identity = factory.parseIdentity(card)
        assertEquals("Troika", identity.name)
        assertNotNull(identity.serialNumber)

        // Should have trips
        val trips = info.trips
        assertNotNull(trips)
        assertTrue(trips.isNotEmpty(), "Should have at least one trip")

        // Should have subscriptions
        val subs = info.subscriptions
        assertNotNull(subs)
        assertTrue(subs.isNotEmpty(), "Should have at least one subscription")
    }

    // --- T-Money (ISO7816) ---
    // Source: Metrodroid test asset oldtmoney.json
    // Card: T-Money, Seoul, South Korea
    // Balance: 17,650 KRW, 5 trips, serial: 1010 0300 0012 3456

    @Test
    fun testTMoneyDump() {
        val factory = TMoneyTransitFactory()
        val (card, info) = loadAndParseMetrodroidJson<ISO7816Card, TMoneyTransitInfo>(
            "tmoney/TMoney.json", factory
        )

        val identity = factory.parseIdentity(card)
        assertEquals("T-Money", identity.name)
        assertNotNull(identity.serialNumber)

        // Balance: 17,650 KRW
        val balances = info.balances
        assertNotNull(balances)
        assertEquals(1, balances.size)
        assertEquals(TransitCurrency.KRW(17650), balances[0].balance)

        // 5 trips
        val trips = info.trips
        assertNotNull(trips)
        assertTrue(trips.isNotEmpty(), "Should have trips")
    }

    // --- EZ-Link/NETS (CEPAS) ---
    // Source: Metrodroid test asset legacy.json
    // Card: EZ-Link/NETS, Singapore
    // Balance: $8.97 SGD (897 cents), 4 trips, serial: 1123456789123456

    @Test
    fun testEZLinkDump() {
        val factory = EZLinkTransitFactory(stringResource)
        val (card, info) = loadAndParseMetrodroidJson<CEPASCard, EZLinkTransitInfo>(
            "cepas/EZLink.json", factory
        )

        val identity = factory.parseIdentity(card)
        // CAN "112..." maps to generic CEPAS issuer (not specifically EZ-Link "100...")
        assertNotNull(identity.name)
        assertNotNull(identity.serialNumber)

        // Balance: $8.97 SGD (897 cents)
        val balances = info.balances
        assertNotNull(balances)
        assertEquals(1, balances.size)
        assertEquals(TransitCurrency.SGD(897), balances[0].balance)

        // 4 trips: BUS, BUS_REFUND, MRT, CREATION
        val trips = info.trips
        assertNotNull(trips)
        assertEquals(4, trips.size)

        // Verify expected modes are present
        val modes = trips.map { it.mode }
        assertEquals(2, modes.count { it == Trip.Mode.BUS }, "Should have 2 BUS trips (bus + refund)")
        assertEquals(1, modes.count { it == Trip.Mode.METRO }, "Should have 1 MRT trip")
        assertEquals(1, modes.count { it == Trip.Mode.OTHER }, "Should have 1 OTHER trip (creation)")

        // MRT trip should have stations
        val mrtTrip = trips.first { it.mode == Trip.Mode.METRO }
        assertNotNull(mrtTrip.startStation, "MRT trip should have a start station")
        assertNotNull(mrtTrip.endStation, "MRT trip should have an end station")

        // Bus trips should not have stations (BUS_REFUND userData handled via toStationOrNull)
        trips.filter { it.mode == Trip.Mode.BUS }.forEach { busTrip ->
            assertNull(busTrip.startStation, "Bus trip should not have a station")
        }

        // CREATION trip should not have a station (blank userData nullified by toStationOrNull)
        val creationTrip = trips.first { it.mode == Trip.Mode.OTHER }
        assertNull(creationTrip.startStation, "CREATION trip should not have a station for blank userData")
    }

    // --- Holo (DESFire, serial-only) ---
    // Source: Metrodroid test asset unused.json
    // Card: HOLO, Honolulu, Hawaii
    // Serial-only card (balance/history stored in central database)

    @Test
    fun testHoloDump() {
        val factory = HoloTransitFactory()
        val (card, info) = loadAndParseMetrodroidJson<DesfireCard, HoloTransitInfo>(
            "holo/Holo.json", factory
        )

        val identity = factory.parseIdentity(card)
        assertEquals("HOLO", identity.name)
        assertNotNull(identity.serialNumber)

        // Serial-only card: no balance, no trips, but has emptyStateMessage
        assertTrue(info is SerialOnlyTransitInfo)
        assertNotNull(info.emptyStateMessage, "Serial-only card should have an emptyStateMessage")
        assertNull(info.trips, "Serial-only card should have null trips")
        assertTrue(info.balances.isNullOrEmpty(), "Serial-only card should have no balances")

        // Holo has extraInfo (last transaction, manufacturing ID)
        assertNotNull(info.info, "Holo should have info items")
        assertTrue(info.info!!.isNotEmpty(), "Holo should have at least one info item")
    }

    // --- Mobib (Calypso, blank card) ---
    // Source: Metrodroid test asset mobib_blank.json
    // Card: Mobib, Brussels, Belgium
    // Blank card: 0 trips, 0 subscriptions

    @Test
    fun testMobibDump() {
        val factory = MobibTransitInfo.Factory(stringResource)
        val rawCard = TestAssetLoader.loadMetrodroidJsonCard("mobib/Mobib.json")
        val card = rawCard.parse() as ISO7816Card
        assertTrue(factory.check(card), "Mobib factory should recognize this card")

        val identity = factory.parseIdentity(card)
        assertEquals("Mobib", identity.name)
        assertNotNull(identity.serialNumber)

        val info = factory.parseInfo(card)
        assertNotNull(info, "Failed to parse Mobib transit info")
        assertTrue(info is MobibTransitInfo)

        // Blank card — no trips
        val trips = info.trips
        assertNotNull(trips)
        assertEquals(0, trips.size)
    }

    // --- EasyCard (Classic, MFC binary) ---
    // Source: Test asset deadbeef.mfc (from Metrodroid's EasyCard test)
    // Card: EasyCard, Taipei, Taiwan
    // Tests the CardImporter.importMfcDump() path used by the Explore screen

    @Test
    fun testEasyCardMfcDump() {
        val bytes = loadTestResource("easycard/deadbeef.mfc")
        assertNotNull(bytes, "Test resource not found: easycard/deadbeef.mfc")

        val json = Json { isLenient = true; ignoreUnknownKeys = true }
        val importer = CardImporter.create(KotlinxCardSerializer(json))
        val result = importer.importMfcDump(bytes)
        assertTrue(result is ImportResult.Success, "Failed to import MFC dump: $result")

        val rawCard = (result as ImportResult.Success).cards.first()
        val card = rawCard.parse() as ClassicCard

        val factory = EasyCardTransitFactory(stringResource)
        assertTrue(factory.check(card), "EasyCard factory should recognize this card")

        val identity = factory.parseIdentity(card)
        assertNotNull(identity.name)

        val info = factory.parseInfo(card)
        assertNotNull(info, "Failed to parse EasyCard transit info")
        assertTrue(info is EasyCardTransitInfo)

        // Balance: 245 TWD
        val balances = info.balances
        assertNotNull(balances)
        assertEquals(1, balances.size)
        assertEquals(TransitCurrency.TWD(245), balances[0].balance)

        // 3 trips: bus, metro (merged tap-on/off), refill
        val trips = info.trips
        assertNotNull(trips)
        assertEquals(3, trips.size)
        assertEquals(Trip.Mode.BUS, trips[0].mode)
        assertEquals(Trip.Mode.METRO, trips[1].mode)
        assertEquals(Trip.Mode.TICKET_MACHINE, trips[2].mode)
    }

    // --- Compass (Ultralight) ---
    // Source: CompassTransitTest LENREK_TEST_DATA[0]
    // Card: Compass, Vancouver, Canada
    // Serial: 0001 0084 2851 9244 6735

    @Test
    fun testCompassDump() {
        val factory = CompassUltralightTransitInfo.FACTORY
        val (card, info) = loadAndParseMetrodroidJson<UltralightCard, CompassUltralightTransitInfo>(
            "compass/Compass.json", factory
        )

        val identity = factory.parseIdentity(card)
        assertNotNull(identity.name)
        assertEquals("0001 0084 2851 9244 6735", identity.serialNumber)

        assertNotNull(info.serialNumber)

        // Should have trips (this card has 2 transaction records)
        val trips = info.trips
        assertNotNull(trips)
        assertTrue(trips.isNotEmpty(), "Should have at least one trip")
    }

    // --- SEQ Go (Classic, Nextfare) ---
    // Source: NextfareTransitTest test data
    // Card: SEQ Go, Brisbane, Australia
    // Serial: 0160 0012 3456 7893

    @Test
    fun testSeqGoDump() {
        val factory = SeqGoTransitFactory()
        val (card, info) = loadAndParseMetrodroidJson<ClassicCard, SeqGoTransitInfo>(
            "seqgo/SeqGo.json", factory
        )

        assertEquals("0160 0012 3456 7893", info.serialNumber)

        val balances = info.balances
        assertNotNull(balances)
        assertEquals("AUD", balances.first().balance.currencyCode)
    }

    // --- LAX TAP (Classic, Nextfare) ---
    // Source: NextfareTransitTest test data
    // Card: LAX TAP, Los Angeles, USA
    // Serial: 0160 0323 4663 8769

    @Test
    fun testLaxTapDump() {
        val factory = LaxTapTransitFactory()
        val (card, info) = loadAndParseMetrodroidJson<ClassicCard, LaxTapTransitInfo>(
            "laxtap/LaxTap.json", factory
        )

        assertEquals("0160 0323 4663 8769", info.serialNumber)

        val balances = info.balances
        assertNotNull(balances)
        assertEquals("USD", balances.first().balance.currencyCode)
    }

    // --- MSP GoTo (Classic, Nextfare) ---
    // Source: NextfareTransitTest test data
    // Card: MSP GoTo, Minneapolis, USA
    // Serial: 0160 0112 3581 3212

    @Test
    fun testMspGoToDump() {
        val factory = MspGotoTransitFactory()
        val (card, info) = loadAndParseMetrodroidJson<ClassicCard, MspGotoTransitInfo>(
            "mspgoto/MspGoTo.json", factory
        )

        assertEquals("0160 0112 3581 3212", info.serialNumber)

        val balances = info.balances
        assertNotNull(balances)
        assertEquals("USD", balances.first().balance.currencyCode)
    }

    // --- Myki (DESFire, serial-only) ---
    // Source: MykiTransitTest test data
    // Card: Myki, Melbourne, Australia
    // Serial: 308425123456780

    @Test
    fun testMykiDump() {
        val factory = MykiTransitFactory()
        val (card, info) = loadAndParseMetrodroidJson<DesfireCard, MykiTransitInfo>(
            "myki/Myki.json", factory
        )

        val identity = factory.parseIdentity(card)
        assertNotNull(identity.name)
        assertEquals("308425123456780", identity.serialNumber)

        // Serial-only card: has emptyStateMessage, no trips
        assertTrue(info is SerialOnlyTransitInfo)
        assertNotNull(info.emptyStateMessage, "Serial-only card should have an emptyStateMessage")
        assertNull(info.trips, "Serial-only card should have null trips")
    }

    // --- Octopus (FeliCa) ---
    // Source: OctopusTransitTest test data
    // Card: Octopus, Hong Kong
    // Balance: -HKD 14.40 (raw 0x164 = 356, offset 500, (356-500)*10 = -1440 cents)
    // scannedAt: 2017-10-02 (post-offset-change date)

    @Test
    fun testOctopusDump() {
        val factory = OctopusTransitFactory()
        val (card, info) = loadAndParseMetrodroidJson<FelicaCard, OctopusTransitInfo>(
            "octopus/Octopus.json", factory
        )

        val identity = factory.parseIdentity(card)
        assertNotNull(identity.name)

        val balances = info.balances
        assertNotNull(balances)
        assertEquals(1, balances.size)
        assertEquals(TransitCurrency.HKD(-1440), balances[0].balance)
    }

    // --- TriMet Hop (DESFire, serial-only) ---
    // Source: Synthetic dump based on TrimetHopTransitFactory data format
    // Card: Hop Fastpass, Portland, Oregon, USA
    // Serial: 01-001-12345678-RA, issue date: 2023-06-15

    @Test
    fun testTrimetHopDump() {
        val factory = TrimetHopTransitFactory()
        val (card, info) = loadAndParseMetrodroidJson<DesfireCard, TrimetHopTransitInfo>(
            "trimethop/TrimetHop.json", factory
        )

        val identity = factory.parseIdentity(card)
        assertEquals("Hop Fastpass", identity.name)
        assertEquals("01-001-12345678-RA", identity.serialNumber)

        assertTrue(info is TrimetHopTransitInfo)
        assertEquals("01-001-12345678-RA", info.serialNumber)

        // Serial-only card: has emptyStateMessage, no trips
        assertTrue(info is SerialOnlyTransitInfo)
        assertNotNull(info.emptyStateMessage, "Serial-only card should have an emptyStateMessage")
        assertNull(info.trips, "Serial-only card should have null trips")
    }

    // --- Bilhete Unico (Classic) ---
    // Source: Synthetic dump based on BilheteUnicoSPTransitFactory data format
    // Card: Bilhete Unico, Sao Paulo, Brazil
    // Balance: R$24.00 (2400 cents BRL), serial: 110 242901149

    @Test
    fun testBilheteUnicoDump() {
        val factory = BilheteUnicoSPTransitFactory()
        val (card, info) = loadAndParseMetrodroidJson<ClassicCard, BilheteUnicoSPTransitInfo>(
            "bilhete/BilheteUnico.json", factory
        )

        val identity = factory.parseIdentity(card)
        assertNotNull(identity.name)
        assertEquals("110 242901149", identity.serialNumber)

        // Balance: R$24.00 (2400 cents BRL)
        val balances = info.balances
        assertNotNull(balances)
        assertEquals(1, balances.size)
        assertEquals(TransitCurrency.BRL(2400), balances[0].balance)

        // Synthetic card has no trips
        val trips = info.trips
        assertNotNull(trips)
        assertEquals(0, trips.size)
    }
}
