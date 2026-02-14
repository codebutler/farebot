/*
 * BonobusTrip.kt
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

package com.codebutler.farebot.transit.bonobus

import com.codebutler.farebot.base.mdst.MdstStationLookup
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.byteArrayToLong
import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.base.util.isAllZero
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import farebot.transit.bonobus.generated.resources.*
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Instant

class BonobusTrip(
    private val mTimestamp: Long,
    private val mFare: Int,
    private val mMode: Int,
    private val mA: Int,
    private val mStation: Int,
    private val mT: Int,
    private val mLine: Int,
    private val mVehicleNumber: Int,
) : Trip() {
    override val fare: TransitCurrency
        get() = TransitCurrency.EUR(if (mMode == MODE_REFILL) -mFare else mFare)

    override val mode: Mode
        get() =
            when (mMode) {
                MODE_BUS -> Mode.BUS
                MODE_REFILL -> Mode.TICKET_MACHINE
                else -> Mode.BUS
            }

    override val startTimestamp: Instant
        get() = parseTimestamp(mTimestamp)

    override val vehicleID: String?
        get() = if (mVehicleNumber == 0) null else NumberUtils.zeroPad(mVehicleNumber, 4)

    override val routeName: String?
        get() = if (mMode == MODE_BUS) (mLine - 10).toString() else null

    override val startStation: Station?
        get() {
            if (mStation == 1 || mStation == 0) return null
            val result = MdstStationLookup.getStation(BONOBUS_STR, mStation)
            return if (result != null) {
                Station(
                    stationNameRaw = result.stationName,
                    shortStationNameRaw = result.shortStationName,
                    companyName = result.companyName,
                    lineNames = result.lineNames,
                    latitude = if (result.hasLocation) result.latitude else null,
                    longitude = if (result.hasLocation) result.longitude else null,
                    humanReadableId = mStation.toString(),
                )
            } else {
                Station.unknown(mStation.toString())
            }
        }

    override val agencyName: String?
        get() = if (mMode == MODE_BUS) getStringBlocking(Res.string.bonobus_agency_tranvia) else null

    companion object {
        fun parse(raw: ByteArray): BonobusTrip? {
            if (raw.isAllZero()) return null
            return BonobusTrip(
                mTimestamp = raw.byteArrayToLong(0, 4),
                mFare = raw.byteArrayToInt(6, 2),
                mMode = raw.getBitsFromBuffer(32, 4),
                mA = raw.getBitsFromBuffer(36, 12),
                mStation = raw.byteArrayToInt(8, 2),
                mT = raw.byteArrayToInt(10, 2),
                mLine = raw.byteArrayToInt(12, 2),
                mVehicleNumber = raw.byteArrayToInt(14, 2),
            )
        }

        fun parseTimestamp(input: Long): Instant {
            val year = (input shr 25).toInt() + 2000
            val month = ((input shr 21).toInt() and 0xf)
            val day = (input shr 16).toInt() and 0x1f
            val hour = (input shr 11).toInt() and 0x1f
            val min = (input shr 5).toInt() and 0x3f
            val sec = (input shl 1).toInt() and 0x3f
            return LocalDateTime(year, month, day, hour, min, sec)
                .toInstant(TZ)
        }

        private val TZ = TimeZone.of("Europe/Madrid")
        private const val MODE_BUS = 8
        private const val MODE_REFILL = 12
        const val BONOBUS_STR = "cadiz"
    }
}
