/*
 * HSLTrip.kt
 *
 * Copyright (C) 2014, 2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.hsl

import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import farebot.farebot_transit_hsl.generated.resources.*
import kotlin.time.Instant

internal class HSLTrip(
    private val timestampValue: Long,
    val line: String?,
    val vehicleNumber: Long,
    private val fareValue: Long,
    val arvo: Long,
    private val expireTimestamp: Long,
    private val pax: Long,
    private val newBalance: Long,
    private val stringResource: StringResource,
    val isRefill: Boolean = false,
) : Trip() {

    override val startTimestamp: Instant
        get() = Instant.fromEpochSeconds(timestampValue)

    override val fare: TransitCurrency
        get() = TransitCurrency.EUR(fareValue.toInt())

    override val agencyName: String
        get() = when {
            isRefill -> stringResource.getString(Res.string.hsl_balance_refill)
            arvo == 1L -> {
                val mins = (expireTimestamp - timestampValue) / 60
                stringResource.getString(Res.string.hsl_balance_ticket, pax.toString(), mins.toString())
            }
            else -> stringResource.getString(Res.string.hsl_pass_ticket, pax.toString())
        }

    override val routeName: String?
        get() {
            if (line == null) return null
            val lineStr = line.substring(1)
            return stringResource.getString(Res.string.hsl_route_line_vehicle, lineStr, vehicleNumber.toString())
        }

    override val mode: Mode
        get() = when {
            isRefill -> Mode.TICKET_MACHINE
            line == null -> Mode.BUS
            line == "1300" -> Mode.METRO
            line == "1019" -> Mode.FERRY
            line.startsWith("100") || line == "1010" -> Mode.TRAM
            line.startsWith("3") -> Mode.TRAIN
            else -> Mode.BUS
        }

    override val passengerCount: Int get() = pax.toInt()

    fun withLineAndVehicleNumber(newLine: String?, newVehicleNumber: Long): HSLTrip =
        HSLTrip(timestampValue, newLine, newVehicleNumber, fareValue, arvo, expireTimestamp, pax, newBalance, stringResource, isRefill)
}
