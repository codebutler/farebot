/*
 * ErgTransitInfo.kt
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

package com.codebutler.farebot.transit.erg

import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.Refill
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.erg.record.ErgBalanceRecord
import com.codebutler.farebot.transit.erg.record.ErgIndexRecord
import com.codebutler.farebot.transit.erg.record.ErgMetadataRecord
import com.codebutler.farebot.transit.erg.record.ErgPreambleRecord
import com.codebutler.farebot.transit.erg.record.ErgPurseRecord
import com.codebutler.farebot.transit.erg.record.ErgRecord
import farebot.farebot_transit_erg.generated.resources.Res
import farebot.farebot_transit_erg.generated.resources.erg_card_name
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

/**
 * Parsed data from an ERG card.
 */
data class ErgTransitInfoCapsule(
    val cardSerial: ByteArray?,
    val epochDate: Int,
    val agencyId: Int,
    val balance: Int,
    val trips: List<Trip>,
    val refills: List<Refill>
)

/**
 * Transit data type for ERG/Videlli/Vix MIFARE Classic cards.
 *
 * Wiki: https://github.com/micolous/metrodroid/wiki/ERG-MFC
 *
 * Subclass this for system-specific implementations (e.g. Manly Fast Ferry, ChC Metrocard).
 */
open class ErgTransitInfo(
    val capsule: ErgTransitInfoCapsule,
    private val currencyFactory: (Int) -> TransitCurrency = { TransitCurrency.XXX(it) }
) : TransitInfo() {

    override val balance: TransitBalance
        get() = TransitBalance(balance = currencyFactory(capsule.balance))

    override val serialNumber: String?
        get() = capsule.cardSerial?.joinToString("") {
            (it.toInt() and 0xFF).toString(16).padStart(2, '0')
        }?.uppercase()

    override val trips: List<Trip> get() = capsule.trips

    override val cardName: String = runBlocking { getString(Res.string.erg_card_name) }

    companion object {
        val NAME: String get() = runBlocking { getString(Res.string.erg_card_name) }

        val SIGNATURE = byteArrayOf(0x32, 0x32, 0x00, 0x00, 0x00, 0x01, 0x01)

        /**
         * Read the metadata record from sector 0 block 2.
         */
        fun getMetadataRecord(card: ClassicCard): ErgMetadataRecord? {
            val sector0 = card.getSector(0)
            if (sector0 !is DataClassicSector) return null
            return try {
                ErgMetadataRecord.recordFromBytes(sector0.getBlock(2).data)
            } catch (_: Exception) {
                null
            }
        }

        /**
         * Core ERG card parsing logic.
         *
         * @param card The ClassicCard to parse
         * @param newTrip Factory for creating Trip objects from purse records
         * @param newRefill Factory for creating Refill objects from purse credit records
         */
        fun parse(
            card: ClassicCard,
            newTrip: (ErgPurseRecord, Int) -> Trip = { purse, epoch -> ErgTrip(purse, epoch) },
            newRefill: (ErgPurseRecord, Int) -> Refill = { purse, epoch -> ErgRefill(purse, epoch) }
        ): ErgTransitInfoCapsule {
            val records = mutableListOf<ErgRecord>()

            // Read the index data from sectors 1 and 2
            val sector1 = card.getSector(1) as? DataClassicSector
            val sector2 = card.getSector(2) as? DataClassicSector

            val index1 = sector1?.let { ErgIndexRecord.recordFromSector(it) }
            val index2 = sector2?.let { ErgIndexRecord.recordFromSector(it) }

            val activeIndex = when {
                index1 != null && index2 != null ->
                    if (index1.version > index2.version) index1 else index2
                index1 != null -> index1
                index2 != null -> index2
                else -> null
            }

            val metadataRecord = getMetadataRecord(card)
                ?: throw IllegalArgumentException("No metadata record found")

            // Iterate through blocks on the card starting from sector 3
            for ((sectorNum, sector) in card.sectors.withIndex()) {
                if (sectorNum < 3) continue
                if (sector !is DataClassicSector) continue
                for ((blockNum, block) in sector.blocks.withIndex()) {
                    if (blockNum >= 3) continue // Skip trailer blocks
                    val record = activeIndex?.readRecord(sectorNum, blockNum, block.data) ?: continue
                    records.add(record)
                }
            }

            val epochDate = metadataRecord.epochDate

            // Split purse records into trips (debits) and refills (credits)
            val purseRecords = records.filterIsInstance<ErgPurseRecord>()
            val trips = purseRecords.filter { !it.isCredit }.map { newTrip(it, epochDate) }
            val refills = purseRecords.filter { it.isCredit }.map { newRefill(it, epochDate) }

            val balance = records.filterIsInstance<ErgBalanceRecord>()
                .sorted()
                .lastOrNull()
                ?.balance ?: 0

            return ErgTransitInfoCapsule(
                cardSerial = metadataRecord.cardSerial,
                trips = trips.sortedByDescending { it.startTimestamp },
                refills = refills.sortedByDescending { it.getTimestamp() },
                balance = balance,
                epochDate = epochDate,
                agencyId = metadataRecord.agencyId
            )
        }
    }

    /**
     * Fallback factory for unrecognized ERG cards.
     */
    open class ErgTransitFactory : TransitFactory<ClassicCard, ErgTransitInfo> {

        override val allCards: List<CardInfo> = emptyList()

        /**
         * Override to match a specific ERG agency ID. Return -1 to match any.
         */
        protected open val ergAgencyId: Int get() = -1

        override fun check(card: ClassicCard): Boolean {
            val sector0 = card.getSector(0)
            if (sector0 !is DataClassicSector) return false
            val file1 = sector0.getBlock(1).data
            if (file1.size < SIGNATURE.size) return false

            if (!file1.copyOfRange(0, SIGNATURE.size).contentEquals(SIGNATURE)) {
                return false
            }

            val agencyId = ergAgencyId
            return if (agencyId == -1) {
                true
            } else {
                val metadata = getMetadataRecord(card)
                metadata != null && metadata.agencyId == agencyId
            }
        }

        override fun parseIdentity(card: ClassicCard): TransitIdentity {
            val metadata = getMetadataRecord(card)
            val serial = metadata?.cardSerial?.joinToString("") {
                (it.toInt() and 0xFF).toString(16).padStart(2, '0')
            }?.uppercase()
            return TransitIdentity.create(NAME, serial)
        }

        override fun parseInfo(card: ClassicCard): ErgTransitInfo {
            val capsule = parse(card)
            return ErgTransitInfo(capsule)
        }
    }
}
