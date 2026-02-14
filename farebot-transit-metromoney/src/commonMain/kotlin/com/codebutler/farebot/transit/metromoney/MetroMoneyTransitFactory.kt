/*
 * MetroMoneyTransitFactory.kt
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

package com.codebutler.farebot.transit.metromoney

import com.codebutler.farebot.base.util.HashUtils
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.byteArrayToIntReversed
import com.codebutler.farebot.base.util.byteArrayToLongReversed
import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import farebot.farebot_transit_metromoney.generated.resources.*

class MetroMoneyTransitFactory : TransitFactory<ClassicCard, MetroMoneyTransitInfo> {
    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override fun check(card: ClassicCard): Boolean {
        val sector0 = card.getSector(0) as? DataClassicSector ?: return false
        return HashUtils.checkKeyHash(
            sector0.keyA,
            sector0.keyB,
            "metromoney",
            "c48676dac68ec332570a7c20e12e08cb",
            "5d2457ed5f196e1757b43d074216d0d0",
        ) >= 0
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity =
        TransitIdentity.create(
            getStringBlocking(Res.string.card_name_metromoney),
            NumberUtils.zeroPad(getSerial(card), 10),
        )

    override fun parseInfo(card: ClassicCard): MetroMoneyTransitInfo {
        val sector0 = card.getSector(0) as DataClassicSector
        val sector1 = card.getSector(1) as DataClassicSector
        val sector2 = card.getSector(2) as DataClassicSector

        return MetroMoneyTransitInfo(
            mSerial = getSerial(card),
            mBalance = sector1.getBlock(1).data.byteArrayToIntReversed(0, 4),
            mDate1 = strDate(sector0.getBlock(1).data, 48),
            mDate2 = strDate(sector1.getBlock(2).data, 32),
            mDate3 = strDate(sector1.getBlock(2).data, 96),
            mDate4 = strDate(sector2.getBlock(2).data, 32),
        )
    }

    companion object {
        private val CARD_INFO =
            CardInfo(
                nameRes = Res.string.card_name_metromoney,
                cardType = CardType.MifareClassic,
                region = TransitRegion.GEORGIA,
                locationRes = Res.string.location_tbilisi,
                imageRes = Res.drawable.metromoney,
                latitude = 41.7151f,
                longitude = 44.8271f,
                brandColor = 0xF2C8B6,
                credits = listOf("Metrodroid Project"),
            )

        private fun getSerial(card: ClassicCard): Long {
            val sector0 = card.getSector(0) as DataClassicSector
            return sector0.getBlock(0).data.byteArrayToLongReversed(0, 4)
        }

        private fun strDate(
            raw: ByteArray,
            off: Int,
        ): String {
            val year = raw.getBitsFromBuffer(off, 6) + 2000
            val month = raw.getBitsFromBuffer(off + 6, 4)
            val day = raw.getBitsFromBuffer(off + 10, 5)
            val hour = raw.getBitsFromBuffer(off + 15, 5)
            val min = raw.getBitsFromBuffer(off + 20, 6)
            val sec = raw.getBitsFromBuffer(off + 26, 6)
            return "$year.$month.$day $hour:$min:$sec"
        }
    }
}
