/*
 * KMTTrip.kt
 *
 * Authors:
 * Bondan Sumbodo <sybond@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.codebutler.farebot.transit.kmt

import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.card.felica.FeliCaUtil
import com.codebutler.farebot.card.felica.FelicaBlock
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import farebot.farebot_transit_kmt.generated.resources.Res
import farebot.farebot_transit_kmt.generated.resources.kmt_agency
import farebot.farebot_transit_kmt.generated.resources.kmt_agency_short
import farebot.farebot_transit_kmt.generated.resources.kmt_defroute
import kotlin.time.Instant

class KMTTrip(
    private val processType: Int,
    private val sequenceNumber: Int,
    private val timestampData: Instant,
    private val transactionAmount: Int,
    private val endStationData: Int,
) : Trip() {
    override val startTimestamp: Instant get() = timestampData

    override val mode: Mode
        get() =
            when (processType) {
                0 -> Mode.TICKET_MACHINE
                1 -> Mode.TRAIN
                2 -> Mode.POS
                else -> Mode.OTHER
            }

    override val fare: TransitCurrency
        get() {
            var tripFare = transactionAmount
            // Top-ups (processType != 1) should be negative (credit)
            // Train rides (processType == 1) should be positive (debit)
            if (processType != 1) {
                tripFare *= -1
            }
            return TransitCurrency.IDR(tripFare)
        }

    override val agencyName: String
        get() = getStringBlocking(Res.string.kmt_agency)

    override val shortAgencyName: String
        get() = getStringBlocking(Res.string.kmt_agency_short)

    override val routeName: String
        get() = getStringBlocking(Res.string.kmt_defroute)

    override val startStation: Station?
        get() =
            if (processType != 1) {
                // Top-ups use startStation
                KMTData.getStation(endStationData)
                    ?: Station.unknown("0x${endStationData.toString(16)}")
            } else {
                null
            }

    override val endStation: Station?
        get() =
            if (processType == 1) {
                // Train rides use endStation
                KMTData.getStation(endStationData)
                    ?: Station.unknown("0x${endStationData.toString(16)}")
            } else {
                null
            }

    fun getProcessType(): Int = processType

    fun getSequenceNumber(): Int = sequenceNumber

    fun getTimestampData(): Instant = timestampData

    fun getTransactionAmount(): Int = transactionAmount

    fun getEndStationData(): Int = endStationData

    companion object {
        fun create(block: FelicaBlock): KMTTrip {
            val data = block.data
            val processType = data[12].toInt()
            val sequenceNumber = FeliCaUtil.toInt(data[13], data[14], data[15])
            val timestampData = KMTUtil.extractDate(data)!!
            val transactionAmount = FeliCaUtil.toInt(data[4], data[5], data[6], data[7])
            val endStationData = FeliCaUtil.toInt(data[8], data[9])
            return KMTTrip(processType, sequenceNumber, timestampData, transactionAmount, endStationData)
        }
    }
}
