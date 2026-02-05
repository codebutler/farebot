/*
 * OVChipTrip.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2012, 2014-2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.ovc

import com.codebutler.farebot.base.mdst.MdstStationLookup
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import kotlin.time.Instant

class OVChipTrip(
    private val id: Int,
    private val processType: Int,
    private val agencyValue: Int,
    private val isBus: Boolean,
    private val isTrain: Boolean,
    private val isMetro: Boolean,
    private val isFerry: Boolean,
    private val isOther: Boolean,
    private val isCharge: Boolean,
    private val isPurchase: Boolean,
    private val isBanned: Boolean,
    private val timestampData: Instant?,
    private val fareValue: Long,
    private val exitTimestampData: Instant?,
    private val startStationValue: Station?,
    private val endStationValue: Station?,
    private val startStationId: Int,
    private val endStationId: Int,
    private val stringResource: StringResource
) : Trip() {

    override val startTimestamp: Instant? get() = timestampData

    override val endTimestamp: Instant? get() = exitTimestampData

    override val agencyName: String
        get() = OVChipTransitInfo.getShortAgencyName(stringResource, agencyValue)

    override val shortAgencyName: String
        get() = OVChipTransitInfo.getShortAgencyName(stringResource, agencyValue)

    override val fare: TransitCurrency
        get() = TransitCurrency.EUR(fareValue.toInt())

    override val startStation: Station?
        get() = startStationValue

    override val endStation: Station?
        get() = endStationValue

    override val mode: Mode
        get() = when {
            isBanned -> Mode.BANNED
            isCharge -> Mode.TICKET_MACHINE
            isPurchase -> Mode.VENDING_MACHINE
            isTrain -> Mode.TRAIN
            isBus -> Mode.BUS
            isMetro -> Mode.METRO
            isFerry -> Mode.FERRY
            isOther -> Mode.OTHER
            else -> Mode.OTHER
        }

    companion object {
        private const val OVC_STR = "ovc"

        val ID_ORDER: kotlin.Comparator<OVChipTrip> = kotlin.Comparator { t1, t2 ->
            t1.id.compareTo(t2.id)
        }

        fun create(transaction: OVChipTransaction, stringResource: StringResource): OVChipTrip {
            return create(transaction, null, stringResource)
        }

        fun create(inTransaction: OVChipTransaction, outTransaction: OVChipTransaction?, stringResource: StringResource): OVChipTrip {
            val id = inTransaction.id
            val processType = inTransaction.transfer
            val agency = inTransaction.company
            val timestamp = OVChipUtil.convertDate(inTransaction.date, inTransaction.time)
            val startStationId = inTransaction.station
            val startStation = getStation(agency, startStationId)

            val endStationId: Int
            val endStation: Station?
            val exitTimestamp: Instant?
            val fare: Int

            if (outTransaction != null) {
                endStationId = outTransaction.station
                endStation = getStation(agency, outTransaction.station)
                    ?: Station.unknown(endStationId.toString())
                exitTimestamp = OVChipUtil.convertDate(outTransaction.date, outTransaction.time)
                fare = outTransaction.amount
            } else {
                endStation = null
                endStationId = 0
                exitTimestamp = null
                fare = inTransaction.amount
            }

            val isTrain = (agency == OVChipTransitInfo.AGENCY_NS) ||
                    ((agency == OVChipTransitInfo.AGENCY_ARRIVA) && (startStationId < 800))
            val isMetro = (agency == OVChipTransitInfo.AGENCY_GVB && startStationId < 3000) ||
                    (agency == OVChipTransitInfo.AGENCY_RET && startStationId < 3000)
            val isOther = agency == OVChipTransitInfo.AGENCY_TLS || agency == OVChipTransitInfo.AGENCY_DUO ||
                    agency == OVChipTransitInfo.AGENCY_STORE
            val isFerry = agency == OVChipTransitInfo.AGENCY_ARRIVA && (startStationId in 4601..4699)
            val isBus = !isTrain && !isMetro && !isOther && !isFerry
            val isCharge = (processType == OVChipTransitInfo.PROCESS_CREDIT) ||
                    (processType == OVChipTransitInfo.PROCESS_TRANSFER)
            val isPurchase = (processType == OVChipTransitInfo.PROCESS_PURCHASE) ||
                    (processType == OVChipTransitInfo.PROCESS_NODATA)
            val isBanned = processType == OVChipTransitInfo.PROCESS_BANNED

            return OVChipTrip(
                id = id,
                processType = processType,
                agencyValue = agency,
                isBus = isBus,
                isTrain = isTrain,
                isMetro = isMetro,
                isFerry = isFerry,
                isOther = isOther,
                isCharge = isCharge,
                isPurchase = isPurchase,
                isBanned = isBanned,
                timestampData = timestamp,
                fareValue = fare.toLong(),
                exitTimestampData = exitTimestamp,
                startStationValue = startStation,
                endStationValue = endStation,
                startStationId = startStationId,
                endStationId = endStationId,
                stringResource = stringResource
            )
        }

        private fun getStation(companyCode: Int, stationCode: Int): Station? {
            val stationId = ((companyCode - 1) shl 16) or stationCode
            val result = MdstStationLookup.getStation(OVC_STR, stationId) ?: return null

            return Station.builder()
                .stationName(result.stationName)
                .companyName(result.companyName)
                .latitude(if (result.hasLocation) result.latitude.toString() else null)
                .longitude(if (result.hasLocation) result.longitude.toString() else null)
                .build()
        }
    }
}
