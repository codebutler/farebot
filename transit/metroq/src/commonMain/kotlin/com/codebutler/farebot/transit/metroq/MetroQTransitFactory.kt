/*
 * MetroQTransitFactory.kt
 *
 * Copyright 2018 Google
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

package com.codebutler.farebot.transit.metroq

import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.byteArrayToLong
import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import farebot.transit.metroq.generated.resources.*
import kotlinx.datetime.LocalDate

class MetroQTransitFactory : TransitFactory<ClassicCard, MetroQTransitInfo> {
    companion object {
        private const val METRO_Q_ID = 0x5420

        private val CARD_INFO =
            CardInfo(
                nameRes = Res.string.metroq_card_name,
                cardType = CardType.MifareClassic,
                region = TransitRegion.QATAR,
                locationRes = Res.string.metroq_location,
                imageRes = Res.drawable.metroq,
                latitude = 25.2854f,
                longitude = 51.5310f,
                brandColor = 0xFC4337,
                credits = listOf("Metrodroid Project"),
            )
    }

    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override fun check(card: ClassicCard): Boolean {
        val sector = card.getSector(0)
        if (sector !is DataClassicSector) return false

        for (i in 1..2) {
            val block = sector.getBlock(i).data
            for (j in (if (i == 1) 1 else 0)..7) {
                if (block.byteArrayToInt(j * 2, 2) != METRO_Q_ID && (i != 2 || j != 6)) {
                    return false
                }
            }
        }
        return true
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity {
        val serial = getSerial(card)
        val cardName = FormattedString(Res.string.metroq_card_name)
        return TransitIdentity.create(
            cardName,
            NumberUtils.zeroPad(serial, 8),
        )
    }

    override fun parseInfo(card: ClassicCard): MetroQTransitInfo {
        val balanceSector = card.getSector(8) as DataClassicSector
        val balanceBlock0 = balanceSector.getBlock(0)
        val balanceBlock1 = balanceSector.getBlock(1)
        val balanceBlock =
            if (balanceBlock0.data.getBitsFromBuffer(93, 8) >
                balanceBlock1.data.getBitsFromBuffer(93, 8)
            ) {
                balanceBlock0
            } else {
                balanceBlock1
            }

        val sector1Block0 = (card.getSector(1) as DataClassicSector).getBlock(0).data

        return MetroQTransitInfo(
            serial = getSerial(card),
            balanceValue = balanceBlock.data.getBitsFromBuffer(77, 16),
            product = balanceBlock.data.getBitsFromBuffer(8, 12),
            expiryDate = parseTimestamp(sector1Block0, 0),
            date1 = parseTimestamp(sector1Block0, 24),
        )
    }

    private fun parseTimestamp(
        data: ByteArray,
        off: Int,
    ): LocalDate {
        val year = data.getBitsFromBuffer(off, 8) + 2000
        val month = data.getBitsFromBuffer(off + 8, 4)
        val day = data.getBitsFromBuffer(off + 12, 5)
        return LocalDate(year, month, day)
    }

    private fun getSerial(card: ClassicCard): Long =
        (card.getSector(1) as DataClassicSector).getBlock(2).data.byteArrayToLong(0, 4)
}
