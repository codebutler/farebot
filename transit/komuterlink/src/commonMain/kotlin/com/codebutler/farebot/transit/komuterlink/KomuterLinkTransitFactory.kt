/*
 * KomuterLinkTransitFactory.kt
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

package com.codebutler.farebot.transit.komuterlink

import com.codebutler.farebot.base.util.ByteUtils
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.byteArrayToIntReversed
import com.codebutler.farebot.base.util.byteArrayToLongReversed
import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import com.codebutler.farebot.transit.Trip
import farebot.transit.komuterlink.generated.resources.*
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Instant
import com.codebutler.farebot.base.util.FormattedString

class KomuterLinkTransitFactory : TransitFactory<ClassicCard, KomuterLinkTransitInfo> {
    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override fun check(card: ClassicCard): Boolean {
        val sector0 = card.getSector(0) as? DataClassicSector ?: return false
        if (sector0.blocks.size < 2) return false
        val block1Data = sector0.getBlock(1).data
        val expected = ByteUtils.hexStringToByteArray("0f0102030405060708090a0b0c0d0e0f")
        return block1Data.contentEquals(expected)
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity {
        val sector0 = card.getSector(0) as DataClassicSector
        val serial = sector0.getBlock(0).data.byteArrayToLongReversed(0, 4)
        return TransitIdentity.create(
            FormattedString(Res.string.komuterlink_card_name),
            NumberUtils.zeroPad(serial, 10),
        )
    }

    override fun parseInfo(card: ClassicCard): KomuterLinkTransitInfo {
        val sector0 = card.getSector(0) as DataClassicSector
        val sector1 = card.getSector(1) as DataClassicSector
        val sector2 = card.getSector(2) as DataClassicSector
        val sector4 = card.getSector(4) as DataClassicSector
        val sector7 = card.getSector(7) as DataClassicSector

        val tz = TimeZone.of("Asia/Kuala_Lumpur")

        fun parseTimestamp(
            data: ByteArray,
            off: Int,
        ): Instant {
            val hour = data.getBitsFromBuffer(off * 8, 5)
            val min = data.getBitsFromBuffer(off * 8 + 5, 6)
            val y = data.getBitsFromBuffer(off * 8 + 17, 6) + 2000
            val month = data.getBitsFromBuffer(off * 8 + 23, 4)
            val d = data.getBitsFromBuffer(off * 8 + 27, 5)
            val ldt = LocalDateTime(y, month, d, hour, min)
            return ldt.toInstant(tz)
        }

        val trips =
            listOfNotNull(
                KomuterLinkTrip.parse(sector4, -1, Trip.Mode.TICKET_MACHINE),
                KomuterLinkTrip.parse(sector7, +1, Trip.Mode.TRAIN),
            )

        return KomuterLinkTransitInfo(
            trips = trips,
            mBalance = sector2.getBlock(0).data.byteArrayToIntReversed(0, 4),
            mSerial = sector0.getBlock(0).data.byteArrayToLongReversed(0, 4),
            mIssueTimestamp = parseTimestamp(sector1.getBlock(0).data, 5),
            mCardNo = sector0.getBlock(2).data.byteArrayToInt(4, 4),
            mStoredLuhn = sector0.getBlock(2).data[8].toInt() and 0xff,
        )
    }

    companion object {
        private val CARD_INFO =
            CardInfo(
                nameRes = Res.string.komuterlink_card_name,
                cardType = CardType.MifareClassic,
                region = TransitRegion.MALAYSIA,
                locationRes = Res.string.komuterlink_location,
                imageRes = Res.drawable.komuterlink,
                latitude = 3.1390f,
                longitude = 101.6869f,
                brandColor = 0x563281,
                credits = listOf("Metrodroid Project"),
            )
    }
}
