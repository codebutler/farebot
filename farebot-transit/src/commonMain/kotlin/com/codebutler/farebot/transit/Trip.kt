/*
 * Trip.kt
 *
 * Copyright (C) 2011-2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2016, 2018-2019 Michael Farrell <micolous+git@gmail.com>
 * Copyright (C) 2019 Google
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

package com.codebutler.farebot.transit

import kotlin.time.Instant

abstract class Trip {

    /**
     * Starting time of the trip.
     */
    abstract val startTimestamp: Instant?

    /**
     * Ending time of the trip. If this is not known, return null.
     */
    open val endTimestamp: Instant? get() = null

    /**
     * Route name for the trip. This could be a bus line, a tram line, a rail line, etc.
     *
     * The default implementation attempts to get the route name based on the start and end
     * stations' line names, finding a common line between them.
     */
    open val routeName: String?
        get() {
            val startLines = startStation?.lineNames.orEmpty()
            val endLines = endStation?.lineNames.orEmpty()
            return getRouteName(startLines, endLines)
        }

    /**
     * Route IDs for the trip, for display when raw station IDs are shown.
     *
     * The default implementation attempts to derive this from station humanReadableLineIds.
     */
    open val humanReadableRouteID: String?
        get() {
            val startLines = startStation?.humanReadableLineIds.orEmpty()
            val endLines = endStation?.humanReadableLineIds.orEmpty()
            return getRouteName(startLines, endLines)
        }

    open val fare: TransitCurrency? get() = null

    open val fareString: String? get() = null

    abstract val mode: Mode

    /**
     * Full name of the agency for the trip.
     * If this is not known (or there is only one agency for the card), then return null.
     */
    open val agencyName: String? get() = null

    /**
     * Short name of the agency for the trip, for use in compact displays.
     * By default, this returns [agencyName].
     */
    open val shortAgencyName: String? get() = agencyName

    /**
     * Vehicle number where the event was recorded.
     * This is generally *not* the Station ID.
     */
    open val vehicleID: String? get() = null

    /**
     * Machine ID that recorded the transaction (farebox, ticket machine, or validator).
     * This is generally *not* the Station ID.
     */
    open val machineID: String? get() = null

    /**
     * Number of passengers. -1 is unknown or irrelevant.
     */
    open val passengerCount: Int get() = -1

    /**
     * Starting station info for the trip, or null if there is no station information available.
     */
    open val startStation: Station? get() = null

    /**
     * Ending station info for the trip, or null if there is no station information available.
     */
    open val endStation: Station? get() = null

    /**
     * If the trip is a transfer from another service, return true.
     */
    open val isTransfer: Boolean get() = false

    /**
     * If the tap-on event was rejected for the trip, return true.
     */
    open val isRejected: Boolean get() = false

    fun hasLocation(): Boolean =
        (startStation?.hasLocation() == true) || (endStation?.hasLocation() == true)

    enum class Mode {
        BUS,
        /** Used for non-metro (rapid transit) trains */
        TRAIN,
        /** Used for trams and light rail */
        TRAM,
        /** Used for electric metro and subway systems */
        METRO,
        FERRY,
        TICKET_MACHINE,
        VENDING_MACHINE,
        /** Used for transactions at a store, buying something other than travel. */
        POS,
        OTHER,
        BANNED,
        TROLLEYBUS,
        TOLL_ROAD,
        MONORAIL,
        CABLECAR
    }

    class Comparator : kotlin.Comparator<Trip> {
        override fun compare(a: Trip, b: Trip): Int {
            val aTs = a.startTimestamp ?: a.endTimestamp
            val bTs = b.startTimestamp ?: b.endTimestamp
            return when {
                aTs == null && bTs == null -> 0
                aTs == null -> 1
                bTs == null -> -1
                else -> bTs.compareTo(aTs)
            }
        }
    }

    companion object {
        /**
         * Finds a common route name between the start and end station line names.
         */
        fun getRouteName(startLines: List<String>, endLines: List<String>): String? {
            if (startLines.isEmpty() && endLines.isEmpty()) {
                return null
            }

            // Method 1: if only the start is set, use the first start line.
            if (endLines.isEmpty()) {
                return startLines[0]
            }

            // Method 2: if only the end is set, use the first end line.
            if (startLines.isEmpty()) {
                return endLines[0]
            }

            // Now there is at least 1 candidate line from each group.

            // Method 3: get the intersection of the two lists
            val lines = startLines.toSet() intersect endLines.toSet()
            if (lines.isNotEmpty()) {
                if (lines.size == 1) {
                    return lines.iterator().next()
                }

                // More than one common line. Return the first one in start station order.
                for (candidateLine in startLines) {
                    if (lines.contains(candidateLine)) {
                        return candidateLine
                    }
                }
            }

            // No overlapping lines. Return the first associated with the start station.
            return startLines[0]
        }

        /**
         * Formats a trip description into a label with start and end station names.
         *
         * @return null if both start and end stations are unknown.
         */
        fun formatStationNames(trip: Trip): String? {
            val startStationName = trip.startStation?.stationName

            val endStationName: String?
            if (trip.endStation?.getStationName(false) == trip.startStation?.getStationName(false)) {
                endStationName = null
            } else {
                endStationName = trip.endStation?.stationName
            }

            return when {
                startStationName != null && endStationName != null ->
                    "$startStationName \u2192 $endStationName"
                startStationName != null -> startStationName
                endStationName != null -> "\u2192 $endStationName"
                else -> null
            }
        }
    }
}
