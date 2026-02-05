/*
 * ManlyFastFerryTransitFactory.kt
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

package com.codebutler.farebot.transit.manly_fast_ferry

import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.Refill
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.manly_fast_ferry.record.ManlyFastFerryBalanceRecord
import com.codebutler.farebot.transit.manly_fast_ferry.record.ManlyFastFerryMetadataRecord
import com.codebutler.farebot.transit.manly_fast_ferry.record.ManlyFastFerryPreambleRecord
import com.codebutler.farebot.transit.manly_fast_ferry.record.ManlyFastFerryPurseRecord
import com.codebutler.farebot.transit.manly_fast_ferry.record.ManlyFastFerryRecord
import com.codebutler.farebot.transit.manly_fast_ferry.record.ManlyFastFerryRegularRecord
import farebot.farebot_transit_manly.generated.resources.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import kotlin.time.Instant

class ManlyFastFerryTransitFactory : TransitFactory<ClassicCard, ManlyFastFerryTransitInfo> {

    companion object {
        val SIGNATURE = byteArrayOf(
            0x32, 0x32, 0x00, 0x00, 0x00, 0x01, 0x01
        )
    }

    override fun check(card: ClassicCard): Boolean {
        // TODO: Improve this check
        // The card contains two copies of the card's serial number on the card.
        // Lets use this for now to check that this is a Manly Fast Ferry card.

        if (card.getSector(0) !is DataClassicSector) {
            // These blocks of the card are not protected.
            // This must not be a Manly Fast Ferry smartcard.
            return false
        }

        val file1 = (card.getSector(0) as DataClassicSector).getBlock(1).data
        //file2 = card.getSector(0).getBlock(2).bytes()

        // Serial number is from byte 10 in file 1 and byte 7 of file 2, for 4 bytes.
        // DISABLED: This check fails on 2012-era cards.
        //if (!Arrays.equals(Arrays.copyOfRange(file1, 10, 14), Arrays.copyOfRange(file2, 7, 11))) {
        //    return false
        //}

        // Check a signature
        return file1.copyOfRange(0, SIGNATURE.size).contentEquals(SIGNATURE)
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity {
        val file2 = (card.getSector(0) as DataClassicSector).getBlock(2).data
        val metadata = recordFromBytes(file2)
        if (metadata !is ManlyFastFerryMetadataRecord) {
            throw AssertionError("Unexpected Manly record type: " + metadata!!::class.simpleName)
        }
        return TransitIdentity.create(
            runBlocking { getString(Res.string.manly_card_name) },
            metadata.cardSerial
        )
    }

    override fun parseInfo(card: ClassicCard): ManlyFastFerryTransitInfo {
        val records = mutableListOf<ManlyFastFerryRecord>()

        // Iterate through blocks on the card and deserialize all the binary data.
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

                val record = recordFromBytes(block.data)
                if (record != null) {
                    records.add(record)
                }
            }
        }

        // Now do a first pass for metadata and balance information.
        val balances = mutableListOf<ManlyFastFerryBalanceRecord>()

        var serialNumber: String? = null
        var epochDate: Instant? = null

        for (record in records) {
            when (record) {
                is ManlyFastFerryMetadataRecord -> {
                    serialNumber = record.cardSerial
                    epochDate = record.epochDate
                }
                is ManlyFastFerryBalanceRecord -> {
                    balances.add(record)
                }
            }
        }

        var balance = 0

        if (balances.size >= 1) {
            balances.sort()
            balance = balances[0].balance
        }

        // Now generate a transaction list.
        // These need the Epoch to be known first.
        val trips = mutableListOf<Trip>()
        val refills = mutableListOf<Refill>()

        for (record in records) {
            if (record is ManlyFastFerryPurseRecord) {
                // Now convert this.
                if (record.isCredit) {
                    // Credit
                    refills.add(ManlyFastFerryRefill.create(record, epochDate!!))
                } else {
                    // Debit
                    trips.add(ManlyFastFerryTrip.create(record, epochDate!!))
                }
            }
        }

        trips.sortWith(Trip.Comparator())
        refills.sortWith(Refill.Comparator())

        return ManlyFastFerryTransitInfo.create(serialNumber!!, trips, epochDate!!, balance)
    }

    private fun recordFromBytes(input: ByteArray): ManlyFastFerryRecord? {
        var record: ManlyFastFerryRecord? = null
        when (input[0]) {
            0x01.toByte() -> {
                // Check if the next bytes are null
                if (input[1] == 0x00.toByte() || input[1] == 0x01.toByte()) {
                    if (input[2] != 0x00.toByte()) {
                        // Fork off to handle balance
                        record = ManlyFastFerryBalanceRecord.recordFromBytes(input)
                    }
                }
            }

            0x02.toByte() -> {
                // Regular record
                record = ManlyFastFerryRegularRecord.recordFromBytes(input)
            }

            0x32.toByte() -> {
                // Preamble record
                record = ManlyFastFerryPreambleRecord.recordFromBytes(input)
            }

            0x00.toByte(), 0x06.toByte() -> {
                // Null record / ignorable record
            }
            else -> {
                // Unknown record type
            }
        }

        return record
    }
}
