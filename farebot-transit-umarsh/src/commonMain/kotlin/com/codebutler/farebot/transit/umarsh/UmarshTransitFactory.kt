/*
 * UmarshTransitFactory.kt
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

package com.codebutler.farebot.transit.umarsh

import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import farebot.farebot_transit_umarsh.generated.resources.*

class UmarshTransitFactory : TransitFactory<ClassicCard, UmarshTransitInfo> {

    override val allCards: List<CardInfo>
        get() = ALL_CARDS

    override fun check(card: ClassicCard): Boolean {
        val sector8 = card.getSector(8)
        if (sector8 !is DataClassicSector) return false
        return UmarshSector.check(sector8)
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity {
        val sec = UmarshSector.parse(card.getSector(8) as DataClassicSector, 8)
        return TransitIdentity.create(
            sec.cardName,
            NumberUtils.formatNumber(sec.serialNumber.toLong(), " ", 3, 3, 3)
        )
    }

    override fun parseInfo(card: ClassicCard): UmarshTransitInfo {
        val sec8 = UmarshSector.parse(card.getSector(8) as DataClassicSector, 8)
        val secs = if (!sec8.hasExtraSector)
            listOf(sec8)
        else
            listOf(sec8, UmarshSector.parse(card.getSector(7) as DataClassicSector, 7))

        val validationData = (card.getSector(0) as? DataClassicSector)?.getBlock(1)?.data
        val validation = if (validationData != null) UmarshTrip.parse(validationData, sec8.region) else null

        return UmarshTransitInfo(secs, validation)
    }

    companion object {
        private val ALL_CARDS = listOf(
            CardInfo(
                nameRes = Res.string.card_name_yoshkar_ola_transport_card,
                cardType = CardType.MifareClassic,
                region = TransitRegion.RUSSIA,
                locationRes = Res.string.card_location_yoshkar_ola_russia,
                keysRequired = true,
                preview = true,
                imageRes = Res.drawable.yoshkar_ola,
                latitude = 56.6346f,
                longitude = 47.8998f,
                brandColor = 0x0466B5,
            ),
            CardInfo(
                nameRes = Res.string.card_name_strizh,
                cardType = CardType.MifareClassic,
                region = TransitRegion.RUSSIA,
                locationRes = Res.string.card_location_izhevsk_russia,
                keysRequired = true,
                preview = true,
                imageRes = Res.drawable.strizh,
                latitude = 56.8519f,
                longitude = 53.2114f,
                brandColor = 0xBAD7EC,
            ),
            CardInfo(
                nameRes = Res.string.card_name_electronic_barnaul,
                cardType = CardType.MifareClassic,
                region = TransitRegion.RUSSIA,
                locationRes = Res.string.card_location_barnaul_russia,
                keysRequired = true,
                preview = true,
                imageRes = Res.drawable.barnaul,
                latitude = 53.3548f,
                longitude = 83.7698f,
                brandColor = 0xA7C7E2,
            ),
            CardInfo(
                nameRes = Res.string.card_name_siticard_vladimir,
                cardType = CardType.MifareClassic,
                region = TransitRegion.RUSSIA,
                locationRes = Res.string.card_location_vladimir_russia,
                keysRequired = true,
                preview = true,
                imageRes = Res.drawable.siticard_vladimir,
                latitude = 56.1290f,
                longitude = 40.4066f,
                brandColor = 0x0265FA,
            ),
            CardInfo(
                nameRes = Res.string.card_name_kirov_transport_card,
                cardType = CardType.MifareClassic,
                region = TransitRegion.RUSSIA,
                locationRes = Res.string.card_location_kirov_russia,
                keysRequired = true,
                preview = true,
                imageRes = Res.drawable.kirov,
                latitude = 58.6036f,
                longitude = 49.6680f,
                brandColor = 0xD61B02,
            ),
            CardInfo(
                nameRes = Res.string.card_name_siticard,
                cardType = CardType.MifareClassic,
                region = TransitRegion.RUSSIA,
                locationRes = Res.string.card_location_nizhniy_novgorod_russia,
                keysRequired = true,
                preview = true,
                imageRes = Res.drawable.siticard,
                latitude = 56.2965f,
                longitude = 43.9361f,
                brandColor = 0x106AA9,
            ),
            CardInfo(
                nameRes = Res.string.card_name_omka,
                cardType = CardType.MifareClassic,
                region = TransitRegion.RUSSIA,
                locationRes = Res.string.card_location_omsk_russia,
                keysRequired = true,
                preview = true,
                imageRes = Res.drawable.omka,
                latitude = 54.9885f,
                longitude = 73.3242f,
                brandColor = 0xA9D4A7,
            ),
            CardInfo(
                nameRes = Res.string.card_name_penza_transport_card,
                cardType = CardType.MifareClassic,
                region = TransitRegion.RUSSIA,
                locationRes = Res.string.card_location_penza_russia,
                keysRequired = true,
                preview = true,
                imageRes = Res.drawable.penza,
                latitude = 53.1959f,
                longitude = 45.0184f,
                brandColor = 0x27A956,
            ),
            CardInfo(
                nameRes = Res.string.card_name_ekarta,
                cardType = CardType.MifareClassic,
                region = TransitRegion.RUSSIA,
                locationRes = Res.string.card_location_yekaterinburg_russia,
                keysRequired = true,
                preview = true,
                imageRes = Res.drawable.ekarta,
                latitude = 56.8389f,
                longitude = 60.6057f,
                brandColor = 0x008946,
            ),
        )
    }
}
