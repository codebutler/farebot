/*
 * UmarshSector.kt
 *
 * Copyright 2019 Google
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

package com.codebutler.farebot.transit.umarsh

import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.byteArrayToIntReversed
import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.base.util.sliceOffLen
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.zolotayakorona.RussiaTaxCodes
import farebot.transit.umarsh.generated.resources.Res
import farebot.transit.umarsh.generated.resources.umarsh_card_name
import farebot.transit.umarsh.generated.resources.umarsh_unknown
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlin.time.Instant

data class UmarshSector(
    val counter: Int,
    val serialNumber: Int,
    val balanceRaw: Int,
    val total: Int,
    val tariffRaw: Int,
    val lastRefill: LocalDate?,
    val validTo: LocalDate?,
    val cardExpiry: LocalDate?,
    val refillCounter: Int,
    val hash: ByteArray,
    val fieldA: ByteArray,
    val region: Int,
    val fieldB: Int,
    val machineId: Int,
    val fieldC: Int,
    val fieldD: Int,
    val fieldE: Int,
    val secno: Int,
) {
    val hasExtraSector: Boolean get() = region == 52

    private val system get() = systemsMap[region]

    private val tariff get() = system?.tariffs?.get(tariffRaw)

    val cardName: FormattedString
        get() = tariff?.cardName ?: system?.cardName ?: FormattedString(Res.string.umarsh_card_name)

    internal val denomination: UmarshDenomination
        get() = tariff?.denomination ?: if (total == 0) UmarshDenomination.RUB else UmarshDenomination.TRIPS

    val subscriptionName: FormattedString?
        get() = tariff?.name ?: FormattedString(Res.string.umarsh_unknown, NumberUtils.intToHex(tariffRaw))

    val remainingTripCount: Int?
        get() = if (denomination == UmarshDenomination.TRIPS) balanceRaw else null

    val totalTripCount: Int?
        get() = if (denomination == UmarshDenomination.TRIPS) total else null

    val balance: TransitBalance?
        get() =
            if (denomination == UmarshDenomination.RUB) {
                TransitBalance(
                    balance = TransitCurrency.RUB(balanceRaw * 100),
                    name = subscriptionName,
                )
            } else {
                null
            }

    val subscriptionValidTo: Instant?
        get() =
            validTo?.let {
                kotlinx.datetime
                    .LocalDateTime(it.year, it.month, it.day, 23, 59, 59)
                    .toInstant(RussiaTaxCodes.codeToTimeZone(region))
            }

    companion object {
        fun getRegion(sector: DataClassicSector): Int =
            (
                sector.getBlock(1).data.getBitsFromBuffer(100, 4)
                    or (sector.getBlock(1).data.getBitsFromBuffer(64, 3) shl 4)
            )

        fun parse(
            sector: DataClassicSector,
            secno: Int,
        ): UmarshSector {
            val block0 = sector.getBlock(0).data
            val block1 = sector.getBlock(1).data
            val block2 = sector.getBlock(2).data
            return UmarshSector(
                counter = 0x7fffffff - block0.byteArrayToIntReversed(0, 4),
                cardExpiry = parseDate(block1, 8),
                fieldA = block1.sliceOffLen(3, 2),
                total = block1.byteArrayToInt(5, 2),
                refillCounter = block1.byteArrayToInt(7, 1),
                region = getRegion(sector),
                serialNumber = block1.getBitsFromBuffer(67, 29),
                tariffRaw = block1.byteArrayToInt(13, 3),
                fieldB = block1.getBitsFromBuffer(96, 4),
                validTo = parseDate(block2, 0),
                fieldC = block2.byteArrayToInt(2, 1),
                machineId = block2.byteArrayToInt(3, 3),
                lastRefill = parseDate(block2, 48),
                fieldD = block2.getBitsFromBuffer(64, 1),
                balanceRaw = block2.getBitsFromBuffer(65, 15),
                fieldE = block2.byteArrayToInt(10, 1),
                hash = block2.sliceOffLen(11, 5),
                secno = secno,
            )
        }

        fun check(sector: DataClassicSector): Boolean {
            val block0 = sector.getBlock(0).data
            return block0.byteArrayToIntReversed(0, 4) == block0.byteArrayToIntReversed(4, 4).inv() &&
                block0.byteArrayToIntReversed(0, 4) == block0.byteArrayToIntReversed(8, 4) &&
                (block0[12].toInt() and 0xff) + (block0[13].toInt() and 0xff) == 0xff &&
                block0[12] == block0[14] &&
                block0[13] == block0[15] &&
                block0.byteArrayToIntReversed(0, 4) >= 0x7fffff00 &&
                (block0[13].toInt() and 0xff) >= 0x70
        }

        fun system(sector: DataClassicSector): UmarshSystem? = systemsMap[getRegion(sector)]
    }
}
