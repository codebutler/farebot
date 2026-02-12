/*
 * NextfareTransitInfo.kt
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

import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.Refill
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.nextfare.record.NextfareBalanceRecord
import com.codebutler.farebot.transit.nextfare.record.NextfareConfigRecord
import com.codebutler.farebot.transit.nextfare.record.NextfareRecord
import com.codebutler.farebot.transit.nextfare.record.NextfareTopupRecord
import com.codebutler.farebot.transit.nextfare.record.NextfareTransactionRecord
import com.codebutler.farebot.transit.nextfare.record.NextfareTravelPassRecord
import farebot.farebot_transit_nextfare.generated.resources.Res
import farebot.farebot_transit_nextfare.generated.resources.nextfare_card_name
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.getString

/**
 * Parsed data from a Nextfare card.
 */
data class NextfareTransitInfoCapsule(
    val config: NextfareConfigRecord?,
    val hasUnknownStations: Boolean,
    val serialNumber: Long,
    val systemCode: ByteArray,
    val block2: ByteArray,
    val balance: Int,
    val trips: List<Trip>,
    val refills: List<Refill>,
    val subscriptions: List<NextfareSubscription>
)

/**
 * Generic transit data type for Cubic Nextfare.
 * https://github.com/micolous/metrodroid/wiki/Cubic-Nextfare-MFC
 *
 * Subclass this for system-specific implementations (e.g. SeqGo, SmartRider).
 */
open class NextfareTransitInfo(
    val capsule: NextfareTransitInfoCapsule,
    private val currencyFactory: (Int) -> TransitCurrency = { TransitCurrency.XXX(it) }
) : TransitInfo() {

    override val balance: TransitBalance
        get() = TransitBalance(balance = currencyFactory(capsule.balance))

    override val serialNumber: String
        get() = formatSerialNumber(capsule.serialNumber)

    override val trips: List<Trip>
        get() = capsule.trips

    override val subscriptions: List<Subscription>
        get() = capsule.subscriptions

    override val cardName: String
        get() = runBlocking { getString(Res.string.nextfare_card_name) }

    override val hasUnknownStations: Boolean
        get() = capsule.hasUnknownStations

    companion object {
        const val NAME = "Nextfare"

        val MANUFACTURER = byteArrayOf(
            0x16, 0x18, 0x1A, 0x1B,
            0x1C, 0x1D, 0x1E, 0x1F
        )

        /**
         * Format a Nextfare serial number with the standard 0160 prefix and Luhn check digit.
         */
        fun formatSerialNumber(serialNumber: Long): String {
            val digits = serialNumber.toString().padStart(11, '0')
            val raw = "0160$digits"
            val spaced = buildString {
                append("0160 ")
                for (i in digits.indices) {
                    append(digits[i])
                    if (i == 3 || i == 7) append(' ')
                }
            }
            val luhn = calculateLuhn(raw)
            return "$spaced$luhn"
        }

        private fun calculateLuhn(input: String): Int {
            var sum = 0
            var alternate = true
            for (i in input.length - 1 downTo 0) {
                var n = input[i] - '0'
                if (alternate) {
                    n *= 2
                    if (n > 9) n -= 9
                }
                sum += n
                alternate = !alternate
            }
            return (10 - (sum % 10)) % 10
        }

        /**
         * Check if two transaction records should be merged into a single trip
         * (tap-on + tap-off in the same journey).
         */
        private fun tapsMergeable(
            tap1: NextfareTransactionRecord,
            tap2: NextfareTransactionRecord
        ): Boolean {
            return when {
                tap1.type.isSale || tap2.type.isSale -> false
                else -> tap1.journey == tap2.journey && tap1.mode == tap2.mode
            }
        }

        /**
         * Core Nextfare card parsing logic. Parses a ClassicCard into a NextfareTransitInfoCapsule.
         *
         * @param card The ClassicCard to parse
         * @param timeZone TimeZone for date parsing
         * @param newTrip Factory for creating Trip objects from capsules
         * @param newRefill Factory for creating Refill objects from top-up records
         * @param shouldMergeJourneys Whether to merge tap-on/tap-off pairs into single trips
         */
        fun parse(
            card: ClassicCard,
            timeZone: TimeZone,
            newTrip: (NextfareTripCapsule) -> Trip = { NextfareTrip(it) },
            newRefill: (NextfareTopupRecord) -> Refill = { NextfareRefill(it) },
            shouldMergeJourneys: Boolean = true
        ): NextfareTransitInfoCapsule {
            val sector0 = card.getSector(0) as DataClassicSector
            val serialData = sector0.getBlock(0).data
            val serialNumber = NextfareRecord.byteArrayToLongReversed(serialData, 0, 4)

            val magicData = sector0.getBlock(1).data
            val systemCode = magicData.copyOfRange(9, 15)
            val block2 = sector0.getBlock(2).data

            // Parse all data blocks (skip sector 0 preamble and block 3 keys/ACL)
            val records = mutableListOf<NextfareRecord>()
            for ((secIdx, sector) in card.sectors.withIndex()) {
                if (secIdx == 0) continue
                if (sector !is DataClassicSector) continue
                for ((blockIdx, block) in sector.blocks.withIndex()) {
                    if (blockIdx >= 3) continue // Skip trailer blocks
                    val record = NextfareRecord.recordFromBytes(
                        block.data, secIdx, blockIdx, timeZone
                    )
                    if (record != null) {
                        records.add(record)
                    }
                }
            }

            // Sort and extract record types
            val balances = records.filterIsInstance<NextfareBalanceRecord>().sorted()
            val taps = records.filterIsInstance<NextfareTransactionRecord>().sorted()
            val passes = records.filterIsInstance<NextfareTravelPassRecord>().sorted()
            val config = records.filterIsInstance<NextfareConfigRecord>().lastOrNull()

            val trips = mutableListOf<Trip>()
            val refills = mutableListOf<Refill>()
            val subscriptions = mutableListOf<NextfareSubscription>()

            // Add refills from top-up records
            refills += records.filterIsInstance<NextfareTopupRecord>().map { newRefill(it) }

            // Determine balance
            val balance: Int = if (balances.isNotEmpty()) {
                var best = balances[0]
                if (balances.size == 2) {
                    // If the version number overflowed, swap them
                    if (balances[0].version >= 240 && balances[1].version <= 10) {
                        best = balances[1]
                    }
                }
                if (best.hasTravelPassAvailable) {
                    subscriptions.add(NextfareSubscription(best))
                }
                best.balance
            } else {
                0
            }

            // Build trips from transaction records
            if (taps.isNotEmpty()) {
                var i = 0
                while (i < taps.size) {
                    val tapOn = taps[i]

                    val trip = NextfareTripCapsule(
                        journeyId = tapOn.journey,
                        startTimestamp = tapOn.timestamp,
                        startStation = tapOn.station,
                        modeInt = tapOn.mode,
                        isTransfer = tapOn.isContinuation,
                        cost = -tapOn.value
                    )

                    // Check if next record is a tap-off for this journey
                    if (shouldMergeJourneys && i + 1 < taps.size && tapsMergeable(tapOn, taps[i + 1])) {
                        val tapOff = taps[i + 1]
                        trip.endTimestamp = tapOff.timestamp
                        trip.endStation = tapOff.station
                        trip.cost -= tapOff.value
                        i++
                    }

                    trips.add(newTrip(trip))
                    i++
                }

                trips.sortWith(Trip.Comparator())
                trips.reverse()
            }

            val hasUnknownStations = trips.any {
                it.startStation == null || it.endStation == null
            }

            if (passes.isNotEmpty()) {
                subscriptions.add(NextfareSubscription(passes[0]))
            }

            return NextfareTransitInfoCapsule(
                config = config,
                hasUnknownStations = hasUnknownStations,
                serialNumber = serialNumber,
                systemCode = systemCode,
                block2 = block2,
                balance = balance,
                trips = trips,
                refills = refills,
                subscriptions = subscriptions
            )
        }
    }

    /**
     * Fallback factory for unrecognized Nextfare cards.
     */
    open class NextfareTransitFactory : TransitFactory<ClassicCard, NextfareTransitInfo> {

        override val allCards: List<CardInfo> = emptyList()

        override fun check(card: ClassicCard): Boolean {
            val sector0 = card.getSector(0)
            if (sector0 !is DataClassicSector) return false
            val blockData = sector0.getBlock(1).data
            if (blockData.size < MANUFACTURER.size + 1) return false
            return blockData.copyOfRange(1, MANUFACTURER.size + 1)
                .contentEquals(MANUFACTURER)
        }

        override fun parseIdentity(card: ClassicCard): TransitIdentity {
            val serialData = (card.getSector(0) as DataClassicSector).getBlock(0).data
            val serialNumber = NextfareRecord.byteArrayToLongReversed(serialData, 0, 4)
            val cardName = runBlocking { getString(Res.string.nextfare_card_name) }
            return TransitIdentity.create(cardName, formatSerialNumber(serialNumber))
        }

        override fun parseInfo(card: ClassicCard): NextfareTransitInfo {
            val capsule = parse(card = card, timeZone = TimeZone.UTC)
            return NextfareTransitInfo(capsule)
        }
    }
}
