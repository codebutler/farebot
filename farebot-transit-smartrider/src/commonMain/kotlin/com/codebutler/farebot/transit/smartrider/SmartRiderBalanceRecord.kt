/*
 * SmartRiderBalanceRecord.kt
 *
 * Copyright 2016-2022 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.smartrider

import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.byteArrayToIntReversed
import com.codebutler.farebot.base.util.sliceOffLen
import com.codebutler.farebot.card.classic.DataClassicSector

/**
 * Parses a balance record from sectors 2 or 3 of a SmartRider / MyWay card.
 *
 * The sector contains all block data concatenated (blocks 0-2, excluding the trailer block 3).
 * Layout (48 bytes total):
 *   [0..1]   unknown
 *   [2]      bitfield (mode, tap direction, balance sign, etc.)
 *   [3..4]   transaction number (little-endian)
 *   [5..18]  first recent tag-on record (14 bytes)
 *   [19..32] second recent tag-on record (14 bytes)
 *   [33..34] total fare paid (little-endian)
 *   [35..36] default fare (little-endian)
 *   [37..38] remaining chargeable fare (little-endian)
 *   [39..40] balance (little-endian, signed via bitfield)
 *   [41..42] date (days since DATE_EPOCH, little-endian)
 *   [43..44] journey number (little-endian)
 *   [45]     zone bitfield
 */
class SmartRiderBalanceRecord(
    smartRiderType: SmartRiderType,
    sector: DataClassicSector,
    stringResource: StringResource,
) {
    private val b: ByteArray = sector.readBlocks(0, 3)
    val bitfield = SmartRiderTripBitfield(smartRiderType, b[2].toInt())

    val transactionNumber = b.byteArrayToIntReversed(3, 2)

    val firstTagOn = SmartRiderTagRecord.parseRecentTransaction(
        smartRiderType, b.sliceOffLen(5, 14), stringResource
    )
    val recentTagOn = SmartRiderTagRecord.parseRecentTransaction(
        smartRiderType, b.sliceOffLen(19, 14), stringResource
    )

    val totalFarePaid = b.byteArrayToIntReversed(33, 2)
    val defaultFare = b.byteArrayToIntReversed(35, 2)
    val remainingChargableFare = b.byteArrayToIntReversed(37, 2)
    val balance = b.byteArrayToIntReversed(39, 2) * if (bitfield.isBalanceNegative) {
        -1
    } else {
        1
    }
    val journeyNumber = b.byteArrayToIntReversed(43, 2)
    val zoneBitfield = b.byteArrayToInt(45, 1)

    override fun toString(): String {
        return "bitfield=[$bitfield], " +
            "transactionNumber=$transactionNumber, totalFarePaid=$totalFarePaid, " +
            "defaultFare=$defaultFare, remainingChargableFare=$remainingChargableFare, " +
            "balance=$balance, journeyNumber=$journeyNumber\n" +
            "  trip1=[$firstTagOn]\n  trip2=[$recentTagOn]\n"
    }
}
