/*
 * KievTrip.kt
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

import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.base.util.getHexString
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import farebot.transit.kiev.generated.resources.Res
import farebot.transit.kiev.generated.resources.kiev_agency_metro
import farebot.transit.kiev.generated.resources.kiev_agency_unknown
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Instant

class KievTrip(
    override val startTimestamp: Instant?,
    private val mTransactionType: String?,
    private val mCounter1: Int,
    private val mCounter2: Int,
    private val mValidator: Int,
) : Trip() {
    override val startStation: Station?
        get() = Station.unknown(mValidator.toString())

    override val fare: TransitCurrency?
        get() = null

    override val mode: Mode
        get() = if (mTransactionType == "84/04/40/53") Mode.METRO else Mode.OTHER

    override val agencyName: String?
        get() =
            when {
                mTransactionType == "84/04/40/53" -> getStringBlocking(Res.string.kiev_agency_metro)
                mTransactionType != null -> getStringBlocking(Res.string.kiev_agency_unknown, mTransactionType)
                else -> null
            }

    constructor(data: ByteArray) : this(
        startTimestamp = parseTimestamp(data),
        // This is a shameless plug. We have no idea which field
        // means what. But metro transport is always 84/04/40/53
        mTransactionType = (
            data.getHexString(0, 1) +
                "/" + data.getHexString(6, 1) +
                "/" + data.getHexString(8, 1) +
                "/" + data.getBitsFromBuffer(88, 10).toString(16)
        ),
        mValidator = data.getBitsFromBuffer(56, 8),
        mCounter1 = data.getBitsFromBuffer(72, 16),
        mCounter2 = data.getBitsFromBuffer(98, 16),
    )

    companion object {
        private val TZ = TimeZone.of("Europe/Kyiv")

        private fun parseTimestamp(data: ByteArray): Instant {
            val year = data.getBitsFromBuffer(17, 5) + 2000
            val month = data.getBitsFromBuffer(13, 4)
            val day = data.getBitsFromBuffer(8, 5)
            val hour = data.getBitsFromBuffer(33, 5)
            val min = data.getBitsFromBuffer(27, 6)
            val sec = data.getBitsFromBuffer(22, 5)
            return LocalDateTime(year, month, day, hour, min, sec)
                .toInstant(TZ)
        }
    }
}
