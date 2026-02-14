/*
 * KievTransitFactory.kt
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

package com.codebutler.farebot.transit.kiev

import com.codebutler.farebot.base.util.HashUtils
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.hex
import com.codebutler.farebot.base.util.reverseBuffer
import com.codebutler.farebot.base.util.sliceOffLen
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import farebot.transit.kiev.generated.resources.*

class KievTransitFactory : TransitFactory<ClassicCard, KievTransitInfo> {
    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override fun check(card: ClassicCard): Boolean {
        val sector1 = card.getSector(1) as? DataClassicSector ?: return false
        return HashUtils.checkKeyHash(
            sector1.keyA,
            sector1.keyB,
            "kiev",
            "902a69a9d68afa1ddac7b61a512f7d4f",
        ) >= 0
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity =
        TransitIdentity.create(
            KievTransitInfo.NAME,
            KievTransitInfo.formatSerial(getSerial(card)),
        )

    override fun parseInfo(card: ClassicCard): KievTransitInfo =
        KievTransitInfo(
            mSerial = getSerial(card),
            trips = parseTrips(card),
        )

    companion object {
        private val CARD_INFO =
            CardInfo(
                nameRes = Res.string.kiev_card_name,
                cardType = CardType.MifareClassic,
                region = TransitRegion.UKRAINE,
                locationRes = Res.string.kiev_location,
                imageRes = Res.drawable.kiev,
                latitude = 50.4501f,
                longitude = 30.5234f,
                brandColor = 0x4972AC,
                credits = listOf("Metrodroid Project"),
            )

        private fun parseTrips(card: ClassicCard): List<KievTrip> =
            (0..5).mapNotNull { i ->
                val sector = card.getSector(3 + i / 3) as? DataClassicSector ?: return@mapNotNull null
                val data = sector.getBlock(i % 3).data
                if (data.byteArrayToInt(0, 4) == 0) null else KievTrip(data)
            }

        private fun getSerial(card: ClassicCard): String {
            val sector1 = card.getSector(1) as DataClassicSector
            return sector1
                .getBlock(0)
                .data
                .sliceOffLen(6, 8)
                .reverseBuffer()
                .hex()
        }
    }
}
