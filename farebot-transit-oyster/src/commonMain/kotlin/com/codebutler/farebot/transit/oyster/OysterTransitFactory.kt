/*
 * OysterTransitFactory.kt
 *
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2025 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.oyster

import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.byteArrayToIntReversed
import com.codebutler.farebot.base.util.isAllFF
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import farebot.farebot_transit_oyster.generated.resources.*

/**
 * Oyster card, London, UK (Transport for London).
 * MIFARE Classic based.
 *
 * This is for old format cards that are **not** labelled with "D".
 *
 * Reference: https://github.com/micolous/metrodroid/wiki/Oyster
 */
class OysterTransitFactory : TransitFactory<ClassicCard, OysterTransitInfo> {

    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override fun check(card: ClassicCard): Boolean {
        val sector0 = card.getSector(0) as? DataClassicSector ?: return false
        if (sector0.blocks.size < 3) return false
        val block1 = sector0.getBlock(1).data
        val block2 = sector0.getBlock(2).data
        if (block1.size < 16 || block2.size < 16) return false

        return block1.contentEquals(MAGIC_BLOCK1) && block2.isAllFF()
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity =
        TransitIdentity.create(NAME, formatSerial(getSerial(card)))

    override fun parseInfo(card: ClassicCard): OysterTransitInfo {
        val purse = OysterPurse.parse(card)
        val transactions = OysterTransaction.parseAll(card)
        val refills = OysterRefill.parseAll(card)
        val passes = OysterTravelPass.parseAll(card)

        return OysterTransitInfo(
            serial = getSerial(card),
            purse = purse,
            transactions = transactions,
            refills = refills,
            passes = passes
        )
    }

    companion object {
        private const val NAME = "Oyster"

        private val CARD_INFO = CardInfo(
            nameRes = Res.string.oyster_card_name,
            cardType = CardType.MifareClassic,
            region = TransitRegion.UK,
            locationRes = Res.string.oyster_location,
            imageRes = Res.drawable.oyster_card,
            latitude = 51.5074f,
            longitude = -0.1278f,
            brandColor = 0x67A8EB,
        )

        // From Metrodroid: ImmutableByteArray.fromHex("964142434445464748494A4B4C4D0101")
        private val MAGIC_BLOCK1 = byteArrayOf(
            0x96.toByte(), 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47,
            0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x01, 0x01
        )

        internal fun formatSerial(serial: Int) = NumberUtils.zeroPad(serial.toLong(), 10)

        private fun getSerial(card: ClassicCard): Int =
            (card.getSector(1) as DataClassicSector).getBlock(0).data.byteArrayToIntReversed(1, 4)
    }
}
