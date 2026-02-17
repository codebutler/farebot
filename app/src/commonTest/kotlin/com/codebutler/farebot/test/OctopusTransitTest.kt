/*
 * OctopusTransitTest.kt
 *
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
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

import com.codebutler.farebot.card.felica.FeliCaConstants
import com.codebutler.farebot.test.CardTestHelper.felicaBlock
import com.codebutler.farebot.test.CardTestHelper.felicaCard
import com.codebutler.farebot.test.CardTestHelper.felicaService
import com.codebutler.farebot.test.CardTestHelper.felicaSystem
import com.codebutler.farebot.test.CardTestHelper.hexToBytes
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.octopus.OctopusTransitFactory
import com.codebutler.farebot.transit.octopus.OctopusTransitInfo
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Tests for Octopus card.
 *
 * Ported from Metrodroid's OctopusTest.kt.
 *
 * Octopus cards use a date-dependent offset for balance calculation:
 * - Before 2017-10-01: offset = 350 (max negative balance was -$35)
 * - After 2017-10-01: offset = 500 (max negative balance increased to -$50)
 *
 * The raw value on the card is in 10-cent units, which is then multiplied by 10 to get cents.
 *
 * See: https://www.octopus.com.hk/en/consumer/customer-service/faq/get-your-octopus/about-octopus.html#3532
 */
class OctopusTransitTest {
    private val factory = OctopusTransitFactory()

    private fun octopusCardFromHex(
        s: String,
        scannedAt: Instant,
    ): com.codebutler.farebot.card.felica.FelicaCard {
        val data = hexToBytes(s)

        val blockBalance = felicaBlock(0, data)
        val serviceBalance = felicaService(FeliCaConstants.SERVICE_OCTOPUS, listOf(blockBalance))

        // Don't know what the purpose of this is, but it appears empty.
        val blockUnknown = felicaBlock(0, ByteArray(16))
        val serviceUnknown = felicaService(0x100b, listOf(blockUnknown))

        val system =
            felicaSystem(
                FeliCaConstants.SYSTEMCODE_OCTOPUS,
                listOf(serviceBalance, serviceUnknown),
            )

        return felicaCard(systems = listOf(system), scannedAt = scannedAt)
    }

    private suspend fun checkCard(
        card: com.codebutler.farebot.card.felica.FelicaCard,
        expectedBalance: TransitCurrency,
    ) {
        // Test factory detection
        assertTrue(factory.check(card))

        // Test TransitIdentity
        val identity = factory.parseIdentity(card)
        assertEquals(OctopusTransitInfo.OCTOPUS_NAME, identity.name.resolveAsync())

        // Test TransitData
        val info = factory.parseInfo(card)

        assertNotNull(info.balances)
        assertTrue(info.balances!!.isNotEmpty())
        assertEquals(expectedBalance, info.balances!!.first().balance)
    }

    @Test
    fun test2018Card() =
        runTest {
            // This data is from a card last used in 2018, but we've adjusted the date here to
            // 2017-10-02 to test the behaviour of OctopusData.getOctopusOffset.
            // Hex 00000164 = 356 decimal. Post-2017-10-01 offset = 500.
            // Balance = (356 - 500) * 10 = -1440 cents
            val scannedAt = LocalDateTime(2017, 10, 2, 0, 0).toInstant(TimeZone.UTC)
            val card = octopusCardFromHex("00000164000000000000000000000021", scannedAt)
            checkCard(card, TransitCurrency.HKD(-1440))
        }

    @Test
    fun test2016Card() =
        runTest {
            // Hex 00000152 = 338 decimal. Pre-2017-10-01 offset = 350.
            // Balance = (338 - 350) * 10 = -120 cents
            val scannedAt = LocalDateTime(2016, 1, 1, 0, 0).toInstant(TimeZone.UTC)
            val card = octopusCardFromHex("000001520000000000000000000086B1", scannedAt)
            checkCard(card, TransitCurrency.HKD(-120))
        }
}
