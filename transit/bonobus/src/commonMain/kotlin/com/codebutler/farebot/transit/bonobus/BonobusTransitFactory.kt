/*
 * BonobusTransitFactory.kt
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

package com.codebutler.farebot.transit.bonobus

import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.base.util.HashUtils
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.byteArrayToLongReversed
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import farebot.transit.bonobus.generated.resources.*

class BonobusTransitFactory : TransitFactory<ClassicCard, BonobusTransitInfo> {
    companion object {
        val NAME: FormattedString
            get() = FormattedString(Res.string.card_name_bonobus)

        private val CARD_INFO =
            CardInfo(
                nameRes = Res.string.card_name_bonobus,
                cardType = CardType.MifareClassic,
                region = TransitRegion.SPAIN,
                locationRes = Res.string.location_cadiz,
                imageRes = Res.drawable.cadizcard,
                latitude = 36.5271f,
                longitude = -6.2886f,
                brandColor = 0x1781C7,
                credits = listOf("Metrodroid Project"),
            )
    }

    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override fun check(card: ClassicCard): Boolean {
        val sector0 = card.getSector(0) as? DataClassicSector ?: return false
        // KeyB is readable and so doesn't act as a key
        return HashUtils.checkKeyHash(
            sector0.keyA,
            sector0.keyB,
            "cadiz",
            "cc2f0d405a4968f95100f776161929f6",
        ) >= 0
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity =
        TransitIdentity.create(NAME, getSerial(card).toString())

    override fun parseInfo(card: ClassicCard): BonobusTransitInfo {
        val trips =
            (7..15).flatMap { sec ->
                val sector = card.getSector(sec) as? DataClassicSector ?: return@flatMap emptyList()
                sector.blocks.dropLast(1).mapNotNull { BonobusTrip.parse(it.data) }
            }

        val sector0 = card.getSector(0) as DataClassicSector
        val sector4 = card.getSector(4) as DataClassicSector
        val block02 = sector0.getBlock(2).data

        return BonobusTransitInfo(
            mSerial = getSerial(card),
            trips = trips,
            mBalance =
                sector4
                    .getBlock(0)
                    .data
                    .byteArrayToLongReversed(0, 4)
                    .toInt(),
            mIssueDate = block02.byteArrayToInt(10, 2),
            mExpiryDate = block02.byteArrayToInt(12, 2),
        )
    }

    private fun getSerial(card: ClassicCard): Long {
        val sector0 = card.getSector(0) as DataClassicSector
        return sector0.getBlock(0).data.byteArrayToLongReversed(0, 4)
    }
}
