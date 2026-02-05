/*
 * CompassUltralightTransaction.kt
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

package com.codebutler.farebot.transit.yvr_compass

import com.codebutler.farebot.base.mdst.MdstStationLookup
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.nextfareul.NextfareUltralightTransaction
import kotlinx.datetime.TimeZone

class CompassUltralightTransaction(
    raw: ByteArray,
    baseDate: Int
) : NextfareUltralightTransaction(raw, baseDate) {

    override val station: Station?
        get() {
            if (mLocation == 0) return null
            val result = MdstStationLookup.getStation(COMPASS_STR, mLocation)
            return if (result != null) {
                Station(
                    stationNameRaw = result.stationName,
                    shortStationNameRaw = result.shortStationName,
                    companyName = result.companyName,
                    lineNames = result.lineNames,
                    latitude = if (result.hasLocation) result.latitude else null,
                    longitude = if (result.hasLocation) result.longitude else null
                )
            } else {
                Station.unknown(mLocation.toString())
            }
        }

    override val timezone: TimeZone
        get() = CompassUltralightTransitInfo.TZ

    override val isBus: Boolean
        get() = mRoute == 5 || mRoute == 7

    override val mode: Trip.Mode
        get() {
            if (isBus)
                return Trip.Mode.BUS
            if (mRoute == 3 || mRoute == 9 || mRoute == 0xa)
                return Trip.Mode.TRAIN
            return if (mRoute == 0) Trip.Mode.TICKET_MACHINE else Trip.Mode.OTHER
        }

    companion object {
        private const val COMPASS_STR = "compass"
    }
}
