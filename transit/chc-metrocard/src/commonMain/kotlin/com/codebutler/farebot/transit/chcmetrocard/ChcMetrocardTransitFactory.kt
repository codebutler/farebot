/*
 * ChcMetrocardTransitFactory.kt
 *
 * Copyright 2018-2019 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.chcmetrocard

import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import com.codebutler.farebot.transit.erg.ErgTransitInfo
import farebot.transit.chc_metrocard.generated.resources.*

/**
 * Factory for detecting and parsing CHC Metrocard (Christchurch, NZ) transit cards.
 *
 * This is an ERG-based card identified by the ERG signature and agency ID 0x0136.
 */
class ChcMetrocardTransitFactory : TransitFactory<ClassicCard, ChcMetrocardTransitInfo> {
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
        return metadata != null && metadata.agencyId == ChcMetrocardTransitInfo.AGENCY_ID
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
        return TransitIdentity.create(FormattedString(Res.string.chc_metrocard_card_name), serial)
    }

    override fun parseInfo(card: ClassicCard): ChcMetrocardTransitInfo {
        val capsule =
            ErgTransitInfo.parse(
                card,
                newTrip = { purse, epoch -> ChcMetrocardTrip(purse, epoch) },
                newRefill = { purse, epoch -> ChcMetrocardRefill(purse, epoch) },
            )
        return ChcMetrocardTransitInfo(capsule)
    }

    companion object {
        private val CARD_INFO =
            CardInfo(
                nameRes = Res.string.chc_metrocard_card_name,
                cardType = CardType.MifareClassic,
                region = TransitRegion.NEW_ZEALAND,
                locationRes = Res.string.chc_metrocard_location,
                imageRes = Res.drawable.chc_metrocard,
                latitude = -43.5321f,
                longitude = 172.6362f,
                brandColor = 0x242245,
                credits = listOf("Metrodroid Project", "Michael Farrell"),
                keysRequired = true,
                extraNoteRes = Res.string.chc_metrocard_note,
            )
    }
}
