/*
 * ChinaTrip.kt
 *
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

package com.codebutler.farebot.transit.china

import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.byteArrayToLong
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import kotlin.time.Instant
import kotlinx.serialization.Serializable

/**
 * Capsule data class holding the parsed trip data from a China card record.
 */
@Serializable
data class ChinaTripCapsule(
    val mTime: Long,
    val mCost: Int,
    val mType: Int,
    val mStation: Long
) {
    constructor(data: ByteArray) : this(
        // 2 bytes counter
        // 3 bytes zero
        // 4 bytes cost
        mCost = data.byteArrayToInt(5, 4),
        mType = data[9].toInt() and 0xff,
        mStation = data.byteArrayToLong(10, 6),
        mTime = data.byteArrayToLong(16, 7)
    )
}

/**
 * Abstract base class for China transit trips.
 */
abstract class ChinaTripAbstract : Trip() {
    abstract val capsule: ChinaTripCapsule

    val mTime: Long get() = capsule.mTime
    private val mCost: Int get() = capsule.mCost
    val mStation: Long get() = capsule.mStation
    val mType: Int get() = capsule.mType

    override val fare: TransitCurrency?
        get() = TransitCurrency.CNY(if (isTopup) -mCost else mCost)

    protected val isTopup: Boolean
        get() = mType == 2

    protected val transport: Int
        get() = (mStation shr 28).toInt()

    val timestamp: Instant
        get() = ChinaTransitData.parseHexDateTime(mTime)

    val isValid: Boolean
        get() = mCost != 0 || mTime != 0L

    // Should be overridden if anything is known about transports
    override val mode: Mode
        get() = if (isTopup) Mode.TICKET_MACHINE else Mode.OTHER

    // Should be overridden if anything is known about transports
    override val routeName: String?
        get() = humanReadableRouteID

    override val humanReadableRouteID: String?
        get() = mStation.toString(16) + "/" + mType

    override val startTimestamp: Instant?
        get() = timestamp
}

/**
 * Generic China trip implementation for cards without specific station/route knowledge.
 */
@Serializable
class ChinaTrip(override val capsule: ChinaTripCapsule) : ChinaTripAbstract() {
    constructor(data: ByteArray) : this(ChinaTripCapsule(data))
}
