/*
 * LaxTapTransitFactory.kt
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.lax_tap

import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import com.codebutler.farebot.transit.nextfare.NextfareRefill
import com.codebutler.farebot.transit.nextfare.NextfareTransitInfo
import farebot.farebot_transit_lax_tap.generated.resources.*
import kotlinx.datetime.TimeZone

/**
 * Transit factory for Los Angeles Transit Access Pass (TAP) cards.
 *
 * TAP is a Nextfare-based MiFare Classic card used in the Los Angeles metro area.
 */
class LaxTapTransitFactory : TransitFactory<ClassicCard, LaxTapTransitInfo> {

    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override fun check(card: ClassicCard): Boolean {
        val sector0 = card.getSector(0)
        if (sector0 !is DataClassicSector) return false

        val block1 = sector0.getBlock(1).data
        if (block1.size < 15) return false
        if (!block1.copyOfRange(1, 15).contentEquals(LaxTapData.BLOCK1)) {
            return false
        }

        val block2 = sector0.getBlock(2).data
        if (block2.size < 4) return false
        return block2.copyOfRange(0, 4).contentEquals(LaxTapData.BLOCK2)
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity {
        val sector0 = card.getSector(0) as DataClassicSector
        val serialData = sector0.getBlock(0).data
        val serialNumber = com.codebutler.farebot.transit.nextfare.record.NextfareRecord
            .byteArrayToLongReversed(serialData, 0, 4)
        return TransitIdentity.create(
            LaxTapTransitInfo.NAME,
            NextfareTransitInfo.formatSerialNumber(serialNumber)
        )
    }

    override fun parseInfo(card: ClassicCard): LaxTapTransitInfo {
        val capsule = NextfareTransitInfo.parse(
            card = card,
            timeZone = TIME_ZONE,
            newTrip = { LaxTapTrip(it) },
            newRefill = { NextfareRefill(it) { TransitCurrency.USD(it) } },
            shouldMergeJourneys = false
        )
        return LaxTapTransitInfo(capsule)
    }

    companion object {
        private val CARD_INFO = CardInfo(
            nameRes = Res.string.lax_tap_card_name,
            cardType = CardType.MifareClassic,
            region = TransitRegion.USA,
            locationRes = Res.string.lax_tap_location,
            imageRes = Res.drawable.laxtap_card,
            latitude = 34.0522f,
            longitude = -118.2437f,
            brandColor = 0x497ABD,
            credits = listOf("Metrodroid Project", "Michael Farrell", "Steven Steiner"),
            sampleDumpFile = "LaxTap.json",
        )

        private val TIME_ZONE = TimeZone.of("America/Los_Angeles")
    }
}
