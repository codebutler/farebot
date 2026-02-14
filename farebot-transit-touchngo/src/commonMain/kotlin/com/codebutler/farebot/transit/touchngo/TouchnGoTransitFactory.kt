/*
 * TouchnGoTransitFactory.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
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

package com.codebutler.farebot.transit.touchngo

import com.codebutler.farebot.base.util.ByteUtils
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
import com.codebutler.farebot.transit.Trip
import farebot.farebot_transit_touchngo.generated.resources.*

class TouchnGoTransitFactory : TransitFactory<ClassicCard, TouchnGoTransitInfo> {
    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override fun check(card: ClassicCard): Boolean {
        val sector0 = card.getSector(0) as? DataClassicSector ?: return false
        val block1Data = sector0.getBlock(1).data
        return block1Data.contentEquals(EXPECTED_BLOCK1)
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity {
        val sector0 =
            card.getSector(0) as? DataClassicSector
                ?: throw RuntimeException("Error parsing Touch 'n Go identity")
        val serial = sector0.getBlock(0).data.byteArrayToLongReversed(0, 4)
        return TransitIdentity.create(NAME, NumberUtils.zeroPad(serial, 10))
    }

    override fun parseInfo(card: ClassicCard): TouchnGoTransitInfo {
        val sector0 = card.getSector(0) as DataClassicSector
        val sector1 = card.getSector(1) as DataClassicSector
        val sector2 = card.getSector(2) as DataClassicSector
        val sector3 = card.getSector(3) as DataClassicSector
        val sector5 = card.getSector(5) as DataClassicSector
        val sector6 = card.getSector(6) as DataClassicSector
        val sector7 = card.getSector(7) as DataClassicSector
        val sector8 = card.getSector(8) as DataClassicSector

        val balance = sector2.getBlock(0).data.byteArrayToIntReversed(0, 4)
        val txnCounter = 0xf9ff - sector3.getBlock(0).data.byteArrayToIntReversed(0, 4)

        val block10 = sector1.getBlock(0).data
        val isTravelPass =
            block10.byteArrayToInt(0, 2) == 0x3233 &&
                block10.byteArrayToInt(4, 2) == 0x5230 &&
                block10.byteArrayToInt(10, 2) == 0x4602

        val serial = sector0.getBlock(0).data.byteArrayToLongReversed(0, 4)

        val trips = mutableListOf<Trip>()
        parseTollTrip(sector5)?.let { trips.add(it) }
        parseTransitTrip(sector6)?.let { trips.add(it) }
        parseInProgressTrip(sector6)?.let { trips.add(it) }
        parseRefill(sector7)?.let { trips.add(it) }
        parsePosTrip(sector8)?.let { trips.add(it) }

        val cardNo = sector0.getBlock(2).data.byteArrayToInt(7, 4)
        val storedLuhn = sector0.getBlock(2).data[11].toInt() and 0xff
        val issueCounter = block10.byteArrayToInt(12, 2)
        val issueDate = parseDaystamp(block10, 14)
        val expiryDate = parseDaystamp(sector0.getBlock(2).data, 14)

        return TouchnGoTransitInfo(
            balanceValue = balance,
            serial = serial,
            txnCounter = txnCounter,
            isTravelPass = isTravelPass,
            trips = trips,
            cardNo = cardNo,
            storedLuhn = storedLuhn,
            issueCounter = issueCounter,
            issueDate = issueDate,
            expiryDate = expiryDate,
        )
    }

    companion object {
        private val CARD_INFO =
            CardInfo(
                nameRes = Res.string.touchngo_card_name,
                cardType = CardType.MifareClassic,
                region = TransitRegion.MALAYSIA,
                locationRes = Res.string.touchngo_location,
                imageRes = Res.drawable.touchngo,
                latitude = 3.1390f,
                longitude = 101.6869f,
                brandColor = 0x292C6C,
                credits = listOf("Metrodroid Project"),
            )

        internal val NAME: String
            get() = getStringBlocking(Res.string.touchngo_card_name)

        private val EXPECTED_BLOCK1 = ByteUtils.hexStringToByteArray("000102030405060708090a0b0c0d0e0f")

        private fun parseTollTrip(sector: DataClassicSector): TouchnGoGenericTrip? =
            TouchnGoGenericTrip.parse(sector, Trip.Mode.TOLL_ROAD)

        private fun parsePosTrip(sector: DataClassicSector): TouchnGoGenericTrip? =
            TouchnGoGenericTrip.parse(sector, Trip.Mode.POS)

        private fun parseTransitTrip(sector: DataClassicSector): TouchnGoTrip? = TouchnGoTrip.parse(sector)

        private fun parseInProgressTrip(sector: DataClassicSector): TouchnGoInProgressTrip? =
            TouchnGoInProgressTrip.parse(sector)

        private fun parseRefill(sector: DataClassicSector): TouchnGoRefill? = TouchnGoRefill.parse(sector)
    }
}
