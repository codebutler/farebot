/*
 * RicaricaMiTransitFactory.kt
 *
 * Copyright 2018 Google
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

package com.codebutler.farebot.transit.ricaricami

import com.codebutler.farebot.base.util.DefaultStringResource
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.base.util.isAllZero
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.TransactionTrip
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.en1545.*
import farebot.farebot_transit_ricaricami.generated.resources.Res
import farebot.farebot_transit_ricaricami.generated.resources.ricaricami_card_name
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

class RicaricaMiTransitFactory(
    private val stringResource: StringResource = DefaultStringResource()
) : TransitFactory<ClassicCard, RicaricaMiTransitInfo> {

    override fun check(card: ClassicCard): Boolean {
        val sector0 = card.getSector(0) as? DataClassicSector ?: return false
        for (i in 1..2) {
            val block = sector0.getBlock(i).data
            for (j in (if (i == 1) 1 else 0)..7)
                if (block.byteArrayToInt(j * 2, 2) != RICARICA_MI_ID)
                    return false
        }
        return true
    }

    override fun parseIdentity(card: ClassicCard) = TransitIdentity(
        runBlocking { getString(Res.string.ricaricami_card_name) },
        null
    )

    override fun parseInfo(card: ClassicCard): RicaricaMiTransitInfo {
        return parse(card, stringResource)
    }

    companion object {
        private const val RICARICA_MI_ID = 0x0221

        private val CONTRACT_LIST_FIELDS = En1545Container(
            En1545Repeat(4, En1545Container(
                En1545FixedInteger(En1545TransitData.CONTRACTS_UNKNOWN_A, 3), // Always 3 so far
                En1545FixedInteger(En1545TransitData.CONTRACTS_TARIFF, 16),
                En1545FixedInteger(En1545TransitData.CONTRACTS_UNKNOWN_B, 5), // No idea
                En1545FixedInteger(En1545TransitData.CONTRACTS_POINTER, 4)
            ))
        )

        private val BLOCK_1_0_FIELDS = En1545Container(
            En1545FixedInteger(En1545TransitData.ENV_UNKNOWN_A, 9),
            En1545FixedInteger.dateBCD(En1545TransitData.HOLDER_BIRTH_DATE),
            En1545FixedHex(En1545TransitData.ENV_UNKNOWN_B, 47),
            En1545FixedInteger.date(En1545TransitData.ENV_APPLICATION_VALIDITY_END),
            En1545FixedInteger(En1545TransitData.ENV_UNKNOWN_C, 26)
        )
        private val BLOCK_1_1_FIELDS = En1545Container(
            En1545FixedHex(En1545TransitData.ENV_UNKNOWN_D, 64),
            En1545FixedInteger.date(En1545TransitData.ENV_APPLICATION_ISSUE),
            En1545FixedHex(En1545TransitData.ENV_UNKNOWN_E, 49)
        )

        private fun selectSubData(subData0: ByteArray, subData1: ByteArray): Int {
            val date0 = subData0.getBitsFromBuffer(6, 14)
            val date1 = subData1.getBitsFromBuffer(6, 14)

            if (date0 > date1)
                return 0
            if (date0 < date1)
                return 1

            val tapno0 = subData0.getBitsFromBuffer(0, 6)
            val tapno1 = subData1.getBitsFromBuffer(0, 6)

            if (tapno0 > tapno1)
                return 0
            if (tapno0 < tapno1)
                return 1

            if (subData1.isAllZero())
                return 0
            if (subData0.isAllZero())
                return 1

            // Compare byte arrays lexicographically
            for (i in subData0.indices) {
                val a = subData0[i].toInt() and 0xFF
                val b = subData1[i].toInt() and 0xFF
                if (a > b) return 0
                if (a < b) return 1
            }
            return 1
        }

        private fun parse(card: ClassicCard, stringResource: StringResource): RicaricaMiTransitInfo {
            val sector1 = card.getSector(1) as DataClassicSector
            val ticketEnvParsed = En1545Parser.parse(sector1.getBlock(0).data, BLOCK_1_0_FIELDS)
            ticketEnvParsed.append(sector1.getBlock(1).data, BLOCK_1_1_FIELDS)

            val trips = (0..5).mapNotNull { i ->
                val base = 0xa * 3 + 2 + i * 2
                val sectorIdx1 = base / 3
                val blockIdx1 = base % 3
                val sectorIdx2 = (base + 1) / 3
                val blockIdx2 = (base + 1) % 3
                val tripData = (card.getSector(sectorIdx1) as DataClassicSector).getBlock(blockIdx1).data +
                        (card.getSector(sectorIdx2) as DataClassicSector).getBlock(blockIdx2).data
                if (tripData.isAllZero()) {
                    null
                } else
                    RicaricaMiTransaction.parse(tripData, stringResource)
            }
            val mergedTrips = TransactionTrip.merge(trips)
            val subscriptions = mutableListOf<RicaricaMiSubscription>()
            for (i in 0..2) {
                val sec = card.getSector(i + 6) as DataClassicSector
                if (sec.getBlock(0).data.isAllZero()
                        && sec.getBlock(1).data.isAllZero()
                        && sec.getBlock(2).data.isAllZero())
                    continue
                val subData = arrayOf(sec.getBlock(0).data, sec.getBlock(1).data)
                val sel = selectSubData(subData[0], subData[1])
                subscriptions.add(RicaricaMiSubscription.parse(subData[sel],
                        (card.getSector(i + 2) as DataClassicSector).getBlock(sel).data,
                        (card.getSector(i + 2) as DataClassicSector).getBlock(2).data, stringResource))
            }
            // TODO: check following. It might have more to do with subscription type
            // than slot
            val sec = card.getSector(9) as DataClassicSector
            val subData = arrayOf(sec.getBlock(1).data, sec.getBlock(2).data)
            if (!subData[0].isAllZero() || !subData[1].isAllZero()) {
                val sel = selectSubData(subData[0], subData[1])
                subscriptions.add(RicaricaMiSubscription.parse(subData[sel],
                        (card.getSector(5) as DataClassicSector).getBlock(1).data,
                        (card.getSector(5) as DataClassicSector).getBlock(2).data, stringResource))
            }
            val contractList1 = En1545Parser.parse(
                (card.getSector(14) as DataClassicSector).getBlock(2).data,
                CONTRACT_LIST_FIELDS
            )
            val contractList2 = En1545Parser.parse(
                (card.getSector(15) as DataClassicSector).getBlock(2).data,
                CONTRACT_LIST_FIELDS
            )
            return RicaricaMiTransitInfo(
                trips = mergedTrips,
                subscriptions = subscriptions,
                ticketEnvParsed = ticketEnvParsed,
                contractList1 = contractList1,
                contractList2 = contractList2
            )
        }
    }
}
