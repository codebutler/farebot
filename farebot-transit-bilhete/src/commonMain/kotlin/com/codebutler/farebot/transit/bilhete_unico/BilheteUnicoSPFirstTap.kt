/*
 * BilheteUnicoSPFirstTap.kt
 *
 * Copyright 2018 Google
 * Copyright (C) 2025 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.bilhete_unico

import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import kotlin.time.Instant

internal class BilheteUnicoSPFirstTap(
    private val mDay: Int,
    private val mTime: Int,
    private val mLine: Int,
) : Trip() {

    override val startTimestamp: Instant?
        get() = BilheteUnicoSPTrip.epochDayMinute(mDay, mTime)

    override val fare: TransitCurrency?
        get() = null

    override val mode: Mode
        get() = when (mLine shr 5) {
            1 -> Mode.BUS
            2 -> Mode.TRAM
            else -> Mode.OTHER
        }

    override val routeName: String?
        get() = mLine.toString(16)
}
