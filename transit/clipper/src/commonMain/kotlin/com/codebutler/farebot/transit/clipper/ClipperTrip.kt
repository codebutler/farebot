/*
 * ClipperTrip.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014, 2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2014 Bao-Long Nguyen-Trong <baolong@inkling.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
 * Copyright (C) 2018 Google
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

package com.codebutler.farebot.transit.clipper

import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import kotlin.time.Instant

class ClipperTrip(
    private val timestamp: Long = 0,
    private val exitTimestampValue: Long = 0,
    private val balance: Long = 0,
    private val fareValue: Long = 0,
    private val agency: Long = 0,
    private val from: Long = 0,
    private val to: Long = 0,
    private val route: Long = 0,
    private val vehicleNum: Long = 0,
    private val transportCode: Long = 0,
) : Trip() {
    override val startTimestamp: Instant
        get() = Instant.fromEpochSeconds(timestamp)

    override val endTimestamp: Instant?
        get() = if (exitTimestampValue != 0L) Instant.fromEpochSeconds(exitTimestampValue) else null

    override val fare: TransitCurrency
        get() = TransitCurrency.USD(fareValue.toInt())

    override val agencyName: FormattedString
        get() = ClipperData.getAgencyName(agency.toInt())

    override val shortAgencyName: FormattedString
        get() = ClipperData.getShortAgencyName(agency.toInt())

    override val routeName: FormattedString?
        get() = ClipperData.getRouteName(agency.toInt(), route.toInt())?.let { FormattedString(it) }

    override val startStation: Station?
        get() = ClipperData.getStation(agency.toInt(), from.toInt(), false)

    override val endStation: Station?
        get() = ClipperData.getStation(agency.toInt(), to.toInt(), true)

    override val mode: Mode
        get() = ClipperData.getMode(agency.toInt(), transportCode.toInt())

    /**
     * Vehicle number display, handling LRV4 Muni vehicle numbering scheme.
     * For newer Clipper readers on LRV4 Muni vehicles, the vehicle number is encoded
     * as (number * 10 + letter), where the letter is 0-15 (A-P).
     */
    override val vehicleID: String?
        get() =
            when (vehicleNum.toInt()) {
                0, 0xffff -> null
                in 1..9999 -> vehicleNum.toString()
                else -> {
                    // LRV4 Muni vehicle: number/10 + letter from remainder
                    val num = vehicleNum.toInt() / 10
                    val letterIndex = (vehicleNum.toInt() % 10) + 9
                    num.toString() + letterIndex.toString(16).uppercase()
                }
            }

    /**
     * For GG Ferry, display the route ID in hex as the raw identifier.
     */
    override val humanReadableRouteID: String?
        get() =
            if (agency.toInt() == ClipperData.AGENCY_GG_FERRY) {
                "0x${route.toInt().toString(16)}"
            } else {
                null
            }

    fun getBalance(): Long = balance

    fun getFareValue(): Long = fareValue

    fun getAgency(): Long = agency

    fun getFrom(): Long = from

    fun getTo(): Long = to

    fun getRoute(): Long = route

    fun withBalance(newBalance: Long): ClipperTrip =
        ClipperTrip(
            timestamp,
            exitTimestampValue,
            newBalance,
            fareValue,
            agency,
            from,
            to,
            route,
            vehicleNum,
            transportCode,
        )
}
