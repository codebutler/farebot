/*
 * RkfTCSTTrip.kt
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

package com.codebutler.farebot.transit.rkf

import com.codebutler.farebot.base.util.getBitsFromBufferLeBits
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.en1545.En1545Container
import com.codebutler.farebot.transit.en1545.En1545FixedHex
import com.codebutler.farebot.transit.en1545.En1545FixedInteger
import com.codebutler.farebot.transit.en1545.En1545Parsed
import com.codebutler.farebot.transit.en1545.En1545Parser
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

data class RkfTCSTTrip(
    private val mParsed: En1545Parsed,
    private val mLookup: RkfLookup,
    private val mTransactions: MutableList<RkfTransaction> = mutableListOf()
) {
    internal val checkoutCompleted
        get() = mParsed.getIntOrZero(VALIDATION_STATUS) == 2 && mParsed.getIntOrZero(VALIDATION_MODEL) == 1
    private val inProgress
        get() = mParsed.getIntOrZero(VALIDATION_STATUS) == 1 && mParsed.getIntOrZero(VALIDATION_MODEL) == 1

    private val passengerCount: Int
        get() = (1..3).sumOf { mParsed.getIntOrZero(passengerTotal(it)) }

    val startTimestamp: Instant?
        get() = parseDateTime(mParsed.getIntOrZero(START_TIME), mLookup.timeZone)

    val endTimestamp: Instant?
        get() = parseDateTime(mParsed.getIntOrZero(START_TIME) + mParsed.getIntOrZero(DESTINATION_TIME), mLookup.timeZone)

    val fare: TransitCurrency
        get() = if (inProgress)
            mLookup.parseCurrency(mParsed.getIntOrZero(PRICE) + mParsed.getIntOrZero(DEPOSIT))
        else mLookup.parseCurrency(mParsed.getIntOrZero(PRICE))

    val mode: Trip.Mode
        get() = mLookup.getMode(mParsed.getIntOrZero(START_AID), 0)

    fun getAgencyName(isShort: Boolean) = mLookup.getAgencyName(mParsed.getIntOrZero(RkfTransitInfo.COMPANY), isShort)

    private val startStation: Station?
        get() = mLookup.getStation(mParsed.getIntOrZero(START_PLACE), mParsed.getIntOrZero(START_AID), null)

    private val endStation: Station?
        get() = if (checkoutCompleted) mLookup.getStation(mParsed.getIntOrZero(DESTINATION_PLACE), mParsed.getIntOrZero(DESTINATION_AID), null) else null

    fun addTransaction(transaction: RkfTransaction) {
        mTransactions.add(transaction)
    }

    val tripLegs: List<RkfTripLeg>
        get() {
            val legs = mutableListOf<RkfTripLeg>()
            var checkout: RkfTransaction? = null
            for ((index, transaction) in mTransactions.withIndex()) {
                // Case 1: if we got the checkin, skip it in this cycle and handle it in next
                if (index == 0 && isCheckin(transaction))
                    continue
                val isLast = index == mTransactions.size - 1
                // Case 2: remember checkout but don't parse it here
                if (isLast && isCheckOut(transaction)) {
                    checkout = transaction
                    continue
                }
                val previous: RkfTransaction? = if (index == 0) null else mTransactions[index - 1]
                // Case 3: transfer without checkin transaction. Happens if checkin went out of the log.
                if (previous == null) {
                    legs.add(RkfTripLeg(startTimestamp = startTimestamp ?: continue, endTimestamp = transaction.timestamp,
                        startStation = startStation, endStation = transaction.station,
                        fare = fare, passengerCount = passengerCount, mode = mode,
                        isTransfer = false,
                        mShortAgencyName = getAgencyName(true), mAgencyName = getAgencyName(false)))
                    continue
                }
                // Case 4: pair of checkin and transfer
                if (index == 1 && isCheckin(previous)) {
                    legs.add(RkfTripLeg(startTimestamp = startTimestamp ?: continue, endTimestamp = transaction.timestamp,
                        startStation = startStation, endStation = transaction.station,
                        fare = fare, passengerCount = passengerCount, mode = previous.mode,
                        isTransfer = false,
                        mShortAgencyName = previous.shortAgencyName, mAgencyName = previous.agencyName))
                    continue
                }
                // Case 5: pair of transfer and transfer
                legs.add(RkfTripLeg(startTimestamp = previous.timestamp ?: continue, endTimestamp = transaction.timestamp,
                    startStation = previous.station, endStation = transaction.station,
                    fare = null, passengerCount = passengerCount, mode = previous.mode,
                    isTransfer = true,
                    mShortAgencyName = previous.shortAgencyName, mAgencyName = previous.agencyName))
            }
            val previousIdx = mTransactions.size - 1 - (if (checkout == null) 0 else 1)
            val previous = if (previousIdx >= 0) mTransactions[previousIdx] else null
            // Case 6: pair of transfer and checkout or checkout missing
            if (previous != null && !isCheckin(previous)) {
                legs.add(RkfTripLeg(startTimestamp = previous.timestamp ?: return legs, endTimestamp = if (checkoutCompleted) endTimestamp else null,
                    startStation = previous.station, endStation = endStation,
                    fare = null, passengerCount = passengerCount, mode = previous.mode,
                    isTransfer = true,
                    mShortAgencyName = previous.shortAgencyName, mAgencyName = previous.agencyName))
            } else {
                // No usable data in TCEL. Happens e.g. on SLAccess which has no TCEL or if there were no transfers
                legs.add(RkfTripLeg(startTimestamp = startTimestamp ?: return legs, endTimestamp = if (checkoutCompleted) endTimestamp else null,
                    startStation = startStation, endStation = endStation,
                    fare = fare, passengerCount = passengerCount, mode = mode,
                    isTransfer = false,
                    mShortAgencyName = getAgencyName(true), mAgencyName = getAgencyName(false)))
            }
            return legs
        }

    private fun isCheckOut(transaction: RkfTransaction) = (
        transaction.isTapOff && checkoutCompleted
            && RkfTransitInfo.clearSeconds(transaction.timestamp?.toEpochMilliseconds() ?: 0) == RkfTransitInfo.clearSeconds(endTimestamp?.toEpochMilliseconds() ?: 0))

    private fun isCheckin(transaction: RkfTransaction) = (transaction.isTapOn
        && RkfTransitInfo.clearSeconds(transaction.timestamp?.toEpochMilliseconds() ?: 0) == RkfTransitInfo.clearSeconds(startTimestamp?.toEpochMilliseconds() ?: 0))

    companion object {
        private fun parseDateTime(value: Int, timeZone: TimeZone): Instant? {
            if (value == 0) return null
            // RKF stores minutes since 2000-01-01 00:00 in local time
            val baseLocal = LocalDateTime(2000, 1, 1, 0, 0, 0)
            val baseInstant = baseLocal.toInstant(timeZone)
            return baseInstant + value.toLong().minutes
        }

        private const val PRICE = "Price"
        private const val START_TIME = "JourneyOriginDateTime"
        private const val DESTINATION_TIME = "JourneyDestinationTime"
        private const val START_AID = "JourneyOriginAID"
        private const val START_PLACE = "JourneyOriginPlace"
        private const val DESTINATION_AID = "JourneyDestinationAID"
        private const val DESTINATION_PLACE = "JourneyDestinationPlace"
        private const val VALIDATION_STATUS = "ValidationStatus"
        private const val VALIDATION_MODEL = "ValidationModel"
        private const val DEPOSIT = "Deposit"
        private fun passengerTotal(num: Int) = "PassengerTotal$num"
        private fun passengerSubGroup(num: Int) = En1545Container(
            En1545FixedInteger("PassengerType$num", 8),
            En1545FixedInteger(passengerTotal(num), 6)
        )

        private val FIELDS = mapOf(
            // From documentation
            1 to En1545Container(
                RkfTransitInfo.HEADER,
                En1545FixedInteger("PIX", 12),
                En1545FixedInteger("Status", 8),
                En1545FixedInteger("PassengerClass", 2),
                passengerSubGroup(1),
                passengerSubGroup(2),
                passengerSubGroup(3),
                En1545FixedInteger(VALIDATION_MODEL, 2),
                En1545FixedInteger(VALIDATION_STATUS, 2),
                En1545FixedInteger("ValidationLevel", 2),
                En1545FixedInteger(PRICE, 20),
                En1545FixedInteger("PriceModificationLevel", 6),
                En1545FixedInteger(START_AID, 12),
                En1545FixedInteger(START_PLACE, 14),
                En1545FixedInteger(START_TIME, 24),
                En1545FixedInteger("JourneyFurthestAID", 12),
                En1545FixedInteger("JourneyFurthestPlace", 14),
                En1545FixedInteger("FurthestTime", 10),
                En1545FixedInteger(DESTINATION_AID, 12),
                En1545FixedInteger(DESTINATION_PLACE, 14),
                En1545FixedInteger(DESTINATION_TIME, 10),
                En1545FixedInteger("SupplementStatus", 2),
                En1545FixedInteger("SupplementType", 6),
                En1545FixedInteger("SupplementOriginAID", 12),
                En1545FixedInteger("SupplementOriginPlace", 14),
                En1545FixedInteger("SupplementDistance", 12),
                En1545FixedInteger("LatestControlAID", 12),
                En1545FixedInteger("LatestControlPlace", 14),
                En1545FixedInteger("LatestControlTime", 10),
                En1545FixedHex("Free", 34),
                RkfTransitInfo.MAC
            ),
            // Reverse engineered from SLaccess
            2 to En1545Container(
                RkfTransitInfo.HEADER, // confirmed
                En1545FixedInteger("PIX", 12),
                En1545FixedInteger("Status", 8),
                En1545FixedInteger("PassengerClass", 2),
                passengerSubGroup(1),
                passengerSubGroup(2),
                passengerSubGroup(3),
                En1545FixedInteger(VALIDATION_MODEL, 2), // confirmed
                En1545FixedInteger(VALIDATION_STATUS, 2), // confirmed
                En1545FixedInteger("ValidationLevel", 2),
                En1545FixedInteger(PRICE, 20), // confirmed
                En1545FixedInteger("A", 10), // always 0x200
                En1545FixedInteger(START_AID, 12), // confirmed
                En1545FixedInteger(START_PLACE, 14),
                En1545FixedInteger(START_TIME, 24), // confirmed
                En1545FixedInteger("JourneyFurthestAID", 12), // confirmed
                En1545FixedInteger("JourneyFurthestPlace", 14),
                En1545FixedInteger("FurthestTime", 10), // confirmed
                En1545FixedInteger(DESTINATION_AID, 12), // confirmed
                En1545FixedInteger(DESTINATION_PLACE, 14),
                En1545FixedHex("B", 44),
                En1545FixedInteger("LatestControlAID", 12), // confirmed
                En1545FixedInteger("LatestControlPlace", 14),
                En1545FixedInteger("LatestControlTime", 10),
                En1545FixedHex("C", 34), // always zero
                En1545FixedInteger("D", 8), // always 0x50
                RkfTransitInfo.MAC
            ),
            // Reverse-engineered from Rejsekort
            5 to En1545Container(
                RkfTransitInfo.HEADER, //confirmed
                // 26
                En1545FixedInteger("A", 16), // Always a000
                En1545FixedInteger("PassengerClass", 2),
                passengerSubGroup(1),
                passengerSubGroup(2),
                passengerSubGroup(3),
                En1545FixedInteger("B", 1), // Always zero
                En1545FixedInteger(VALIDATION_MODEL, 2), // confirmed
                En1545FixedInteger(VALIDATION_STATUS, 2), // confirmed
                En1545FixedInteger("ValidationLevel", 2),
                En1545FixedInteger(PRICE, 20), // confirmed
                // 113
                En1545FixedInteger(DEPOSIT, 20), //confirmed
                En1545FixedInteger("C", 19), // always 8c9
                En1545FixedInteger("SeqNo", 17),
                En1545FixedInteger(START_AID, 12), // confirmed
                En1545FixedInteger(START_PLACE, 14), // confirmed
                // 195
                En1545FixedInteger(START_TIME, 24), //confirmed
                En1545FixedInteger("JourneyFurthestAID", 12),
                En1545FixedInteger("JourneyFurthestPlace", 14),
                // 245
                En1545FixedInteger("D", 26), // always 0
                // 271
                En1545FixedInteger(DESTINATION_AID, 12), //confirmed
                En1545FixedInteger(DESTINATION_PLACE, 14), //confirmed
                En1545FixedInteger(DESTINATION_TIME, 10),
                //307
                // +181
                En1545FixedInteger("E", 16), // dynamic
                En1545FixedInteger("SupplementStatus", 2), // looks ok
                En1545FixedInteger("SupplementType", 6), // looks ok
                // On Rejsekort in Copenhagen : number of zones time 5
                En1545FixedInteger("SupplementDistance", 12),
                // 343
                En1545FixedInteger("F1", 4), // 1 on completed, 0 otherwise
                En1545FixedInteger("F2", 20), // always 0x80
                En1545FixedInteger("F3", 16), // always 0
                En1545FixedInteger("F4", 24), // dynamic
                En1545FixedInteger("F5", 24), // 480010 or 500000
                En1545FixedInteger("F6", 16), // always 0
                En1545FixedInteger("F7", 16), // always zero
                En1545FixedInteger("F8", 25), // always 0xa8

                // 488
                RkfTransitInfo.MAC,
                // 512
                En1545FixedHex("X", 256)
            )
        )

        fun parse(record: ByteArray, lookup: RkfLookup): RkfTCSTTrip? {
            val aid = record.getBitsFromBufferLeBits(14, 12)
            if (aid == 0)
                return null

            var version = record.getBitsFromBufferLeBits(8, 6)
            if (version < 1)
                version = 1
            // Stub
            if (version == 3 || version == 4)
                version = 2
            // Stub
            if (version > 5)
                version = 5
            return RkfTCSTTrip(En1545Parser.parseLeBits(record, FIELDS.getValue(version)), lookup)
        }
    }
}
