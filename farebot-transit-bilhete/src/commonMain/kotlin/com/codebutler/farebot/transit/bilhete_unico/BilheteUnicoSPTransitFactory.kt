/*
 * BilheteUnicoSPTransitFactory.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2013-2015 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2013 Marcelo Liberato <mliberato@gmail.com>
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

package com.codebutler.farebot.transit.bilhete_unico

import com.codebutler.farebot.base.util.HashUtils
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.byteArrayToIntReversed
import com.codebutler.farebot.base.util.byteArrayToLong
import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.ClassicBlock
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import com.codebutler.farebot.transit.Trip
import farebot.farebot_transit_bilhete.generated.resources.*

class BilheteUnicoSPTransitFactory : TransitFactory<ClassicCard, BilheteUnicoSPTransitInfo> {

    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    companion object {
        private val CARD_INFO = CardInfo(
            nameRes = Res.string.bilhete_card_name,
            cardType = CardType.MifareClassic,
            region = TransitRegion.BRAZIL,
            locationRes = Res.string.bilhete_location,
            imageRes = Res.drawable.bilheteunicosp_card,
            latitude = -23.5505f,
            longitude = -46.6333f,
            brandColor = 0xD32424,
            credits = listOf("Marcelo Liberato", "Michael Farrell"),
            sampleDumpFile = "BilheteUnico.json",
        )

        private val MANUFACTURER = byteArrayOf(
            0x62.toByte(), 0x63.toByte(), 0x64.toByte(), 0x65.toByte(),
            0x66.toByte(), 0x67.toByte(), 0x68.toByte(), 0x69.toByte()
        )

        private fun checkCRC16Sector(sector: DataClassicSector): Boolean {
            val allData = ByteArray(sector.blocks.size * 16)
            for (i in sector.blocks.indices) {
                sector.getBlock(i).data.copyInto(allData, i * 16)
            }
            return HashUtils.calculateCRC16IBM(allData) == 0
        }

        private fun checkValueBlock(block: ClassicBlock): Boolean {
            val data = block.data
            if (data.size < 16) return false
            // MIFARE Classic value block format:
            // bytes 0-3: value, 4-7: ~value, 8-11: value, 12: addr, 13: ~addr, 14: addr, 15: ~addr
            for (i in 0..3) {
                if (data[i] != data[i + 8]) return false
                if ((data[i].toInt() and 0xff) xor (data[i + 4].toInt() and 0xff) != 0xff) return false
            }
            if (data[12] != data[14]) return false
            if ((data[12].toInt() and 0xff) xor (data[13].toInt() and 0xff) != 0xff) return false
            return true
        }

        private fun getSerial(card: ClassicCard): Long =
            (card.getSector(2) as DataClassicSector).getBlock(0).data.byteArrayToLong(3, 5)

        private fun formatSerial(serial: Long): String =
            NumberUtils.zeroPad(serial shr 36, 2) + "0 " +
                NumberUtils.zeroPad((serial shr 4) and 0xffffffffL, 9)
    }

    override fun check(card: ClassicCard): Boolean {
        val sector0 = card.getSector(0)
        if (sector0 !is DataClassicSector) return false
        val blockData = sector0.getBlock(0).data
        if (!blockData.copyOfRange(8, 16).contentEquals(MANUFACTURER)) return false

        // CRC16 validation on sectors 3-4 (at least one must pass)
        val sector3 = card.getSector(3) as? DataClassicSector
        val sector4 = card.getSector(4) as? DataClassicSector
        if (sector3 != null || sector4 != null) {
            val crc3ok = sector3?.let { checkCRC16Sector(it) } ?: false
            val crc4ok = sector4?.let { checkCRC16Sector(it) } ?: false
            if (!crc3ok && !crc4ok) return false
        }

        // Value block validation on sectors 5-8
        for (i in 5..8) {
            val sector = card.getSector(i) as? DataClassicSector ?: continue
            if (!checkValueBlock(sector.getBlock(1))) return false
        }

        return true
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity {
        return TransitIdentity.create(getStringBlocking(Res.string.bilhete_card_name), formatSerial(getSerial(card)))
    }

    override fun parseInfo(card: ClassicCard): BilheteUnicoSPTransitInfo {
        val creditSector = card.getSector(8) as DataClassicSector
        val creditBlock0 = creditSector.getBlock(0).data
        val lastRefillDay = creditBlock0.getBitsFromBuffer(2, 14)
        val lastRefillAmount = creditBlock0.getBitsFromBuffer(29, 11)
        val refillTransactionCounter = creditBlock0.getBitsFromBuffer(44, 14)

        val credit = creditSector.getBlock(1).data.byteArrayToIntReversed(0, 4)

        // Normally both sectors are identical but occasionally one might get corrupted
        var lastTripSector = card.getSector(3) as DataClassicSector
        if (!checkCRC16Sector(lastTripSector)) {
            lastTripSector = card.getSector(4) as DataClassicSector
        }

        val tripBlock0 = lastTripSector.getBlock(0).data
        val block1 = lastTripSector.getBlock(1).data
        val day = block1.getBitsFromBuffer(76, 14)
        val time = block1.getBitsFromBuffer(90, 11)
        val block2 = lastTripSector.getBlock(2).data
        val firstTapDay = block2.getBitsFromBuffer(2, 14)
        val firstTapTime = block2.getBitsFromBuffer(16, 11)
        val firstTapLine = block2.getBitsFromBuffer(27, 9)
        val transactionCounter = tripBlock0.getBitsFromBuffer(48, 14)

        val trips = mutableListOf<Trip>()
        if (day != 0) {
            trips.add(BilheteUnicoSPTrip.parse(lastTripSector))
        }
        if (firstTapDay != day || firstTapTime != time) {
            trips.add(BilheteUnicoSPFirstTap(firstTapDay, firstTapTime, firstTapLine))
        }
        if (lastRefillDay != 0) {
            trips.add(BilheteUnicoSPRefill(lastRefillDay, lastRefillAmount))
        }

        val identitySector = card.getSector(2) as DataClassicSector
        val day2 = identitySector.getBlock(0).data.getBitsFromBuffer(2, 14)

        return BilheteUnicoSPTransitInfo(
            credit = credit,
            serialNumber = formatSerial(getSerial(card)),
            trips = trips,
            transactionCounter = transactionCounter,
            refillTransactionCounter = refillTransactionCounter,
            day2 = day2,
        )
    }
}
