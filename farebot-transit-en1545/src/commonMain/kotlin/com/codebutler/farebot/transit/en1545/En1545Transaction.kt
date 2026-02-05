/*
 * En1545Transaction.kt
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

package com.codebutler.farebot.transit.en1545

import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.Transaction
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import kotlin.time.Instant

abstract class En1545Transaction : Transaction() {
    abstract val parsed: En1545Parsed
    protected abstract val lookup: En1545Lookup

    protected open val routeNumber: Int?
        get() = parsed.getInt(EVENT_ROUTE_NUMBER)

    protected val routeVariant: Int?
        get() = parsed.getInt(EVENT_ROUTE_VARIANT)

    override val routeNames: List<String>
        get() {
            val route = lookup.getRouteName(
                routeNumber,
                routeVariant,
                agency, transport
            )
            if (route != null) {
                return listOf(route)
            }
            val st = station ?: return emptyList()
            return st.lineNames
        }

    override val humanReadableLineIDs: List<String>
        get() {
            val route = lookup.getHumanReadableRouteId(
                routeNumber,
                routeVariant,
                agency, transport
            )
            if (route != null) {
                return listOf(route)
            }
            val st = station ?: return emptyList()
            return st.humanReadableLineIds
        }

    override val passengerCount: Int
        get() = parsed.getIntOrZero(EVENT_PASSENGER_COUNT)

    override val vehicleID: String?
        get() {
            val id = parsed.getIntOrZero(EVENT_VEHICLE_ID)
            return if (id == 0) null else id.toString()
        }

    override val machineID: String?
        get() {
            val id = parsed.getIntOrZero(EVENT_DEVICE_ID)
            return if (id == 0) null else id.toString()
        }

    private val eventCode: Int
        get() = parsed.getIntOrZero(EVENT_CODE)

    protected open val transport: Int
        get() = getTransport(eventCode)

    protected open val agency: Int?
        get() = parsed.getInt(EVENT_SERVICE_PROVIDER)

    override val station: Station?
        get() = getStation(stationId)

    override val timestamp: Instant?
        get() = parsed.getTimeStamp(EVENT, lookup.timeZone)

    override val mode: Trip.Mode
        get() = parsed.getInt(EVENT_CODE)?.let { eventCodeToMode(it) }
            ?: lookup.getMode(agency, parsed.getInt(EVENT_ROUTE_NUMBER))

    override val fare: TransitCurrency?
        get() {
            val x = parsed.getInt(EVENT_PRICE_AMOUNT) ?: return null
            return lookup.parseCurrency(x)
        }

    protected open val eventType: Int
        get() = eventCode and 0xf

    override val isTapOn: Boolean
        get() {
            val eventCode = eventType
            return eventCode == EVENT_TYPE_BOARD || eventCode == EVENT_TYPE_BOARD_TRANSFER
        }

    override val isTapOff: Boolean
        get() {
            val eventCode = eventType
            return eventCode == EVENT_TYPE_EXIT || eventCode == EVENT_TYPE_EXIT_TRANSFER
        }

    override val isTransfer: Boolean
        get() {
            val eventCode = eventType
            return eventCode == EVENT_TYPE_BOARD_TRANSFER || eventCode == EVENT_TYPE_EXIT_TRANSFER
        }

    protected open val stationId: Int?
        get() = parsed.getInt(EVENT_LOCATION_ID)

    override val isRejected: Boolean
        get() = parsed.getIntOrZero(EVENT_RESULT) != 0

    override val agencyName: String?
        get() = lookup.getAgencyName(agency, false)

    override val shortAgencyName: String?
        get() = lookup.getAgencyName(agency, true)

    open fun getStation(station: Int?): Station? {
        return if (station == null) null else lookup.getStation(station, agency, transport)
    }

    override fun isSameTrip(other: Transaction): Boolean {
        if (other !is En1545Transaction)
            return false
        return (transport == other.transport
            && parsed.getIntOrZero(EVENT_SERVICE_PROVIDER) == other.parsed.getIntOrZero(EVENT_SERVICE_PROVIDER)
            && parsed.getIntOrZero(EVENT_ROUTE_NUMBER) == other.parsed.getIntOrZero(EVENT_ROUTE_NUMBER)
            && parsed.getIntOrZero(EVENT_ROUTE_VARIANT) == other.parsed.getIntOrZero(EVENT_ROUTE_VARIANT))
    }

    override fun toString(): String = "En1545Transaction: $parsed"

    companion object {
        const val EVENT_ROUTE_NUMBER = "EventRouteNumber"
        const val EVENT_ROUTE_VARIANT = "EventRouteVariant"
        const val EVENT_PASSENGER_COUNT = "EventPassengerCount"
        const val EVENT_VEHICLE_ID = "EventVehicleId"
        const val EVENT_CODE = "EventCode"
        const val EVENT_SERVICE_PROVIDER = "EventServiceProvider"
        const val EVENT = "Event"
        const val EVENT_PRICE_AMOUNT = "EventPriceAmount"
        const val EVENT_LOCATION_ID = "EventLocationId"
        const val EVENT_UNKNOWN_A = "EventUnknownA"
        const val EVENT_UNKNOWN_B = "EventUnknownB"
        const val EVENT_UNKNOWN_C = "EventUnknownC"
        const val EVENT_UNKNOWN_D = "EventUnknownD"
        const val EVENT_UNKNOWN_E = "EventUnknownE"
        const val EVENT_UNKNOWN_F = "EventUnknownF"
        const val EVENT_UNKNOWN_G = "EventUnknownG"
        const val EVENT_UNKNOWN_H = "EventUnknownH"
        const val EVENT_UNKNOWN_I = "EventUnknownI"
        const val EVENT_CONTRACT_POINTER = "EventContractPointer"
        const val EVENT_CONTRACT_TARIFF = "EventContractTariff"
        const val EVENT_SERIAL_NUMBER = "EventSerialNumber"
        const val EVENT_AUTHENTICATOR = "EventAuthenticator"
        const val EVENT_NETWORK_ID = "EventNetworkId"
        const val EVENT_FIRST_STAMP = "EventFirstStamp"
        const val EVENT_FIRST_LOCATION_ID = "EventFirstLocationId"
        const val EVENT_DEVICE_ID = "EventDeviceId"
        const val EVENT_RESULT = "EventResult"
        const val EVENT_DISPLAY_DATA = "EventDisplayData"
        const val EVENT_NOT_OK_COUNTER = "EventNotOkCounter"
        const val EVENT_DESTINATION = "EventDestination"
        const val EVENT_LOCATION_GATE = "EventLocationGate"
        const val EVENT_DEVICE = "EventDevice"
        const val EVENT_JOURNEY_RUN = "EventJourneyRun"
        const val EVENT_VEHICULE_CLASS = "EventVehiculeClass"
        const val EVENT_LOCATION_TYPE = "EventLocationType"
        const val EVENT_EMPLOYEE = "EventEmployee"
        const val EVENT_LOCATION_REFERENCE = "EventLocationReference"
        const val EVENT_JOURNEY_INTERCHANGES = "EventJourneyInterchanges"
        const val EVENT_PERIOD_JOURNEYS = "EventPeriodJourneys"
        const val EVENT_TOTAL_JOURNEYS = "EventTotalJourneys"
        const val EVENT_JOURNEY_DISTANCE = "EventJourneyDistance"
        const val EVENT_PRICE_UNIT = "EventPriceUnit"
        const val EVENT_DATA_SIMULATION = "EventDataSimulation"
        const val EVENT_DATA_TRIP = "EventDataTrip"
        const val EVENT_DATA_ROUTE_DIRECTION = "EventDataRouteDirection"

        private const val EVENT_TYPE_BOARD = 1
        private const val EVENT_TYPE_EXIT = 2
        private const val EVENT_TYPE_BOARD_TRANSFER = 6
        private const val EVENT_TYPE_EXIT_TRANSFER = 7
        const val EVENT_TYPE_TOPUP = 13
        const val EVENT_TYPE_CANCELLED = 9
        const val TRANSPORT_UNSPECIFIED = 0
        const val TRANSPORT_BUS = 1
        private const val TRANSPORT_INTERCITY_BUS = 2
        const val TRANSPORT_METRO = 3
        const val TRANSPORT_TRAM = 4
        const val TRANSPORT_TRAIN = 5
        private const val TRANSPORT_FERRY = 6
        private const val TRANSPORT_PARKING = 8
        private const val TRANSPORT_TAXI = 9
        private const val TRANSPORT_TOPUP = 11

        private fun getTransport(eventCode: Int): Int {
            return eventCode shr 4
        }

        private fun eventCodeToMode(ec: Int): Trip.Mode? {
            if (ec and 0xf == EVENT_TYPE_TOPUP)
                return Trip.Mode.TICKET_MACHINE
            return when (getTransport(ec)) {
                TRANSPORT_BUS, TRANSPORT_INTERCITY_BUS -> Trip.Mode.BUS
                TRANSPORT_METRO -> Trip.Mode.METRO
                TRANSPORT_TRAM -> Trip.Mode.TRAM
                TRANSPORT_TRAIN -> Trip.Mode.TRAIN
                TRANSPORT_FERRY -> Trip.Mode.FERRY
                TRANSPORT_PARKING, TRANSPORT_TAXI -> Trip.Mode.OTHER
                TRANSPORT_TOPUP -> Trip.Mode.TICKET_MACHINE
                TRANSPORT_UNSPECIFIED -> null
                else -> Trip.Mode.OTHER
            }
        }
    }
}
