/*
 * MspGotoTransitFactory.kt
 *
 * Copyright 2018-2019 Google
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

package com.codebutler.farebot.transit.mspgoto

import com.codebutler.farebot.base.util.ByteUtils
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import com.codebutler.farebot.transit.nextfare.NextfareRefill
import com.codebutler.farebot.transit.nextfare.NextfareTransitInfo
import com.codebutler.farebot.transit.nextfare.record.NextfareRecord
import farebot.farebot_transit_msp_goto.generated.resources.*
import kotlinx.datetime.TimeZone

/**
 * Transit factory for MSP Go-To card (Minneapolis, MN).
 * This is a Cubic Nextfare card.
 *
 * Ported from Metrodroid.
 */
class MspGotoTransitFactory : TransitFactory<ClassicCard, MspGotoTransitInfo> {
    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override fun check(card: ClassicCard): Boolean {
        val sector0 = card.getSector(0)
        if (sector0 !is DataClassicSector) return false
        val block1 = sector0.getBlock(1).data
        if (block1.size < 15) return false
        if (!block1.copyOfRange(1, 15).contentEquals(BLOCK1)) return false
        val block2 = sector0.getBlock(2).data
        return block2.contentEquals(BLOCK2)
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity {
        val serialData = (card.getSector(0) as DataClassicSector).getBlock(0).data
        val serialNumber = NextfareRecord.byteArrayToLongReversed(serialData, 0, 4)
        val cardName = getStringBlocking(Res.string.msp_goto_card_name)
        return TransitIdentity.create(cardName, NextfareTransitInfo.formatSerialNumber(serialNumber))
    }

    override fun parseInfo(card: ClassicCard): MspGotoTransitInfo {
        val capsule =
            NextfareTransitInfo.parse(
                card = card,
                timeZone = TIME_ZONE,
                newTrip = { MspGotoTrip(it) },
                newRefill = { NextfareRefill(it) },
                shouldMergeJourneys = false,
            )
        return MspGotoTransitInfo(capsule)
    }

    companion object {
        private val CARD_INFO =
            CardInfo(
                nameRes = Res.string.msp_goto_card_name,
                cardType = CardType.MifareClassic,
                region = TransitRegion.USA,
                locationRes = Res.string.msp_goto_location,
                imageRes = Res.drawable.msp_goto_card,
                latitude = 44.9778f,
                longitude = -93.2650f,
                brandColor = 0x0E519E,
                credits = listOf("Metrodroid Project"),
                sampleDumpFile = "MspGoTo.json",
            )

        private val BLOCK1 =
            byteArrayOf(
                0x16,
                0x18,
                0x1A,
                0x1B,
                0x1C,
                0x1D,
                0x1E,
                0x1F,
                0x01,
                0x01,
                0x01,
                0x01,
                0x01,
                0x01,
            )

        val BLOCK2 =
            ByteUtils.hexStringToByteArray(
                "3f332211c0ccddee3f33221101fe01fe",
            )

        internal val TIME_ZONE = TimeZone.of("America/Chicago")
    }
}
