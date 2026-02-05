/*
 * ObfuscatedTrip.kt
 *
 * Copyright 2017 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit

import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.random.Random

/**
 * Special wrapper for Trip that handles obfuscation of Trip data.
 *
 * This class holds trip data where timestamps have been shifted by a consistent delta
 * and fares have optionally been obfuscated.
 */
@Serializable
data class ObfuscatedTrip(
    @SerialName("startTimestamp")
    private val _startTimestamp: Long? = null,
    @SerialName("endTimestamp")
    private val _endTimestamp: Long? = null,
    @SerialName("routeName")
    private val _routeName: String? = null,
    @SerialName("startStation")
    private val _startStation: Station? = null,
    @SerialName("endStation")
    private val _endStation: Station? = null,
    @SerialName("mode")
    private val _mode: Mode,
    @SerialName("fare")
    private val _fare: TransitCurrency? = null,
    @SerialName("humanReadableRouteID")
    private val _humanReadableRouteID: String? = null,
    @SerialName("vehicleID")
    private val _vehicleID: String? = null,
    @SerialName("machineID")
    private val _machineID: String? = null,
    @SerialName("passengerCount")
    private val _passengerCount: Int = -1,
    @SerialName("agencyName")
    private val _agencyName: String? = null,
    @SerialName("shortAgencyName")
    private val _shortAgencyName: String? = null,
    @SerialName("isTransfer")
    private val _isTransfer: Boolean = false,
    @SerialName("isRejected")
    private val _isRejected: Boolean = false
) : Trip() {

    override val startTimestamp: Instant?
        get() = _startTimestamp?.let { Instant.fromEpochMilliseconds(it) }

    override val endTimestamp: Instant?
        get() = _endTimestamp?.let { Instant.fromEpochMilliseconds(it) }

    override val routeName: String?
        get() = _routeName

    override val startStation: Station?
        get() = _startStation

    override val endStation: Station?
        get() = _endStation

    override val mode: Mode
        get() = _mode

    override val fare: TransitCurrency?
        get() = _fare

    override val humanReadableRouteID: String?
        get() = _humanReadableRouteID

    override val vehicleID: String?
        get() = _vehicleID

    override val machineID: String?
        get() = _machineID

    override val passengerCount: Int
        get() = _passengerCount

    override val agencyName: String?
        get() = _agencyName

    override val shortAgencyName: String?
        get() = _shortAgencyName

    override val isTransfer: Boolean
        get() = _isTransfer

    override val isRejected: Boolean
        get() = _isRejected

    /**
     * Creates an ObfuscatedTrip from a real Trip.
     *
     * @param realTrip The original trip to obfuscate
     * @param timeDelta The time delta in milliseconds to apply to timestamps
     * @param obfuscateFares Whether to obfuscate fare values
     * @param random Random source for fare obfuscation
     */
    constructor(
        realTrip: Trip,
        timeDelta: Long,
        obfuscateFares: Boolean,
        random: Random = Random.Default
    ) : this(
        _startTimestamp = realTrip.startTimestamp?.let {
            it.toEpochMilliseconds() + timeDelta
        },
        _endTimestamp = realTrip.endTimestamp?.let {
            it.toEpochMilliseconds() + timeDelta
        },
        _routeName = realTrip.routeName,
        _startStation = realTrip.startStation,
        _endStation = realTrip.endStation,
        _mode = realTrip.mode,
        _fare = realTrip.fare?.let {
            if (obfuscateFares) {
                TripObfuscator.obfuscateCurrency(it, random)
            } else {
                it
            }
        },
        _humanReadableRouteID = realTrip.humanReadableRouteID,
        _vehicleID = realTrip.vehicleID,
        _machineID = realTrip.machineID,
        _passengerCount = realTrip.passengerCount,
        _agencyName = realTrip.agencyName,
        _shortAgencyName = realTrip.shortAgencyName,
        _isTransfer = realTrip.isTransfer,
        _isRejected = realTrip.isRejected
    )
}
