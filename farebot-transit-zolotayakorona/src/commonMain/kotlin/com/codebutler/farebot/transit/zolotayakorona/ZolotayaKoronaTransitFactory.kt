/*
 * ZolotayaKoronaTransitFactory.kt
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

package com.codebutler.farebot.transit.zolotayakorona

import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.byteArrayToIntReversed
import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.base.util.getHexString
import com.codebutler.farebot.base.util.sliceOffLen
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.card.classic.UnauthorizedClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import farebot.farebot_transit_zolotayakorona.generated.resources.*

class ZolotayaKoronaTransitFactory : TransitFactory<ClassicCard, ZolotayaKoronaTransitInfo> {
    override val allCards: List<CardInfo>
        get() = ALL_CARDS

    override fun check(card: ClassicCard): Boolean {
        val sector0 = card.getSector(0)
        if (sector0 !is DataClassicSector) return false
        val toc = sector0.getBlock(1).data
        // Check toc entries for sectors 10,12,13,14 and 15
        return toc.byteArrayToInt(8, 2) == 0x18ee &&
            toc.byteArrayToInt(12, 2) == 0x18ee
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity {
        val cardType = getCardType(card)
        val serial = getSerial(card)
        return TransitIdentity.create(
            ZolotayaKoronaTransitInfo.nameCard(cardType),
            ZolotayaKoronaTransitInfo.formatSerial(serial),
        )
    }

    override fun parseInfo(card: ClassicCard): ZolotayaKoronaTransitInfo {
        val cardType = getCardType(card)

        val balance =
            if (card.getSector(6) is UnauthorizedClassicSector) {
                null
            } else {
                (card.getSector(6) as DataClassicSector).getBlock(0).data.byteArrayToIntReversed(0, 4)
            }

        val sector4 = card.getSector(4) as DataClassicSector
        val infoBlock = sector4.getBlock(0).data
        val refill = ZolotayaKoronaRefill.parse(sector4.getBlock(1).data, cardType)
        val trip = ZolotayaKoronaTrip.parse(sector4.getBlock(2).data, cardType, refill, balance)

        val sector0 = card.getSector(0) as DataClassicSector

        return ZolotayaKoronaTransitInfo(
            serial = getSerial(card),
            cardSerial = sector0.getBlock(0).data.getHexString(0, 4),
            cardType = cardType,
            balanceValue = balance,
            trip = trip,
            refill = refill,
            status = infoBlock.getBitsFromBuffer(60, 4),
            sequenceCtr = infoBlock.byteArrayToInt(8, 2),
            discountCode = infoBlock[10].toInt() and 0xff,
            tail = infoBlock.sliceOffLen(11, 5),
        )
    }

    private fun getSerial(card: ClassicCard): String {
        val sector15 = card.getSector(15) as DataClassicSector
        return sector15
            .getBlock(2)
            .data
            .getHexString(4, 10)
            .substring(0, 19)
    }

    private fun getCardType(card: ClassicCard): Int {
        val sector15 = card.getSector(15) as DataClassicSector
        return sector15.getBlock(1).data.byteArrayToInt(10, 3)
    }

    companion object {
        private val ALL_CARDS =
            listOf(
                CardInfo(
                    nameRes = Res.string.card_name_zolotaya_korona,
                    cardType = CardType.MifareClassic,
                    region = TransitRegion.RUSSIA,
                    locationRes = Res.string.card_location_russia,
                    keysRequired = true,
                    preview = true,
                    imageRes = Res.drawable.zolotayakorona,
                    latitude = 55.0084f,
                    longitude = 82.9357f,
                    brandColor = 0xE0002D,
                    credits = listOf("Metrodroid Project"),
                ),
                CardInfo(
                    nameRes = Res.string.card_name_krasnodar_etk,
                    cardType = CardType.MifareClassic,
                    region = TransitRegion.RUSSIA,
                    locationRes = Res.string.card_location_krasnodar_russia,
                    keysRequired = true,
                    preview = true,
                    imageRes = Res.drawable.krasnodar_etk,
                    latitude = 45.0355f,
                    longitude = 38.9753f,
                    brandColor = 0x4B75B8,
                    credits = listOf("Metrodroid Project"),
                ),
                CardInfo(
                    nameRes = Res.string.card_name_orenburg_ekg,
                    cardType = CardType.MifareClassic,
                    region = TransitRegion.RUSSIA,
                    locationRes = Res.string.card_location_orenburg_russia,
                    keysRequired = true,
                    preview = true,
                    imageRes = Res.drawable.orenburg_ekg,
                    latitude = 51.7727f,
                    longitude = 55.0988f,
                    brandColor = 0xFED653,
                    credits = listOf("Metrodroid Project"),
                ),
                CardInfo(
                    nameRes = Res.string.card_name_samara_etk,
                    cardType = CardType.MifareClassic,
                    region = TransitRegion.RUSSIA,
                    locationRes = Res.string.card_location_samara_russia,
                    keysRequired = true,
                    preview = true,
                    imageRes = Res.drawable.samara_etk,
                    latitude = 53.1959f,
                    longitude = 50.1001f,
                    brandColor = 0xE6213A,
                    credits = listOf("Metrodroid Project"),
                ),
                CardInfo(
                    nameRes = Res.string.card_name_yaroslavl_etk,
                    cardType = CardType.MifareClassic,
                    region = TransitRegion.RUSSIA,
                    locationRes = Res.string.card_location_yaroslavl_russia,
                    keysRequired = true,
                    preview = true,
                    imageRes = Res.drawable.yaroslavl_etk,
                    latitude = 57.6261f,
                    longitude = 39.8845f,
                    brandColor = 0x1C3778,
                    credits = listOf("Metrodroid Project"),
                ),
            )
    }
}
