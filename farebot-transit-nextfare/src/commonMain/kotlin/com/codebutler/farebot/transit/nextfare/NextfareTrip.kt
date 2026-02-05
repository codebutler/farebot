/*
 * NextfareTrip.kt
 *
 * Copyright 2015-2019 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.nextfare

import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import kotlin.time.Instant

/**
 * Mutable capsule holding trip data parsed from Nextfare records.
 * Subclasses can extend this to add system-specific station lookups, etc.
 */
data class NextfareTripCapsule(
    var journeyId: Int = 0,
    var isTopup: Boolean = false,
    var modeInt: Int = 0,
    var startTimestamp: Instant? = null,
    var endTimestamp: Instant? = null,
    var startStation: Int = -1,
    var endStation: Int = -1,
    var isTransfer: Boolean = false,
    var cost: Int = 0
)

/**
 * Represents trips on Nextfare.
 * Subclasses should override [getStation] and [lookupMode] for system-specific behavior,
 * and [currency] to provide the correct currency factory.
 */
open class NextfareTrip(
    val capsule: NextfareTripCapsule,
    private val currencyFactory: (Int) -> TransitCurrency = { TransitCurrency.XXX(it) }
) : Trip() {

    override val startTimestamp: Instant? get() = capsule.startTimestamp

    override val endTimestamp: Instant? get() = capsule.endTimestamp

    override val startStation: Station?
        get() {
            if (capsule.startStation < 0) return null
            return getStation(capsule.startStation)
        }

    override val endStation: Station?
        get() {
            if (capsule.endTimestamp == null || capsule.endStation < 0) return null
            return getStation(capsule.endStation)
        }

    override val fare: TransitCurrency?
        get() {
            if (capsule.cost == 0) return null
            return currencyFactory(capsule.cost)
        }

    override val mode: Mode
        get() = if (capsule.isTopup) Mode.TICKET_MACHINE else lookupMode()

    override val isTransfer: Boolean get() = capsule.isTransfer

    /**
     * Look up a station by its ID. Override in subclasses to provide
     * system-specific station databases.
     */
    protected open fun getStation(stationId: Int): Station? = null

    /**
     * Look up the transport mode. Override in subclasses to provide
     * system-specific mode mapping.
     */
    protected open fun lookupMode(): Mode = Mode.OTHER
}
