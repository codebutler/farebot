/*
 * BipTrip.kt
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

package com.codebutler.farebot.transit.bip

import com.codebutler.farebot.base.util.getBitsFromBufferLeBits
import com.codebutler.farebot.base.util.isAllZero
import com.codebutler.farebot.base.util.sliceOffLen
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class BipTrip(
    private val mFare: Int,
    override val startTimestamp: Instant?,
    private val mType: Int,
    private val mA: Int,
    private val mB: Int,
    private val mD: Int,
    private val mE: Int,
    private val mHash: Byte,
) : Trip() {
    override val mode: Mode
        get() =
            when (mType) {
                0x45 -> Mode.METRO
                0x46 -> Mode.BUS
                else -> Mode.OTHER
            }

    override val fare: TransitCurrency
        get() = TransitCurrency.CLP(mFare)

    companion object {
        fun parse(raw: ByteArray): BipTrip? {
            if (raw.sliceOffLen(1, 14).isAllZero()) {
                return null
            }
            return BipTrip(
                mType = raw[8].toInt(),
                startTimestamp = parseTimestamp(raw),
                mA = raw.getBitsFromBufferLeBits(0, 6),
                mB = raw.getBitsFromBufferLeBits(37, 27),
                mD = raw.getBitsFromBufferLeBits(70, 10),
                mE = raw.getBitsFromBufferLeBits(98, 22),
                mHash = raw[15],
                mFare = raw.getBitsFromBufferLeBits(82, 16),
            )
        }
    }
}
