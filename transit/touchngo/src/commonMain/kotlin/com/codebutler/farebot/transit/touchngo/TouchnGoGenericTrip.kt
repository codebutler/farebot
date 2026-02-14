/*
 * TouchnGoGenericTrip.kt
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

import com.codebutler.farebot.base.mdst.MdstStationLookup
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.hex
import com.codebutler.farebot.base.util.isASCII
import com.codebutler.farebot.base.util.readASCII
import com.codebutler.farebot.base.util.sliceOffLen
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import kotlin.time.Instant

/**
 * Represents a generic Touch 'n Go trip such as a toll road transaction or POS purchase.
 */
internal class TouchnGoGenericTrip(
    private val header: ByteArray,
    override val routeName: String?,
    override val mode: Mode,
) : Trip() {
    private val agencyRaw: ByteArray
        get() = header.sliceOffLen(2, 4)

    private val amount: Int
        get() = header.byteArrayToInt(10, 2)

    override val startTimestamp: Instant
        get() = parseTimestamp(header, 12)

    override val fare: TransitCurrency
        get() = TransitCurrency.MYR(amount)

    override val agencyName: String?
        get() {
            val operatorId = agencyRaw.byteArrayToInt()
            val mdstName = MdstStationLookup.getOperatorName(TNG_STR, operatorId)
            return mdstName ?: if (agencyRaw.isASCII()) agencyRaw.readASCII() else agencyRaw.hex()
        }

    companion object {
        fun parse(
            sector: DataClassicSector,
            mode: Mode,
            routeName: String? = null,
        ): TouchnGoGenericTrip? {
            if (sector.getBlock(0).isEmpty && sector.getBlock(1).isEmpty && sector.getBlock(2).isEmpty) {
                return null
            }
            return TouchnGoGenericTrip(
                header = sector.getBlock(0).data,
                mode = mode,
                routeName = routeName,
            )
        }
    }
}
