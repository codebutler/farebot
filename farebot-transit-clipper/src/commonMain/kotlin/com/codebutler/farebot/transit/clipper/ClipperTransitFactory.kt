/*
 * ClipperTransitFactory.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014-2016 Eric Butler <eric@codebutler.com>
 *
 * Thanks to:
 * An anonymous contributor for reverse engineering Clipper data and providing
 * most of the code here.
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

import com.codebutler.farebot.base.util.ByteUtils
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.desfire.DesfireCard
import com.codebutler.farebot.card.desfire.StandardDesfireFile
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import com.codebutler.farebot.transit.Trip
import farebot.farebot_transit_clipper.generated.resources.*
import kotlin.time.Instant

class ClipperTransitFactory : TransitFactory<DesfireCard, ClipperTransitInfo> {

    companion object {
        private const val RECORD_LENGTH = 32

        // Seconds per day for converting day-based expiry to timestamp
        private const val SECONDS_PER_DAY = 86400L

        private val CARD_INFO = CardInfo(
            nameRes = Res.string.transit_clipper_card_name,
            cardType = CardType.MifareDesfire,
            region = TransitRegion.USA,
            locationRes = Res.string.location_san_francisco,
            imageRes = Res.drawable.clipper_card,
            latitude = 37.7749f,
            longitude = -122.4194f,
            brandColor = 0x274986,
            credits = listOf("Anonymous Contributor", "Bao-Long Nguyen-Trong", "Michael Farrell"),
            sampleDumpFile = "Clipper.nfc",
        )
    }

    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override fun check(card: DesfireCard): Boolean {
        return card.getApplication(0x9011f2) != null
    }

    override fun parseIdentity(card: DesfireCard): TransitIdentity {
        try {
            val file = card.getApplication(0x9011f2)!!.getFile(0x08) as? StandardDesfireFile
                ?: throw RuntimeException("Clipper file 0x08 is not readable")
            val cardName = getStringBlocking(Res.string.transit_clipper_card_name)
            return TransitIdentity.create(cardName, ByteUtils.byteArrayToLong(file.data, 1, 4).toString())
        } catch (ex: Exception) {
            throw RuntimeException("Error parsing Clipper serial", ex)
        }
    }

    override fun parseInfo(card: DesfireCard): ClipperTransitInfo {
        try {
            val serialFile = card.getApplication(0x9011f2)!!.getFile(0x08) as? StandardDesfireFile
                ?: throw RuntimeException("Clipper file 0x08 is not readable")
            val serialNumber = ByteUtils.byteArrayToLong(serialFile.data, 1, 4)

            val balanceFile = card.getApplication(0x9011f2)!!.getFile(0x02) as? StandardDesfireFile
                ?: throw RuntimeException("Clipper file 0x02 is not readable")
            var data = balanceFile.data
            // Read as unsigned, then convert to Short for sign extension, then to Int
            // This handles negative balances correctly
            val balance = ((0xFF and data[18].toInt()) shl 8 or (0xFF and data[19].toInt())).toShort().toInt()

            // Read expiry date from file 0x01, offset 8 (2 bytes)
            // The expiry value is stored as days since Clipper epoch
            val expiryTimestamp = try {
                val expiryData = (card.getApplication(0x9011f2)!!.getFile(0x01) as? StandardDesfireFile)?.data
                if (expiryData != null && expiryData.size > 9) {
                    val expiryDays = ByteUtils.byteArrayToInt(expiryData, 8, 2)
                    if (expiryDays > 0) {
                        // Convert days since Clipper epoch to Unix timestamp
                        clipperTimestampToInstant(expiryDays.toLong() * SECONDS_PER_DAY)
                    } else null
                } else null
            } catch (e: Exception) {
                null // Expiry date not available
            }

            val refills = parseRefills(card)
            val rawTrips = parseTrips(card)
            val tripsWithBalances = computeBalances(balance.toLong(), rawTrips, refills)

            // Combine trips and refills into a single list, then sort by timestamp (newest first)
            val allTrips: List<Trip> = (tripsWithBalances + refills).sortedWith(Trip.Comparator())

            return ClipperTransitInfo.create(
                serialNumber.toString(),
                allTrips,
                balance,
                expiryTimestamp
            )
        } catch (ex: Exception) {
            throw RuntimeException("Error parsing Clipper data", ex)
        }
    }

    /**
     * Convert a Clipper timestamp (seconds since 1900-01-01) to a kotlin.time.Instant.
     */
    private fun clipperTimestampToInstant(clipperSeconds: Long): Instant? {
        if (clipperSeconds == 0L) return null
        val unixSeconds = ClipperUtil.clipperTimestampToEpochSeconds(clipperSeconds)
        return Instant.fromEpochSeconds(unixSeconds)
    }

    private fun computeBalances(
        balance: Long,
        trips: List<ClipperTrip>,
        refills: List<ClipperRefill>
    ): List<ClipperTrip> {
        var currentBalance = balance
        val tripsWithBalance = MutableList<ClipperTrip?>(trips.size) { null }
        var tripIdx = 0
        var refillIdx = 0
        while (tripIdx < trips.size) {
            while (refillIdx < refills.size) {
                val refillTimestamp = refills[refillIdx].startTimestamp?.epochSeconds ?: 0L
                val tripTimestamp = trips[tripIdx].startTimestamp.epochSeconds
                if (refillTimestamp > tripTimestamp) {
                    // Refill's fare is negative (money added), so subtracting it increases the balance
                    currentBalance -= refills[refillIdx].fare?.currency ?: 0
                    refillIdx++
                } else {
                    break
                }
            }
            tripsWithBalance[tripIdx] = trips[tripIdx].withBalance(currentBalance)
            currentBalance += trips[tripIdx].getFareValue()
            tripIdx++
        }
        @Suppress("UNCHECKED_CAST")
        return tripsWithBalance as List<ClipperTrip>
    }

    private fun parseTrips(card: DesfireCard): List<ClipperTrip> {
        val file = card.getApplication(0x9011f2)!!.getFile(0x0e) as? StandardDesfireFile
            ?: return emptyList()
        /*
         *  This file reads very much like a record file but it professes to
         *  be only a regular file.  As such, we'll need to extract the records
         *  manually.
         */
        val data = file.data
        var pos = data.size - RECORD_LENGTH
        val result = mutableListOf<ClipperTrip>()
        while (pos >= 0) {
            val slice = ByteUtils.byteArraySlice(data, pos, RECORD_LENGTH)
            val trip = createTrip(slice)
            if (trip != null) {
                // Some transaction types are temporary -- remove previous trip with the same timestamp.
                val existingTrip = result.firstOrNull { it.startTimestamp == trip.startTimestamp }
                if (existingTrip != null) {
                    if (existingTrip.endTimestamp != null) {
                        // Old trip has exit timestamp, and is therefore better.
                        pos -= RECORD_LENGTH
                        continue
                    } else {
                        result.remove(existingTrip)
                    }
                }
                result.add(trip)
            }
            pos -= RECORD_LENGTH
        }

        result.sortWith(Trip.Comparator())

        return result
    }

    private fun createTrip(useData: ByteArray): ClipperTrip? {
        // Convert Clipper timestamps (seconds since 1900) to Unix timestamps
        val timestamp = ClipperUtil.clipperTimestampToEpochSeconds(ByteUtils.byteArrayToLong(useData, 0xc, 4))
        val exitTimestamp = ClipperUtil.clipperTimestampToEpochSeconds(ByteUtils.byteArrayToLong(useData, 0x10, 4))
        val fare = ByteUtils.byteArrayToLong(useData, 0x6, 2)
        val agency = ByteUtils.byteArrayToLong(useData, 0x2, 2)
        val from = ByteUtils.byteArrayToLong(useData, 0x14, 2)
        val to = ByteUtils.byteArrayToLong(useData, 0x16, 2)
        val route = ByteUtils.byteArrayToLong(useData, 0x1c, 2)
        val vehicleNum = ByteUtils.byteArrayToLong(useData, 0xa, 2)
        val transportCode = ByteUtils.byteArrayToLong(useData, 0x1e, 2)

        if (agency == 0L) {
            return null
        }

        return ClipperTrip.builder()
            .timestamp(timestamp)
            .exitTimestamp(exitTimestamp)
            .fare(fare)
            .agency(agency)
            .from(from)
            .to(to)
            .route(route)
            .vehicleNum(vehicleNum)
            .transportCode(transportCode)
            .balance(0) // Filled in later
            .build()
    }

    private fun parseRefills(card: DesfireCard): List<ClipperRefill> {
        val file = card.getApplication(0x9011f2)!!.getFile(0x04) as? StandardDesfireFile
            ?: return emptyList()

        /*
         *  This file reads very much like a record file but it professes to
         *  be only a regular file.  As such, we'll need to extract the records
         *  manually.
         */
        val data = file.data
        var pos = data.size - RECORD_LENGTH
        val result = mutableListOf<ClipperRefill>()
        while (pos >= 0) {
            val slice = ByteUtils.byteArraySlice(data, pos, RECORD_LENGTH)
            val refill = createRefill(slice)
            if (refill != null) {
                result.add(refill)
            }
            pos -= RECORD_LENGTH
        }
        result.sortWith(Trip.Comparator())

        return result
    }

    private fun createRefill(useData: ByteArray): ClipperRefill? {
        val timestamp = ByteUtils.byteArrayToLong(useData, 0x4, 4)
        val agency = ByteUtils.byteArrayToLong(useData, 0x2, 2)
        val machineid = ByteUtils.byteArrayToLong(useData, 0x8, 4)
        val amount = ByteUtils.byteArrayToLong(useData, 0xe, 2)
        if (timestamp == 0L) {
            return null
        }
        return ClipperRefill.create(ClipperUtil.clipperTimestampToEpochSeconds(timestamp), amount, agency, machineid)
    }
}
