/*
 * LeapTransitFactory.kt
 *
 * Copyright 2018-2019 Google
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

package com.codebutler.farebot.transit.tfi_leap

import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.desfire.DesfireCard
import com.codebutler.farebot.card.desfire.StandardDesfireFile
import com.codebutler.farebot.card.desfire.UnauthorizedDesfireFile
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.TransitRegion
import farebot.farebot_transit_tfi_leap.generated.resources.*
import kotlin.time.Instant

class LeapTransitFactory : TransitFactory<DesfireCard, TransitInfo> {

    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override fun check(card: DesfireCard): Boolean {
        return card.getApplication(LeapTransitInfo.APP_ID) != null
    }

    override fun parseIdentity(card: DesfireCard): TransitIdentity {
        return try {
            val app = card.getApplication(LeapTransitInfo.APP_ID)!!
            val file2 = (app.getFile(2) as StandardDesfireFile).data
            val file6 = (app.getFile(6) as StandardDesfireFile).data
            TransitIdentity("Leap", LeapTransitInfo.getSerial(file2, file6))
        } catch (e: Exception) {
            TransitIdentity("Locked Leap", null)
        }
    }

    override fun parseInfo(card: DesfireCard): TransitInfo {
        val app = card.getApplication(LeapTransitInfo.APP_ID)!!

        // If file 2 is unauthorized, return locked info
        val file2Raw = app.getFile(2)
        if (file2Raw is UnauthorizedDesfireFile) {
            return LockedLeapTransitInfo()
        }

        val file2 = (file2Raw as StandardDesfireFile).data
        val file4 = (app.getFile(4) as StandardDesfireFile).data
        val file6 = (app.getFile(6) as StandardDesfireFile).data

        val balanceBlock = LeapTransitInfo.chooseBlock(file6, 6)
        // 1 byte unknown
        val initDate = LeapTransitInfo.parseDate(file6, balanceBlock + 1)
        // Expiry is 12 years after init
        val expiryDate = Instant.fromEpochSeconds(initDate.epochSeconds + (12L * 365 * 24 * 60 * 60))
        // 1 byte unknown

        // offset: 0xc

        // offset 0x20
        val trips = mutableListOf<LeapTrip?>()

        trips.add(LeapTrip.parseTopup(file6, 0x20))
        trips.add(LeapTrip.parseTopup(file6, 0x35))
        trips.add(LeapTrip.parseTopup(file6, 0x20 + BLOCK_SIZE))
        trips.add(LeapTrip.parseTopup(file6, 0x35 + BLOCK_SIZE))

        trips.add(LeapTrip.parsePurseTrip(file6, 0x80))
        trips.add(LeapTrip.parsePurseTrip(file6, 0x90))
        trips.add(LeapTrip.parsePurseTrip(file6, 0x80 + BLOCK_SIZE))
        trips.add(LeapTrip.parsePurseTrip(file6, 0x90 + BLOCK_SIZE))

        val file9Raw = app.getFile(9)
        if (file9Raw is StandardDesfireFile) {
            val file9 = file9Raw.data
            for (i in 0..6) {
                trips.add(LeapTrip.parseTrip(file9, 0x80 * i))
            }
        }

        val capBlock = LeapTransitInfo.chooseBlock(file6, 0xa6)

        return LeapTransitInfo(
            serialNumber = LeapTransitInfo.getSerial(file2, file6),
            balanceValue = LeapTransitInfo.parseBalance(file6, balanceBlock + 9),
            trips = LeapTrip.postprocess(trips),
            expiryDate = expiryDate,
            initDate = initDate,
            issueDate = LeapTransitInfo.parseDate(file4, 0x22),
            issuerId = file2.byteArrayToInt(0x22, 3),
            // offset 0x140
            dailyAccumulators = AccumulatorBlock(file6, capBlock + 0x140),
            weeklyAccumulators = AccumulatorBlock(file6, capBlock + 0x160)
        )
    }

    companion object {
        private const val BLOCK_SIZE = 0x180

        private val CARD_INFO = CardInfo(
            nameRes = Res.string.transit_leap_card_name,
            cardType = CardType.MifareDesfire,
            region = TransitRegion.IRELAND,
            locationRes = Res.string.transit_leap_location_ireland,
            imageRes = Res.drawable.leap_card,
            latitude = 53.3498f,
            longitude = -6.2603f,
            brandColor = 0x08B26E,
            credits = listOf("Metrodroid Project", "Vladimir Serbinenko"),
        )
    }
}
