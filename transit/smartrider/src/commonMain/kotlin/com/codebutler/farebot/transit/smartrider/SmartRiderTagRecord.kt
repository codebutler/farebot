/*
 * SmartRiderTagRecord.kt
 *
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
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

import com.codebutler.farebot.base.mdst.MdstStationLookup
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.byteArrayToIntReversed
import com.codebutler.farebot.base.util.byteArrayToLongReversed
import com.codebutler.farebot.base.util.hex
import com.codebutler.farebot.base.util.isASCII
import com.codebutler.farebot.base.util.readASCII
import com.codebutler.farebot.base.util.sliceOffLen
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.Transaction
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import farebot.transit.smartrider.generated.resources.*
import kotlin.time.Instant

/**
 * Represents a single "tag on" / "tag off" event on a SmartRider or MyWay card.
 */
class SmartRiderTagRecord(
    internal val mTimestamp: Long,
    override val isTapOn: Boolean,
    private val mRoute: String,
    val cost: Int,
    override val mode: Trip.Mode,
    override val isTransfer: Boolean,
    private val mSmartRiderType: SmartRiderType,
    private val mStopId: Int = 0,
    private val mZone: Int = 0,
    private val stringResource: StringResource,
) : Transaction() {
    val isValid: Boolean
        get() = mTimestamp != 0L

    override val timestamp: Instant?
        get() = convertTime(mTimestamp, mSmartRiderType)

    override val isTapOff: Boolean
        get() = !isTapOn

    override val fare: TransitCurrency?
        get() = TransitCurrency.AUD(cost)

    override val routeNames: List<String>
        get() = listOf(mRoute)

    override val station: Station?
        get() =
            when {
                mStopId == 0 -> null
                mSmartRiderType == SmartRiderType.SMARTRIDER && mode == Trip.Mode.TRAIN ->
                    lookupMdstStation(SMARTRIDER_STR, mStopId)
                // TODO: Handle other modes of transit. Stops there are a combination of the
                // route + Stop (ie: route A stop 3 != route B stop 3)
                else -> Station.unknown(mStopId.toString())
            }

    override fun shouldBeMerged(other: Transaction): Boolean =
        // Are the two trips within 90 minutes of each other (sanity check)
        (
            other is SmartRiderTagRecord &&
                other.mTimestamp - mTimestamp <= 5400 &&
                super.shouldBeMerged(other)
        )

    override val agencyName: String?
        get() =
            when (mSmartRiderType) {
                SmartRiderType.MYWAY -> stringResource.getString(Res.string.agency_name_action)
                SmartRiderType.SMARTRIDER -> stringResource.getString(Res.string.agency_name_transperth)
                else -> stringResource.getString(Res.string.unknown)
            }

    override fun isSameTrip(other: Transaction): Boolean =
        // SmartRider only ever records route names.
        other is SmartRiderTagRecord && mRoute == other.mRoute && isTapOn != other.isTapOn

    /**
     * Enriches a [SmartRiderTagRecord] created by [parse] with data from another
     * [SmartRiderTagRecord] created by [parseRecentTransaction].
     */
    fun enrichWithRecentData(other: SmartRiderTagRecord): SmartRiderTagRecord {
        require(other.mTimestamp == mTimestamp) { "trip timestamps must be equal" }
        return SmartRiderTagRecord(
            mTimestamp = mTimestamp,
            isTapOn = isTapOn,
            mSmartRiderType = mSmartRiderType,
            cost = cost,
            mRoute = mRoute,
            mode = mode,
            isTransfer = isTransfer,
            mZone = other.mZone,
            mStopId = other.mStopId,
            stringResource = stringResource,
        )
    }

    private fun lookupMdstStation(
        dbName: String,
        stationId: Int,
    ): Station? {
        val result = MdstStationLookup.getStation(dbName, stationId) ?: return null
        return Station
            .Builder()
            .stationName(result.stationName)
            .shortStationName(result.shortStationName)
            .companyName(result.companyName)
            .lineNames(result.lineNames)
            .latitude(if (result.hasLocation) result.latitude.toString() else null)
            .longitude(if (result.hasLocation) result.longitude.toString() else null)
            .build()
    }

    companion object {
        private fun routeName(input: ByteArray): String {
            val cleaned = input.filter { it != 0.toByte() }.toByteArray()
            try {
                if (cleaned.isASCII()) {
                    return cleaned.readASCII()
                }
            } catch (_: Exception) {
            }
            return cleaned.hex()
        }

        /**
         * Parse a transaction in a single block of sectors 10 - 13.
         */
        fun parse(
            smartRiderType: SmartRiderType,
            record: ByteArray,
            stringResource: StringResource,
        ): SmartRiderTagRecord {
            val mTimestamp = record.byteArrayToLongReversed(3, 4)
            val bitfield = SmartRiderTripBitfield(smartRiderType, record[7].toInt())
            val route = routeName(record.sliceOffLen(8, 4))
            val cost = record.byteArrayToIntReversed(13, 2)

            return SmartRiderTagRecord(
                mTimestamp = mTimestamp,
                isTapOn = bitfield.isTapOn,
                mSmartRiderType = smartRiderType,
                cost = cost,
                mRoute = route,
                mode = bitfield.mode,
                isTransfer = bitfield.isTransfer,
                stringResource = stringResource,
            )
        }

        /**
         * Parses a recent transaction inside block 2 - 3, bytes 5-18 and 19-32 inclusive.
         */
        fun parseRecentTransaction(
            smartRiderType: SmartRiderType,
            record: ByteArray,
            stringResource: StringResource,
        ): SmartRiderTagRecord {
            require(record.size == 14) { "Recent transactions must be 14 bytes" }
            val timestamp = record.byteArrayToLongReversed(0, 4)
            // This is sometimes the vehicle number, sometimes the route name
            val route = routeName(record.sliceOffLen(4, 4))
            // 8 .. 9 unknown bitfield
            // StopID may actually be binary-coded decimal
            val stopId = record.byteArrayToInt(10, 2)
            val zone = record[12].toInt()
            // 13 unknown

            return SmartRiderTagRecord(
                mTimestamp = timestamp,
                isTapOn = false,
                mSmartRiderType = smartRiderType,
                cost = 0,
                mRoute = route,
                mStopId = stopId,
                mZone = zone,
                mode = Trip.Mode.OTHER,
                isTransfer = false,
                stringResource = stringResource,
            )
        }
    }
}
