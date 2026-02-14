/*
 * WaikatoCardTransitFactory.kt
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

package com.codebutler.farebot.transit.waikato

import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.byteArrayToIntReversed
import com.codebutler.farebot.base.util.byteArrayToLong
import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.base.util.readASCII
import com.codebutler.farebot.base.util.sliceOffLen
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import com.codebutler.farebot.transit.Trip
import farebot.farebot_transit_waikato.generated.resources.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Instant

/**
 * Transit factory for Waikato-region cards (BUSIT / SmartRide Rotorua, New Zealand).
 *
 * These are MIFARE Classic cards used for bus transit in the Waikato region
 * (Hamilton) and Rotorua. The card type is identified by checking for
 * "Valid" or "Panda" strings in sector 0, block 1.
 *
 * Ported from Metrodroid.
 */
class WaikatoCardTransitFactory : TransitFactory<ClassicCard, WaikatoCardTransitInfo> {
    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override fun check(card: ClassicCard): Boolean {
        val sector0 = card.getSector(0)
        if (sector0 !is DataClassicSector) return false
        if (card.sectors.size < 2) return false
        val sector1 = card.getSector(1)
        if (sector1 !is DataClassicSector) return false

        val block1Data = sector0.getBlock(1).data
        if (block1Data.size < 5) return false
        val header = block1Data.copyOfRange(0, 5).readASCII()

        val isValidHeader = header == "Valid" || header == "Panda"
        if (!isValidHeader) return false

        val sector1Block0 = sector1.getBlock(0).data
        return sector1Block0.byteArrayToInt(2, 2) == 0x4850
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity {
        val serial = getSerial(card)
        val name = getName(card)
        return TransitIdentity.create(name, formatSerial(serial))
    }

    override fun parseInfo(card: ClassicCard): WaikatoCardTransitInfo {
        val serial = getSerial(card)
        val name = getName(card)

        val sector0 = card.getSector(0) as DataClassicSector
        val balSec = if (sector0.getBlock(1).data[5].toInt() and 0x10 == 0) 1 else 5

        val balSector1 = card.getSector(balSec + 1) as DataClassicSector
        val balSector2 = card.getSector(balSec + 2) as DataClassicSector

        val balBlock1Data = balSector1.getBlock(1).data
        val tripBlock0Data = balSector2.getBlock(0).data

        val lastTrip =
            WaikatoCardTrip.parse(
                tripBlock0Data.sliceOffLen(0, 7),
                Trip.Mode.BUS,
            )
        val lastRefill =
            WaikatoCardTrip.parse(
                tripBlock0Data.sliceOffLen(7, 7),
                Trip.Mode.TICKET_MACHINE,
            )

        val balance = balBlock1Data.byteArrayToIntReversed(9, 2)
        val lastTransactionDate = parseDate(balBlock1Data, 7)

        return WaikatoCardTransitInfo(
            serialNumberValue = formatSerial(serial),
            cardNameValue = name,
            balanceValue = balance,
            tripList = listOfNotNull(lastRefill, lastTrip),
            lastTransactionDate = lastTransactionDate,
        )
    }

    companion object {
        private val CARD_INFO =
            CardInfo(
                nameRes = Res.string.waikato_card_name_busit,
                cardType = CardType.MifareClassic,
                region = TransitRegion.NEW_ZEALAND,
                locationRes = Res.string.waikato_location,
                imageRes = Res.drawable.busitcard,
                latitude = -37.7870f,
                longitude = 175.2793f,
                brandColor = 0x2675AB,
                credits = listOf("Metrodroid Project"),
                preview = true,
            )

        private val TIME_ZONE = TimeZone.of("Pacific/Auckland")

        private fun getSerial(card: ClassicCard): Long {
            val sector1 = card.getSector(1) as DataClassicSector
            return sector1.getBlock(0).data.byteArrayToLong(4, 4)
        }

        private fun getName(card: ClassicCard): String {
            val sector0 = card.getSector(0) as DataClassicSector
            val header =
                sector0
                    .getBlock(1)
                    .data
                    .copyOfRange(0, 5)
                    .readASCII()
            return if (header == "Panda") {
                getStringBlocking(Res.string.waikato_card_name_busit)
            } else {
                getStringBlocking(Res.string.waikato_card_name_rotorua)
            }
        }

        private fun formatSerial(serial: Long): String = serial.toString(16)

        internal fun parseTimestamp(
            input: ByteArray,
            off: Int,
        ): Instant {
            val d = input.getBitsFromBuffer(off * 8, 5)
            val m = input.getBitsFromBuffer(off * 8 + 5, 4)
            val y = input.getBitsFromBuffer(off * 8 + 9, 4) + 2007
            val hm = input.getBitsFromBuffer(off * 8 + 13, 11)
            val localDateTime =
                LocalDateTime(
                    year = y,
                    month = m,
                    day = d,
                    hour = hm / 60,
                    minute = hm % 60,
                )
            return localDateTime.toInstant(TIME_ZONE)
        }

        internal fun parseDate(
            input: ByteArray,
            off: Int,
        ): LocalDate {
            val d = input.getBitsFromBuffer(off * 8, 5)
            val m = input.getBitsFromBuffer(off * 8 + 5, 4)
            val y = input.getBitsFromBuffer(off * 8 + 9, 7) + 1991
            return LocalDate(year = y, month = m, day = d)
        }
    }
}
