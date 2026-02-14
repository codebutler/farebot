/*
 * YarGorTransitFactory.kt
 *
 * Copyright 2019 Google
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

package com.codebutler.farebot.transit.yargor

import com.codebutler.farebot.base.util.HashUtils
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import farebot.farebot_transit_yargor.generated.resources.*

class YarGorTransitFactory : TransitFactory<ClassicCard, YarGorTransitInfo> {

    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override fun check(card: ClassicCard): Boolean {
        val sector10 = card.getSector(10) as? DataClassicSector ?: return false
        return HashUtils.checkKeyHash(
            sector10.keyA, sector10.keyB,
            "yaroslavl",
            "0deaf06098f0f7ab47a7ea22945ee81a",
            "6775e7c1a73e0e9c98167a7665ef4bc1"
        ) >= 0
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity =
        TransitIdentity(
            getStringBlocking(Res.string.yargor_card_name),
            YarGorTransitInfo.formatSerial(YarGorTransitInfo.getSerial(card))
        )

    override fun parseInfo(card: ClassicCard) = YarGorTransitInfo.parse(card)

    companion object {
        private val CARD_INFO = CardInfo(
            nameRes = Res.string.yargor_card_name,
            cardType = CardType.MifareClassic,
            region = TransitRegion.RUSSIA,
            locationRes = Res.string.yargor_location,
            imageRes = Res.drawable.yargor,
            latitude = 57.6261f,
            longitude = 39.8845f,
            brandColor = 0x6676A6,
            credits = listOf("Metrodroid Project"),
        )
    }
}
