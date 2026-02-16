/*
 * CharlieCardTransitFactory.kt
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

package com.codebutler.farebot.transit.charlie

import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.base.util.HashUtils
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
import farebot.transit.charlie.generated.resources.*

/**
 * CharlieCard, Boston, USA (MBTA).
 * Card detection requires MBTA-specific keys.
 */
class CharlieCardTransitFactory : TransitFactory<ClassicCard, CharlieCardTransitInfo> {
    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override fun check(card: ClassicCard): Boolean {
        val sector0 = card.getSector(0) as? DataClassicSector ?: return false
        return HashUtils.checkKeyHash(
            sector0.keyA,
            sector0.keyB,
            "charlie",
            "63ee95c7340fceb524cae7aab66fb1f9",
            "2114a2414d6b378e36a4e9540d1adc9f",
        ) >= 0
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity =
        TransitIdentity.create(NAME, formatSerial(getSerial(card)))

    override fun parseInfo(card: ClassicCard): CharlieCardTransitInfo {
        val sector2 = card.getSector(2) as DataClassicSector
        val sector3 = card.getSector(3) as DataClassicSector
        val balanceSector: DataClassicSector =
            if (sector2.getBlock(0).data.getBitsFromBuffer(81, 16)
                > sector3.getBlock(0).data.getBitsFromBuffer(81, 16)
            ) {
                sector2
            } else {
                sector3
            }

        val trips = mutableListOf<CharlieCardTrip>()
        for (i in 0..11) {
            val sector = card.getSector(6 + i / 6) as? DataClassicSector ?: continue
            val block = sector.getBlock(i / 2 % 3)
            if (block.data.byteArrayToInt(7 * (i % 2), 4) == 0) {
                continue
            }
            trips.add(CharlieCardTrip.parse(block.data, 7 * (i % 2)))
        }

        return CharlieCardTransitInfo(
            serial = getSerial(card),
            secondSerial =
                (card.getSector(8) as? DataClassicSector)
                    ?.getBlock(0)
                    ?.data
                    ?.byteArrayToLong(0, 4) ?: 0L,
            mBalance = getPrice(balanceSector.getBlock(1).data, 5),
            startDate = balanceSector.getBlock(0).data.byteArrayToInt(6, 3),
            trips = trips,
        )
    }

    companion object {
        private val CARD_INFO =
            CardInfo(
                nameRes = Res.string.charlie_card_name,
                cardType = CardType.MifareClassic,
                region = TransitRegion.USA,
                locationRes = Res.string.charlie_location_boston,
                imageRes = Res.drawable.charlie_card,
                latitude = 42.3601f,
                longitude = -71.0589f,
                brandColor = 0x47A64A,
                credits = listOf("Metrodroid Project", "Vladimir Serbinenko"),
            )

        internal val NAME: FormattedString
            get() = FormattedString(Res.string.charlie_card_name)

        internal fun getPrice(
            data: ByteArray,
            off: Int,
        ): Int {
            var value = data.byteArrayToInt(off, 2)
            if (value and 0x8000 != 0) {
                value = -(value and 0x7fff)
            }
            return value / 2
        }

        internal fun formatSerial(serial: Long) = "5-$serial"

        private fun getSerial(card: ClassicCard): Long =
            (card.getSector(0) as DataClassicSector).getBlock(0).data.byteArrayToLong(0, 4)
    }
}
