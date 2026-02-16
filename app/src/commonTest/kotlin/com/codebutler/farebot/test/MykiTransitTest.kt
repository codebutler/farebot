/*
 * MykiTransitTest.kt
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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
import com.codebutler.farebot.transit.myki.MykiTransitFactory
import com.codebutler.farebot.transit.myki.MykiTransitInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for Myki card.
 *
 * Ported from Metrodroid's MykiTest.kt.
 */
class MykiTransitTest {
    private val factory = MykiTransitFactory()

    private fun constructMykiCardFromHexString(s: String): com.codebutler.farebot.card.desfire.DesfireCard {
        val demoData = hexToBytes(s)

        // Construct a card to hold the data.
        // APP_ID_1 = 4594, APP_ID_2 = 15732978
        return desfireCard(
            applications =
                listOf(
                    desfireApp(4594, listOf(standardFile(15, demoData))),
                    desfireApp(15732978, emptyList()),
                ),
        )
    }

    @Test
    fun testDemoCard() {
        // This is mocked-up, incomplete data.
        val card = constructMykiCardFromHexString("C9B404004E61BC000000000000000000")

        // Verify the card has the expected DESFire application IDs
        assertEquals(2, card.applications.size)
        assertEquals(4594, card.applications[0].id) // APP_ID_1
        assertEquals(15732978, card.applications[1].id) // APP_ID_2

        // Verify the factory detects the card
        assertTrue(factory.check(card))

        // Test TransitIdentity
        val identity = factory.parseIdentity(card)
        assertEquals(MykiTransitInfo.NAME, identity.name)
        assertEquals("308425123456780", identity.serialNumber)

        // Test TransitData
        val info = factory.parseInfo(card)
        assertEquals("308425123456780", info.serialNumber)
    }
}
