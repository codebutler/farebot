/*
 * WarsawTransitFactory.kt
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

package com.codebutler.farebot.transit.warsaw

import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.byteArrayToIntReversed
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import farebot.farebot_transit_warsaw.generated.resources.*

class WarsawTransitFactory : TransitFactory<ClassicCard, WarsawTransitInfo> {

    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override fun check(card: ClassicCard): Boolean {
        val sector0 = card.getSector(0)
        if (sector0 !is DataClassicSector) return false
        val toc = sector0.getBlock(1).data
        // Check toc entries for sectors 1, 2 and 3
        return (toc.byteArrayToInt(2, 2) == 0x1320
                && toc.byteArrayToInt(4, 2) == 0x1320
                && toc.byteArrayToInt(6, 2) == 0x1320)
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity {
        val serial = getSerial(card)
        val formatted = NumberUtils.zeroPad(serial.first, 3) + " " +
                NumberUtils.zeroPad(serial.second, 8)
        return TransitIdentity.create(getStringBlocking(Res.string.warsaw_card_name), formatted)
    }

    override fun parseInfo(card: ClassicCard): WarsawTransitInfo {
        return WarsawTransitInfo(
            serial = getSerial(card),
            sectorA = WarsawSector.parse(card.getSector(2) as DataClassicSector),
            sectorB = WarsawSector.parse(card.getSector(3) as DataClassicSector)
        )
    }

    companion object {
        private val CARD_INFO = CardInfo(
            nameRes = Res.string.warsaw_card_name,
            cardType = CardType.MifareClassic,
            region = TransitRegion.POLAND,
            locationRes = Res.string.warsaw_location,
            imageRes = Res.drawable.warsaw_card,
            latitude = 52.2297f,
            longitude = 21.0122f,
            brandColor = 0x092979,
            credits = listOf("Metrodroid Project"),
            keysRequired = true,
        )
    }

    private fun getSerial(card: ClassicCard): Pair<Int, Int> {
        val block0 = (card.getSector(0) as DataClassicSector).getBlock(0).data
        return Pair(
            block0[3].toInt() and 0xff,
            block0.byteArrayToIntReversed(0, 3)
        )
    }
}
