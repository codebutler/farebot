/*
 * YarGorTrip.kt
 *
 * Copyright 2019 Google
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

package com.codebutler.farebot.transit.yargor

import com.codebutler.farebot.base.mdst.MdstStationTableReader
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.byteArrayToIntReversed
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import farebot.transit.yargor.generated.resources.Res
import farebot.transit.yargor.generated.resources.yargor_mode_bus
import farebot.transit.yargor.generated.resources.yargor_mode_tram
import farebot.transit.yargor.generated.resources.yargor_mode_trolleybus
import farebot.transit.yargor.generated.resources.yargor_unknown_format
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlin.time.Instant
import com.codebutler.farebot.base.util.FormattedString

class YarGorTrip(
    override val startTimestamp: Instant,
    private val mRoute: Int,
    private val mVehicle: Int,
) : Trip() {
    override val fare: TransitCurrency?
        get() = null

    override val mode: Mode
        get() {
            val lineMode = MdstStationTableReader.getReader(YARGOR_STR)?.getLineTransport(mRoute)
            if (lineMode != null) {
                return when (lineMode) {
                    com.codebutler.farebot.base.mdst.TransportType.BUS -> Mode.BUS
                    com.codebutler.farebot.base.mdst.TransportType.TRAM -> Mode.TRAM
                    com.codebutler.farebot.base.mdst.TransportType.TROLLEYBUS -> Mode.TROLLEYBUS
                    com.codebutler.farebot.base.mdst.TransportType.TRAIN -> Mode.TRAIN
                    com.codebutler.farebot.base.mdst.TransportType.METRO -> Mode.METRO
                    com.codebutler.farebot.base.mdst.TransportType.FERRY -> Mode.FERRY
                    com.codebutler.farebot.base.mdst.TransportType.MONORAIL -> Mode.MONORAIL
                    else -> Mode.OTHER
                }
            }
            return when (mRoute / 100) {
                0, 20 -> Mode.BUS
                1 -> Mode.TRAM
                2, 3 -> Mode.TROLLEYBUS
                else -> Mode.OTHER
            }
        }

    override val agencyName: FormattedString?
        get() =
            when (mode) {
                Mode.TRAM -> FormattedString(Res.string.yargor_mode_tram)
                Mode.TROLLEYBUS -> FormattedString(Res.string.yargor_mode_trolleybus)
                Mode.BUS -> FormattedString(Res.string.yargor_mode_bus)
                else -> FormattedString(Res.string.yargor_unknown_format, (mRoute / 100).toString())
            }

    override val routeName: FormattedString?
        get() {
            val reader = MdstStationTableReader.getReader(YARGOR_STR)
            val line = reader?.getLine(mRoute)
            val name = line?.name?.english
            if (!name.isNullOrEmpty()) return FormattedString(name)
            return FormattedString((mRoute % 100).toString())
        }

    override val vehicleID: String?
        get() = mVehicle.toString()

    companion object {
        private const val YARGOR_STR = "yargor"

        private fun parseTimestampBCD(
            data: ByteArray,
            off: Int,
        ): Instant {
            val year = 2000 + NumberUtils.convertBCDtoInteger(data[off])
            val month = NumberUtils.convertBCDtoInteger(data[off + 1])
            val day = NumberUtils.convertBCDtoInteger(data[off + 2])
            val hour = NumberUtils.convertBCDtoInteger(data[off + 3])
            val min = NumberUtils.convertBCDtoInteger(data[off + 4])
            val sec = NumberUtils.convertBCDtoInteger(data[off + 5])
            val ldt = LocalDateTime(year, month, day, hour, min, sec)
            return ldt.toInstant(YarGorTransitInfo.TZ)
        }

        fun parse(input: ByteArray): YarGorTrip? {
            if (input[9] == 0.toByte()) {
                return null
            }
            return YarGorTrip(
                startTimestamp = parseTimestampBCD(input, 9),
                mVehicle = input.byteArrayToIntReversed(0, 2),
                mRoute = input.byteArrayToIntReversed(3, 2),
            )
        }
    }
}
