/*
 * BipTransitFactory.kt
 *
 * Copyright 2019 Google
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

package com.codebutler.farebot.transit.bip

import com.codebutler.farebot.base.util.HashUtils
import com.codebutler.farebot.base.util.byteArrayToIntReversed
import com.codebutler.farebot.base.util.byteArrayToLongReversed
import com.codebutler.farebot.base.util.readASCII
import com.codebutler.farebot.base.util.reverseBuffer
import com.codebutler.farebot.base.util.sliceOffLen
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import farebot.farebot_transit_bip.generated.resources.*
import kotlin.experimental.and

private const val NAME = "bip!"

class BipTransitFactory : TransitFactory<ClassicCard, BipTransitInfo> {

    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override fun check(card: ClassicCard): Boolean {
        if (card.sectors.isEmpty()) return false
        val sector0 = card.getSector(0)
        if (sector0 !is DataClassicSector) return false
        return HashUtils.checkKeyHash(
            sector0.keyA, sector0.keyB,
            "chilebip",
            "201d3ae5a9e52edd4e8efbfb1e75b42c",
            "23f0d2cfb56e189553c46af1e2ff3faf"
        ) >= 0
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity {
        val serial = getSerial(card)
        return TransitIdentity.create(NAME, formatSerial(serial))
    }

    override fun parseInfo(card: ClassicCard): BipTransitInfo {
        val balanceBlock = (card.getSector(8) as DataClassicSector).getBlock(1).data
        val balance = balanceBlock.byteArrayToIntReversed(0, 3).let {
            if (balanceBlock[3] and 0x7f != 0.toByte())
                -it
            else
                it
        }

        val nameBlock = (card.getSector(3) as DataClassicSector).getBlock(0).data
        val holderName = if (nameBlock[14] != 0.toByte()) {
            nameBlock.sliceOffLen(1, 14).reverseBuffer().readASCII()
        } else {
            null
        }

        return BipTransitInfo(
            mSerial = getSerial(card),
            mBalance = balance,
            mHolderName = holderName,
            mHolderId = (card.getSector(3) as DataClassicSector).getBlock(1).data
                .byteArrayToIntReversed(3, 4),
            trips = (0..2).mapNotNull { BipTrip.parse((card.getSector(11) as DataClassicSector).getBlock(it).data) } +
                    (0..2).mapNotNull { BipRefill.parse((card.getSector(10) as DataClassicSector).getBlock(it).data) }
        )
    }

    companion object {
        private val CARD_INFO = CardInfo(
            nameRes = Res.string.bip_card_name,
            cardType = CardType.MifareClassic,
            region = TransitRegion.CHILE,
            locationRes = Res.string.bip_location,
            imageRes = Res.drawable.chilebip,
            latitude = -33.4489f,
            longitude = -70.6693f,
            brandColor = 0x214B87,
        )

        private fun getSerial(card: ClassicCard): Long =
            (card.getSector(0) as DataClassicSector).getBlock(1).data
                .byteArrayToLongReversed(4, 4)

        private fun formatSerial(serial: Long): String = serial.toString()
    }
}
