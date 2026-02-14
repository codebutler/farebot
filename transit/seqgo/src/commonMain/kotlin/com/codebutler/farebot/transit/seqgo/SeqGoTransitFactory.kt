/*
 * SeqGoTransitFactory.kt
 *
 * Copyright 2015 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.seqgo

import com.codebutler.farebot.base.util.ByteUtils
import com.codebutler.farebot.base.util.Luhn
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.Refill
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.seqgo.record.SeqGoBalanceRecord
import com.codebutler.farebot.transit.seqgo.record.SeqGoRecord
import com.codebutler.farebot.transit.seqgo.record.SeqGoTapRecord
import com.codebutler.farebot.transit.seqgo.record.SeqGoTopupRecord
import farebot.transit.seqgo.generated.resources.*

class SeqGoTransitFactory : TransitFactory<ClassicCard, SeqGoTransitInfo> {
    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override fun check(card: ClassicCard): Boolean {
        if (card.getSector(0) is DataClassicSector) {
            val blockData = (card.getSector(0) as DataClassicSector).getBlock(1).data
            if (!blockData.copyOfRange(1, 9).contentEquals(MANUFACTURER)) {
                return false
            }
            // Also check the system code to distinguish from other Nextfare-based cards
            val systemCode = blockData.copyOfRange(9, 15)
            return systemCode.contentEquals(SYSTEM_CODE1) || systemCode.contentEquals(SYSTEM_CODE2)
        }
        return false
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity {
        var serialData = (card.getSector(0) as DataClassicSector).getBlock(0).data
        serialData = ByteUtils.reverseBuffer(serialData, 0, 4)
        val serialNumber = bytesToLong(serialData.copyOfRange(0, 4))
        return TransitIdentity.create(SeqGoTransitInfo.NAME, formatSerialNumber(serialNumber))
    }

    override fun parseInfo(card: ClassicCard): SeqGoTransitInfo {
        var serialData = (card.getSector(0) as DataClassicSector).getBlock(0).data
        serialData = ByteUtils.reverseBuffer(serialData, 0, 4)
        val serialNumber = bytesToLong(serialData.copyOfRange(0, 4))

        val records = mutableListOf<SeqGoRecord>()

        for (sector in card.sectors) {
            if (sector !is DataClassicSector) {
                continue
            }
            for (block in sector.blocks) {
                if (sector.index == 0 && block.index == 0) {
                    continue
                }
                if (block.index == 3) {
                    continue
                }
                val record = SeqGoRecord.recordFromBytes(block.data)
                if (record != null) {
                    records.add(record)
                }
            }
        }

        val balances = mutableListOf<SeqGoBalanceRecord>()
        val trips = mutableListOf<SeqGoTrip>()
        val refills = mutableListOf<SeqGoRefill>()
        val taps = mutableListOf<SeqGoTapRecord>()

        for (record in records) {
            when (record) {
                is SeqGoBalanceRecord -> balances.add(record)
                is SeqGoTopupRecord -> refills.add(SeqGoRefill.create(record))
                is SeqGoTapRecord -> taps.add(record)
            }
        }

        var balance = 0
        if (balances.size >= 1) {
            val sorted = balances.sortedDescending()
            balance = sorted[0].balance
        }

        if (taps.size >= 1) {
            val sortedTaps = taps.sorted()

            var i = 0
            while (sortedTaps.size > i) {
                val tapOn = sortedTaps[i]
                val tripBuilder = SeqGoTrip.builder()

                tripBuilder.journeyId(tapOn.journey)
                tripBuilder.startTime(tapOn.timestamp)
                tripBuilder.startStationId(tapOn.station)
                tripBuilder.startStation(SeqGoUtil.getStation(tapOn.station))
                tripBuilder.mode(tapOn.mode)

                if (sortedTaps.size > i + 1 &&
                    sortedTaps[i + 1].journey == tapOn.journey &&
                    sortedTaps[i + 1].mode == tapOn.mode
                ) {
                    val tapOff = sortedTaps[i + 1]
                    tripBuilder.endTime(tapOff.timestamp)
                    tripBuilder.endStationId(tapOff.station)
                    tripBuilder.endStation(SeqGoUtil.getStation(tapOff.station))
                    i++
                }

                trips.add(tripBuilder.build())
                i++
            }

            trips.sortWith(Trip.Comparator())
        }

        var hasUnknownStations = false
        for (trip in trips) {
            if (trip.startStation == null || (trip.endTimestamp != null && trip.endStation == null)) {
                hasUnknownStations = true
            }
        }

        if (refills.size > 1) {
            refills.sortWith(Refill.Comparator())
        }

        return SeqGoTransitInfo.create(
            formatSerialNumber(serialNumber),
            trips.toList<Trip>(),
            refills.toList<Refill>(),
            hasUnknownStations,
            balance,
        )
    }

    companion object {
        private val CARD_INFO =
            CardInfo(
                nameRes = Res.string.seqgo_card_name,
                cardType = CardType.MifareClassic,
                region = TransitRegion.AUSTRALIA,
                locationRes = Res.string.seqgo_location,
                imageRes = Res.drawable.seqgo_card,
                latitude = -27.4698f,
                longitude = 153.0251f,
                brandColor = 0x00427B,
                credits = listOf("Michael Farrell"),
                keysRequired = true,
                sampleDumpFile = "SeqGo.json",
            )

        private val MANUFACTURER =
            byteArrayOf(
                0x16,
                0x18,
                0x1A,
                0x1B,
                0x1C,
                0x1D,
                0x1E,
                0x1F,
            )

        private val SYSTEM_CODE1 =
            byteArrayOf(
                0x5A,
                0x5B,
                0x20,
                0x21,
                0x22,
                0x23,
            )

        private val SYSTEM_CODE2 =
            byteArrayOf(
                0x20,
                0x21,
                0x22,
                0x23,
                0x01,
                0x01,
            )

        /**
         * Convert up to 4 bytes to a Long, treating as unsigned big-endian.
         */
        private fun bytesToLong(bytes: ByteArray): Long {
            var result = 0L
            for (b in bytes) {
                result = (result shl 8) or (b.toLong() and 0xFF)
            }
            return result
        }

        private fun formatSerialNumber(serialNumber: Long): String {
            var serial = serialNumber.toString().padStart(12, '0')
            serial = "016$serial"
            val fullSerial = serial + Luhn.calculateLuhn(serial)
            // Format as "0160 0012 3456 7893" with spaces every 4 digits
            return fullSerial.chunked(4).joinToString(" ")
        }
    }
}
