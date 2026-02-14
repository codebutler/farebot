/*
 * PodorozhnikTransitFactory.kt
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.podorozhnik

import com.codebutler.farebot.base.util.HashUtils
import com.codebutler.farebot.base.util.Luhn
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.base.util.byteArrayToIntReversed
import com.codebutler.farebot.base.util.byteArrayToLongReversed
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import com.codebutler.farebot.transit.Trip
import farebot.transit.podorozhnik.generated.resources.*

class PodorozhnikTransitFactory(
    private val stringResource: StringResource,
) : TransitFactory<ClassicCard, PodorozhnikTransitInfo> {
    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override fun check(card: ClassicCard): Boolean {
        val sector4 = card.getSector(4) as? DataClassicSector ?: return false
        return HashUtils.checkKeyHash(sector4.keyA, sector4.keyB, KEY_SALT, KEY_DIGEST_A, KEY_DIGEST_B) >= 0
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity {
        val sector0 = card.getSector(0) as? DataClassicSector
        val block0Data = sector0?.getBlock(0)?.data
        val serial = if (block0Data != null) getSerial(block0Data) else null
        return TransitIdentity(
            stringResource.getString(Res.string.podorozhnik_card_name),
            serial,
        )
    }

    override fun parseInfo(card: ClassicCard): PodorozhnikTransitInfo {
        val sector0 = card.getSector(0) as? DataClassicSector
        val block0Data = sector0?.getBlock(0)?.data
        val serialNumber = if (block0Data != null) getSerial(block0Data) else null

        // Decode sector 4 (balance and last topup)
        val sector4Data = decodeSector4(card)

        // Decode sector 5 (last trip, counters)
        val sector5Data = decodeSector5(card)

        val trips = mutableListOf<Trip>()
        if (sector4Data != null && sector4Data.lastTopupTime != 0) {
            trips.add(
                PodorozhnikTopup(
                    mTimestamp = sector4Data.lastTopupTime,
                    mFare = sector4Data.lastTopup,
                    mAgency = sector4Data.lastTopupAgency,
                    mTopupMachine = sector4Data.lastTopupMachine,
                    stringResource = stringResource,
                ),
            )
        }
        if (sector5Data != null && sector5Data.lastTripTime != 0) {
            trips.add(
                PodorozhnikTrip(
                    mTimestamp = sector5Data.lastTripTime,
                    mFare = sector5Data.lastFare,
                    mLastTransport = sector5Data.lastTransport,
                    mLastValidator = sector5Data.lastValidator,
                    stringResource = stringResource,
                ),
            )
        }
        for (timestamp in sector5Data?.extraTripTimes.orEmpty()) {
            trips.add(PodorozhnikDetachedTrip(timestamp))
        }

        return PodorozhnikTransitInfo(
            serialNumber = serialNumber,
            balanceValue = sector4Data?.balance,
            tripList = trips,
            groundCounter = sector5Data?.groundCounter,
            subwayCounter = sector5Data?.subwayCounter,
            stringResource = stringResource,
        )
    }

    private fun decodeSector4(card: ClassicCard): Sector4Data? {
        val sector4 = card.getSector(4) as? DataClassicSector ?: return null

        // Block 0 and block 1 are copies. Let's use block 0
        val block0 = sector4.getBlock(0).data
        val block2 = sector4.getBlock(2).data
        return Sector4Data(
            balance = block0.byteArrayToIntReversed(0, 4),
            lastTopupTime = block2.byteArrayToIntReversed(2, 3),
            lastTopupAgency = block2[5].toInt(),
            lastTopupMachine = block2.byteArrayToIntReversed(6, 2),
            lastTopup = block2.byteArrayToIntReversed(8, 3),
        )
    }

    private fun decodeSector5(card: ClassicCard): Sector5Data? {
        val sector5 = card.getSector(5) as? DataClassicSector ?: return null

        val block0 = sector5.getBlock(0).data
        val block1 = sector5.getBlock(1).data
        val block2 = sector5.getBlock(2).data

        val lastTripTime = block0.byteArrayToIntReversed(0, 3)

        // Usually block1 and block2 are identical. However rarely only one of them
        // gets updated. Pick most recent one for counters but remember both trip
        // timestamps.
        val subwayCounter: Int
        val groundCounter: Int
        if (block2.byteArrayToIntReversed(2, 3) > block1.byteArrayToIntReversed(2, 3)) {
            subwayCounter = block2[0].toInt() and 0xff
            groundCounter = block2[1].toInt() and 0xff
        } else {
            subwayCounter = block1[0].toInt() and 0xff
            groundCounter = block1[1].toInt() and 0xff
        }

        val extraTripTimes =
            listOf(
                block1.byteArrayToIntReversed(2, 3),
                block2.byteArrayToIntReversed(2, 3),
            ).filter { it != lastTripTime }.distinct()

        return Sector5Data(
            lastTripTime = lastTripTime,
            groundCounter = groundCounter,
            subwayCounter = subwayCounter,
            extraTripTimes = extraTripTimes,
            lastTransport = block0[3].toInt() and 0xff,
            lastValidator = block0.byteArrayToIntReversed(4, 2),
            lastFare = block0.byteArrayToIntReversed(6, 4),
        )
    }

    private data class Sector4Data(
        val balance: Int,
        val lastTopup: Int,
        val lastTopupTime: Int,
        val lastTopupMachine: Int,
        val lastTopupAgency: Int,
    )

    private data class Sector5Data(
        val lastFare: Int,
        val extraTripTimes: List<Int>,
        val lastValidator: Int,
        val lastTripTime: Int,
        val groundCounter: Int,
        val subwayCounter: Int,
        val lastTransport: Int,
    )

    companion object {
        private val CARD_INFO =
            CardInfo(
                nameRes = Res.string.podorozhnik_card_name,
                cardType = CardType.MifareClassic,
                region = TransitRegion.RUSSIA,
                locationRes = Res.string.podorozhnik_location,
                imageRes = Res.drawable.podorozhnik_card,
                latitude = 59.9343f,
                longitude = 30.3351f,
                brandColor = 0x7CA22C,
                credits = listOf("Metrodroid Project", "Vladimir Serbinenko", "Michael Farrell"),
            )

        // We don't want to actually include these keys in the program, so include a hashed version of
        // this key.
        private const val KEY_SALT = "podorozhnik"

        // md5sum of Salt + Common Key + Salt, used on sector 4.
        private const val KEY_DIGEST_A = "f3267ff451b1fc3076ba12dcee2bf803"
        private const val KEY_DIGEST_B = "3823b5f0b45f3519d0ce4a8b5b9f1437"

        private fun getSerial(sec0: ByteArray): String {
            var sn =
                "9643 3078 " +
                    NumberUtils.formatNumber(
                        sec0.byteArrayToLongReversed(0, 7),
                        " ",
                        4,
                        4,
                        4,
                        4,
                        1,
                    )
            sn += Luhn.calculateLuhn(sn.replace(" ", "")) // last digit is luhn
            return sn
        }
    }
}
