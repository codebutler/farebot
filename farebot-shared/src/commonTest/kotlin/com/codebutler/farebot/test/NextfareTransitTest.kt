/*
 * NextfareTransitTest.kt
 *
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
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

import com.codebutler.farebot.card.classic.ClassicBlock
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.test.CardTestHelper.hexToBytes
import com.codebutler.farebot.transit.lax_tap.LaxTapTransitFactory
import com.codebutler.farebot.transit.lax_tap.LaxTapTransitInfo
import com.codebutler.farebot.transit.msp_goto.MspGotoTransitFactory
import com.codebutler.farebot.transit.msp_goto.MspGotoTransitInfo
import com.codebutler.farebot.transit.nextfare.NextfareTransitInfo
import com.codebutler.farebot.transit.seq_go.SeqGoTransitFactory
import com.codebutler.farebot.transit.seq_go.SeqGoTransitInfo
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Card-level tests for Cubic Nextfare reader.
 *
 * Ported from Metrodroid's NextfareTest.kt (card-level tests).
 */
class NextfareTransitTest {

    private fun buildNextfareCard(
        uid: ByteArray,
        systemCode: ByteArray,
        block2: ByteArray? = null
    ): ClassicCard {
        require(systemCode.size == 6)
        require(uid.size == 4)

        val trailer = hexToBytes("ffffffffffff78778800a1a2a3a4a5a6")
        val keyA = hexToBytes("ffffffffffff")

        val sectors = mutableListOf<DataClassicSector>()

        val b2data = block2 ?: ByteArray(0)
        val block0Data = uid + ByteArray(16 - uid.size)
        val block1Data = byteArrayOf(0) + NextfareTransitInfo.MANUFACTURER + systemCode + byteArrayOf(0)
        val block2Data = b2data + ByteArray(16 - b2data.size)

        sectors += DataClassicSector(
            index = 0,
            blocks = listOf(
                ClassicBlock.create(ClassicBlock.TYPE_MANUFACTURER, 0, block0Data),
                ClassicBlock.create(ClassicBlock.TYPE_DATA, 1, block1Data),
                ClassicBlock.create(ClassicBlock.TYPE_DATA, 2, block2Data),
                ClassicBlock.create(ClassicBlock.TYPE_TRAILER, 3, trailer)
            ),
            keyA = keyA
        )

        for (sectorNum in 1..15) {
            sectors += DataClassicSector(
                index = sectorNum,
                blocks = listOf(
                    ClassicBlock.create(ClassicBlock.TYPE_DATA, 0, ByteArray(16)),
                    ClassicBlock.create(ClassicBlock.TYPE_DATA, 1, ByteArray(16)),
                    ClassicBlock.create(ClassicBlock.TYPE_DATA, 2, ByteArray(16)),
                    ClassicBlock.create(ClassicBlock.TYPE_TRAILER, 3, trailer)
                ),
                keyA = keyA
            )
        }

        return CardTestHelper.classicCard(sectors)
    }

    @Test
    fun testNextfareDetection() {
        // Build a card with Nextfare manufacturer bytes
        val card = buildNextfareCard(
            uid = hexToBytes("15cd5b07"),
            systemCode = hexToBytes("010101010101")
        )

        // Verify the factory detects Nextfare manufacturer bytes
        val factory = NextfareTransitInfo.NextfareTransitFactory()
        assertTrue(factory.check(card), "Factory should detect Nextfare card")
    }

    @Test
    fun testNextfareSerialNumber() {
        // 0160 0012 3456 7893
        // This is a fake card number.
        val card = buildNextfareCard(
            uid = hexToBytes("15cd5b07"),
            systemCode = hexToBytes("010101010101")
        )

        val capsule = NextfareTransitInfo.parse(
            card = card,
            timeZone = TimeZone.UTC
        )
        val info = NextfareTransitInfo(capsule)
        assertEquals("0160 0012 3456 7893", info.serialNumber)
    }

    @Test
    fun testNextfareSerialNumber2() {
        // 0160 0098 7654 3213
        // This is a fake card number.
        val card = buildNextfareCard(
            uid = hexToBytes("b168de3a"),
            systemCode = hexToBytes("010101010101")
        )

        val capsule = NextfareTransitInfo.parse(
            card = card,
            timeZone = TimeZone.UTC
        )
        val info = NextfareTransitInfo(capsule)
        assertEquals("0160 0098 7654 3213", info.serialNumber)
    }

    @Test
    fun testNextfareEmptyBalance() {
        // Card with no balance records should have 0 balance
        val card = buildNextfareCard(
            uid = hexToBytes("897df842"),
            systemCode = hexToBytes("010101010101")
        )

        val capsule = NextfareTransitInfo.parse(
            card = card,
            timeZone = TimeZone.UTC
        )
        assertEquals(0, capsule.balance)
    }

    @Test
    fun testSeqGo() {
        // 0160 0012 3456 7893
        // This is a fake card number.
        val c1 = buildNextfareCard(
            uid = hexToBytes("15cd5b07"),
            systemCode = SEQGO_SYSTEM_CODE1
        )
        val seqGoFactory = SeqGoTransitFactory()
        assertTrue(seqGoFactory.check(c1), "Card is seqgo")
        val d1 = seqGoFactory.parseInfo(c1)
        assertTrue(d1 is SeqGoTransitInfo, "Card is SeqGoTransitInfo")
        assertEquals("0160 0012 3456 7893", d1.serialNumber)
        val balances1 = d1.balances
        assertNotNull(balances1)
        assertEquals("AUD", balances1.first().balance.currencyCode)

        // 0160 0098 7654 3213
        // This is a fake card number.
        val c2 = buildNextfareCard(
            uid = hexToBytes("b168de3a"),
            systemCode = SEQGO_SYSTEM_CODE2
        )
        assertTrue(seqGoFactory.check(c2), "Card is seqgo")
        val d2 = seqGoFactory.parseInfo(c2)
        assertTrue(d2 is SeqGoTransitInfo, "Card is SeqGoTransitInfo")
        assertEquals("0160 0098 7654 3213", d2.serialNumber)
        val balances2 = d2.balances
        assertNotNull(balances2)
        assertEquals("AUD", balances2.first().balance.currencyCode)
    }

    @Test
    fun testLaxTap() {
        // 0160 0323 4663 8769
        // This is a fake card number (323.GO.METRO)
        // LAX TAP BLOCK2 is 4 bytes of zeros
        val c = buildNextfareCard(
            uid = hexToBytes("c40dcdc0"),
            systemCode = hexToBytes("010101010101"),
            block2 = LAX_TAP_BLOCK2
        )
        val laxTapFactory = LaxTapTransitFactory()
        assertTrue(laxTapFactory.check(c), "Card is laxtap")
        val d = laxTapFactory.parseInfo(c)
        assertTrue(d is LaxTapTransitInfo, "Card is LaxTapTransitInfo")
        assertEquals("0160 0323 4663 8769", d.serialNumber)
        val balances = d.balances
        assertNotNull(balances)
        assertEquals("USD", balances.first().balance.currencyCode)
    }

    @Test
    fun testMspGoTo() {
        // 0160 0112 3581 3212
        // This is a fake card number
        val c = buildNextfareCard(
            uid = hexToBytes("897df842"),
            systemCode = hexToBytes("010101010101"),
            block2 = MSP_GOTO_BLOCK2
        )
        val mspGotoFactory = MspGotoTransitFactory()
        assertTrue(mspGotoFactory.check(c), "Card is mspgoto")
        val d = mspGotoFactory.parseInfo(c)
        assertTrue(d is MspGotoTransitInfo, "Card is MspGotoTransitInfo")
        assertEquals("0160 0112 3581 3212", d.serialNumber)
        val balances = d.balances
        assertNotNull(balances)
        assertEquals("USD", balances.first().balance.currencyCode)
    }

    @Test
    fun testUnknownCard() {
        // 0160 0112 3581 3212
        // This is a fake card number
        // Card with unrecognized block2 should be detected as generic Nextfare
        val c1 = buildNextfareCard(
            uid = hexToBytes("897df842"),
            systemCode = hexToBytes("010101010101"),
            block2 = hexToBytes("ff00ff00ff00ff00ff00ff00ff00ff00")
        )
        val factory = NextfareTransitInfo.NextfareTransitFactory()
        assertTrue(factory.check(c1), "Card is nextfare")
        // Specific factories should NOT match
        val laxTapFactory = LaxTapTransitFactory()
        val mspGotoFactory = MspGotoTransitFactory()
        assertFalse(laxTapFactory.check(c1), "Card should not be laxtap")
        assertFalse(mspGotoFactory.check(c1), "Card should not be mspgoto")

        val d1 = factory.parseInfo(c1)
        assertEquals("0160 0112 3581 3212", d1.serialNumber)
        val balances1 = d1.balances
        assertNotNull(balances1)
        assertEquals("XXX", balances1.first().balance.currencyCode)

        // Card with unrecognized system code should also be unknown Nextfare
        val c2 = buildNextfareCard(
            uid = hexToBytes("897df842"),
            systemCode = hexToBytes("ff00ff00ff00")
        )
        assertTrue(factory.check(c2), "Card is nextfare")
        val d2 = factory.parseInfo(c2)
        assertEquals("0160 0112 3581 3212", d2.serialNumber)
        val balances2 = d2.balances
        assertNotNull(balances2)
        assertEquals("XXX", balances2.first().balance.currencyCode)
    }

    companion object {
        // SEQ Go system codes from Metrodroid
        private val SEQGO_SYSTEM_CODE1 = hexToBytes("5A5B20212223")
        private val SEQGO_SYSTEM_CODE2 = hexToBytes("202122230101")
        // LAX TAP BLOCK2 is 4 bytes of zeros
        private val LAX_TAP_BLOCK2 = ByteArray(4)
        // MSP Go-To BLOCK2 from Metrodroid
        private val MSP_GOTO_BLOCK2 = hexToBytes("3f332211c0ccddee3f33221101fe01fe")
    }
}
