/*
 * KazanTransitFactory.kt
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

package com.codebutler.farebot.transit.kazan

import com.codebutler.farebot.base.util.HashUtils
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.byteArrayToIntReversed
import com.codebutler.farebot.base.util.byteArrayToLongReversed
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import farebot.farebot_transit_kazan.generated.resources.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.time.Instant

class KazanTransitFactory : TransitFactory<ClassicCard, KazanTransitInfo> {
    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override fun check(card: ClassicCard): Boolean {
        val sector8 = card.getSector(8) as? DataClassicSector ?: return false
        return HashUtils.checkKeyHash(
            sector8.keyA,
            sector8.keyB,
            "kazan",
            "0f30386921b6558b133f0f49081b932d",
            "ec1b1988a2021019074d4304b4aea772",
        ) >= 0
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity =
        TransitIdentity.create(
            getStringBlocking(Res.string.card_name_kazan),
            NumberUtils.zeroPad(getSerial(card), 10),
        )

    override fun parseInfo(card: ClassicCard): KazanTransitInfo {
        val sector8 = card.getSector(8) as DataClassicSector
        val sector9 = card.getSector(9) as DataClassicSector
        val block80 = sector8.getBlock(0).data
        val block82 = sector8.getBlock(2).data

        return KazanTransitInfo(
            mSerial = getSerial(card),
            mSub =
                KazanSubscription(
                    mType = block80[6].toInt() and 0xff,
                    validFrom = parseDate(block80, 7),
                    validTo = parseDate(block80, 10),
                    mCounter = sector9.getBlock(0).data.byteArrayToIntReversed(0, 4),
                ),
            mTrip = KazanTrip.parse(block82),
        )
    }

    companion object {
        private val CARD_INFO =
            CardInfo(
                nameRes = Res.string.card_name_kazan,
                cardType = CardType.MifareClassic,
                region = TransitRegion.RUSSIA,
                locationRes = Res.string.location_kazan,
                imageRes = Res.drawable.kazan,
                latitude = 55.7963f,
                longitude = 49.1089f,
                brandColor = 0x014797,
                credits = listOf("Metrodroid Project"),
                keysRequired = true,
            )

        private val TZ = TimeZone.of("Europe/Moscow")

        private fun getSerial(card: ClassicCard): Long = card.tagId.byteArrayToLongReversed()

        private fun parseDate(
            raw: ByteArray,
            off: Int,
        ): Instant? {
            if (raw.byteArrayToInt(off, 3) == 0) return null
            val year = (raw[off].toInt() and 0xff) + 2000
            val month = raw[off + 1].toInt() and 0xff
            val day = raw[off + 2].toInt() and 0xff
            return LocalDate(year, month, day).atStartOfDayIn(TZ)
        }
    }
}
