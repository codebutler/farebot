/*
 * ManlyFastFerryTransitFactory.kt
 *
 * Copyright 2015 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.manlyfastferry

import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import com.codebutler.farebot.transit.erg.ErgTransitInfo
import farebot.transit.manly.generated.resources.*

class ManlyFastFerryTransitFactory : TransitFactory<ClassicCard, ManlyFastFerryTransitInfo> {
    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override fun check(card: ClassicCard): Boolean {
        val sector0 = card.getSector(0)
        if (sector0 !is DataClassicSector) return false
        val file1 = sector0.getBlock(1).data
        if (file1.size < ErgTransitInfo.SIGNATURE.size) return false

        if (!file1
                .copyOfRange(0, ErgTransitInfo.SIGNATURE.size)
                .contentEquals(ErgTransitInfo.SIGNATURE)
        ) {
            return false
        }

        val metadata = ErgTransitInfo.getMetadataRecord(card)
        return metadata != null && metadata.agencyId == ManlyFastFerryTransitInfo.AGENCY_ID
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity {
        val metadata = ErgTransitInfo.getMetadataRecord(card)
        val serial =
            metadata?.cardSerial?.let { s ->
                var result = 0
                for (b in s) {
                    result = (result shl 8) or (b.toInt() and 0xFF)
                }
                result.toString()
            }
        return TransitIdentity.create(getStringBlocking(Res.string.manly_card_name), serial)
    }

    override fun parseInfo(card: ClassicCard): ManlyFastFerryTransitInfo {
        val capsule =
            ErgTransitInfo.parse(
                card,
                newTrip = { purse, epoch -> ManlyFastFerryTrip(purse, epoch) },
                newRefill = { purse, epoch -> ManlyFastFerryRefill(purse, epoch) },
            )
        return ManlyFastFerryTransitInfo(capsule)
    }

    companion object {
        private val CARD_INFO =
            CardInfo(
                nameRes = Res.string.manly_card_name,
                cardType = CardType.MifareClassic,
                region = TransitRegion.AUSTRALIA,
                locationRes = Res.string.manly_location,
                imageRes = Res.drawable.manly_fast_ferry_card,
                latitude = -33.8688f,
                longitude = 151.2093f,
                brandColor = 0x004080,
                credits = listOf("Michael Farrell"),
                keysRequired = true,
            )
    }
}
