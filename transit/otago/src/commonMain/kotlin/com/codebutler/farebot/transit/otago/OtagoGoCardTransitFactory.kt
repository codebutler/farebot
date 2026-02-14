/*
 * OtagoGoCardTransitFactory.kt
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

package com.codebutler.farebot.transit.otago

import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.byteArrayToIntReversed
import com.codebutler.farebot.base.util.byteArrayToLong
import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.base.util.hex
import com.codebutler.farebot.base.util.sliceOffLen
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import farebot.transit.otago.generated.resources.*
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Instant

class OtagoGoCardTransitFactory : TransitFactory<ClassicCard, OtagoGoCardTransitInfo> {
    companion object {
        private val TZ = TimeZone.of("Pacific/Auckland")

        private val CARD_INFO =
            CardInfo(
                nameRes = Res.string.otago_card_name,
                cardType = CardType.MifareClassic,
                region = TransitRegion.NEW_ZEALAND,
                locationRes = Res.string.otago_location,
                imageRes = Res.drawable.otago_gocard,
                latitude = -45.8788f,
                longitude = 170.5028f,
                brandColor = 0x01275C,
            )
    }

    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override fun check(card: ClassicCard): Boolean {
        val sector0 = card.getSector(0)
        if (sector0 !is DataClassicSector) return false
        val sector1 = card.getSector(1)
        if (sector1 !is DataClassicSector) return false

        val block1 = sector0.getBlock(1).data
        if (!block1.sliceOffLen(0, 5).contentEquals("Valid".encodeToByteArray())) return false
        if (sector1.getBlock(0).data.byteArrayToInt(2, 2) != 0x4321) return false
        return true
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity {
        val serial = getSerial(card)
        val cardName = getStringBlocking(Res.string.otago_card_name)
        return TransitIdentity.create(cardName, serial.toString(16))
    }

    override fun parseInfo(card: ClassicCard): OtagoGoCardTransitInfo {
        val sector0 = card.getSector(0) as DataClassicSector
        val balSec = if (sector0.getBlock(1).data[5].toInt() and 0x10 == 0) 1 else 5

        // Read trip data: blocks from balSec+1 (block 1,2) and balSec+2 (blocks 0,1,2)
        val balSecPlus1 = card.getSector(balSec + 1) as DataClassicSector
        val balSecPlus2 = card.getSector(balSec + 2) as DataClassicSector
        val tripData = balSecPlus1.readBlocks(1, 2) + balSecPlus2.readBlocks(0, 3)

        val trips =
            (0..3).mapNotNull { i ->
                val slice = tripData.sliceOffLen(i * 17, 17)
                parseTripFromData(slice)
            }

        val balanceSector = card.getSector(balSec) as DataClassicSector
        val balanceValue = balanceSector.getBlock(2).data.byteArrayToIntReversed(8, 3)

        val refillSector = card.getSector(balSec + 3) as DataClassicSector
        val refill = parseRefill(refillSector)

        return OtagoGoCardTransitInfo(
            serial = getSerial(card),
            balanceValue = balanceValue,
            refill = refill,
            tripList = trips,
        )
    }

    private fun parseTimestamp(
        input: ByteArray,
        off: Int,
    ): Instant {
        val d = input.getBitsFromBuffer(off * 8, 5)
        val m = input.getBitsFromBuffer(off * 8 + 5, 4)
        val y = input.getBitsFromBuffer(off * 8 + 9, 4) + 2007
        val hm = input.getBitsFromBuffer(off * 8 + 13, 11)
        val ldt = LocalDateTime(y, m, d, hm / 60, hm % 60)
        return ldt.toInstant(TZ)
    }

    private fun parseTripFromData(data: ByteArray): OtagoGoCardTrip? {
        if (data.byteArrayToInt(3, 3) in listOf(0, 0xffffff)) return null
        val timestamp = parseTimestamp(data, 3)
        val cost = data.byteArrayToIntReversed(7, 2)
        val machine = data.sliceOffLen(11, 2).hex()
        return OtagoGoCardTrip(timestamp = timestamp, cost = cost, machine = machine)
    }

    private fun parseRefill(sector: DataClassicSector): OtagoGoCardRefill? {
        val block0 = sector.getBlock(0).data
        val block1 = sector.getBlock(1).data
        val amount = block0.byteArrayToIntReversed(12, 2)
        val timestamp = parseTimestamp(block0, 8)
        val machineId = block1.sliceOffLen(0, 2).hex()
        return OtagoGoCardRefill(
            timestamp = timestamp,
            amount = amount,
            machine = machineId,
        )
    }

    private fun getSerial(card: ClassicCard): Long =
        (card.getSector(1) as DataClassicSector).getBlock(0).data.byteArrayToLong(4, 4)
}
