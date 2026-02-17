/*
 * MetrodroidDumpIntegrationTest.kt
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

import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.raw.RawClassicBlock
import com.codebutler.farebot.card.classic.raw.RawClassicCard
import com.codebutler.farebot.card.classic.raw.RawClassicSector
import com.codebutler.farebot.card.ultralight.UltralightPage
import com.codebutler.farebot.card.ultralight.raw.RawUltralightCard
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.troika.TroikaTransitFactory
import com.codebutler.farebot.transit.ventra.VentraUltralightTransitInfo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Integration tests using card dump data sourced from Metrodroid GitHub issues.
 *
 * These tests construct raw cards from known dump data and exercise the complete
 * parsing pipeline: raw card -> parsed card -> transit info.
 *
 * Data sources:
 * - Ventra: metrodroid/metrodroid#855
 * - Troika Classic: metrodroid/metrodroid#735
 */
@OptIn(ExperimentalStdlibApi::class)
class MetrodroidDumpIntegrationTest {
    // --- Ventra (Ultralight) ---
    // Source: https://github.com/metrodroid/metrodroid/issues/855
    // Card: Ventra disposable 1-day pass, scanned 2024-02-15
    // Balance: $8.44 USD, 2 transactions

    @Test
    fun testVentraUltralight() =
        runTest {
            val pages =
                listOf(
                    UltralightPage.create(0, "04898386".hexToByteArray()),
                    UltralightPage.create(1, "ba8a1494".hexToByteArray()),
                    UltralightPage.create(2, "b0480000".hexToByteArray()),
                    UltralightPage.create(3, "00000000".hexToByteArray()),
                    UltralightPage.create(4, "0a04009a".hexToByteArray()),
                    UltralightPage.create(5, "30013f00".hexToByteArray()),
                    UltralightPage.create(6, "000000a4".hexToByteArray()),
                    UltralightPage.create(7, "7b7b1681".hexToByteArray()),
                    UltralightPage.create(8, "00000000".hexToByteArray()),
                    UltralightPage.create(9, "83690100".hexToByteArray()),
                    UltralightPage.create(10, "59300001".hexToByteArray()),
                    UltralightPage.create(11, "00001940".hexToByteArray()),
                    UltralightPage.create(12, "665a5a07".hexToByteArray()),
                    UltralightPage.create(13, "82690100".hexToByteArray()),
                    UltralightPage.create(14, "593d0001".hexToByteArray()),
                    UltralightPage.create(15, "00009e4f".hexToByteArray()),
                    UltralightPage.create(16, "000000ff".hexToByteArray()),
                    UltralightPage.create(17, "00050000".hexToByteArray()),
                    UltralightPage.create(18, "00000000".hexToByteArray()),
                    UltralightPage.create(19, "00000000".hexToByteArray()),
                )

            val rawCard =
                RawUltralightCard.create(
                    tagId = "048983ba8a1494".hexToByteArray(),
                    scannedAt = Instant.fromEpochMilliseconds(1708017434025),
                    pages = pages,
                    type = 1, // EV1
                )

            val card = rawCard.parse()

            val factory = VentraUltralightTransitInfo.FACTORY
            assertTrue(factory.check(card), "Ventra factory should recognize this card")

            val identity = factory.parseIdentity(card)
            assertEquals("Ventra", identity.name.resolveAsync())
            assertNotNull(identity.serialNumber, "Should have a serial number")

            val info = factory.parseInfo(card)

            // Balance: $8.44 USD (844 cents)
            val balances = info.balances
            assertNotNull(balances, "Should have balances")
            assertEquals(1, balances.size)
            assertEquals(TransitCurrency.USD(844), balances[0].balance)

            // Should have trips
            val trips = info.trips
            assertNotNull(trips, "Should have trips")
            assertTrue(trips.isNotEmpty(), "Should have at least one trip")
        }

    // --- Troika Classic (E/3 format, balance 0 RUB) ---
    // Source: https://github.com/metrodroid/metrodroid/issues/735
    // Card: Troika classic, layout E sublayout 3, Moscow Metro

    @Test
    fun testTroikaClassicE3() =
        runTest {
            val card =
                buildTroikaClassicCard(
                    // Sector 8 data: layout E, sublayout 3, balance 0 kopeks
                    sector8Block0 = "45DB101958FBCE19768AA40000000000".hexToByteArray(),
                    sector8Block1 = "2C013D460A001400000010009BB56E63".hexToByteArray(),
                    sector8Block2 = "2C013D460A001400000010009BB56E63".hexToByteArray(),
                )

            val factory = TroikaTransitFactory()
            assertTrue(factory.check(card), "Troika factory should recognize this card (E/3)")

            val identity = factory.parseIdentity(card)
            assertEquals("Troika", identity.name.resolveAsync())
            assertNotNull(identity.serialNumber, "Should have serial number")

            val info = factory.parseInfo(card)

            // Balance: 0.00 RUB (0 kopeks)
            val balances = info.balances
            assertNotNull(balances, "Should have balances")
            assertEquals(1, balances.size)
            assertEquals(TransitCurrency.RUB(0), balances[0].balance)
        }

    // --- Troika Classic (E/5 format, balance 50 RUB) ---
    // Source: https://github.com/metrodroid/metrodroid/issues/735#issuecomment-637891248
    // Card: Troika classic, layout E sublayout 5, Moscow Metro

    @Test
    fun testTroikaClassicE5() =
        runTest {
            val card =
                buildTroikaClassicCard(
                    // Sector 8 data: layout E, sublayout 5, balance 5000 kopeks (50 RUB)
                    sector8Block0 = "45DB101958FBCE2A4915216AA1A00800".hexToByteArray(),
                    sector8Block1 = "0D1C0A000004E20B004001001EB7ADDE".hexToByteArray(),
                    sector8Block2 = "0D1C0A000004E20B004001001EB7ADDE".hexToByteArray(),
                )

            val factory = TroikaTransitFactory()
            assertTrue(factory.check(card), "Troika factory should recognize this card (E/5)")

            val identity = factory.parseIdentity(card)
            assertEquals("Troika", identity.name.resolveAsync())
            assertNotNull(identity.serialNumber, "Should have serial number")

            val info = factory.parseInfo(card)

            // Balance: 50.00 RUB (5000 kopeks)
            val balances = info.balances
            assertNotNull(balances, "Should have balances")
            assertEquals(1, balances.size)
            assertEquals(TransitCurrency.RUB(5000), balances[0].balance)
        }

    /**
     * Builds a minimal Classic card with Troika data in sector 8.
     * All other sectors are filled with empty data so the card structure is valid.
     */
    private fun buildTroikaClassicCard(
        sector8Block0: ByteArray,
        sector8Block1: ByteArray,
        sector8Block2: ByteArray,
    ): ClassicCard {
        val emptyBlock = ByteArray(16)
        val sectorTrailer = ByteArray(16) { 0xFF.toByte() }

        val sectors =
            (0 until 16).map { sectorIndex ->
                if (sectorIndex == 8) {
                    RawClassicSector.createData(
                        sectorIndex,
                        listOf(
                            RawClassicBlock.create(0, sector8Block0),
                            RawClassicBlock.create(1, sector8Block1),
                            RawClassicBlock.create(2, sector8Block2),
                            RawClassicBlock.create(3, sectorTrailer),
                        ),
                    )
                } else {
                    RawClassicSector.createData(
                        sectorIndex,
                        listOf(
                            RawClassicBlock.create(0, emptyBlock),
                            RawClassicBlock.create(1, emptyBlock),
                            RawClassicBlock.create(2, emptyBlock),
                            RawClassicBlock.create(3, sectorTrailer),
                        ),
                    )
                }
            }

        return RawClassicCard
            .create(
                tagId = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte()),
                scannedAt = Instant.fromEpochSeconds(1590969600), // 2020-06-01T00:00:00Z
                sectors = sectors,
            ).parse()
    }
}
