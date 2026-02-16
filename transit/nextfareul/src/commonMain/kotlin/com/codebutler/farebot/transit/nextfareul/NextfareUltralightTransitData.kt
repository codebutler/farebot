/*
 * NextfareUltralightTransitData.kt
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

package com.codebutler.farebot.transit.nextfareul

import com.codebutler.farebot.base.ui.FareBotUiTree
import com.codebutler.farebot.base.ui.uiTree
import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.base.util.Luhn
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.byteArrayToIntReversed
import com.codebutler.farebot.base.util.byteArrayToLong
import com.codebutler.farebot.base.util.isAllZero
import com.codebutler.farebot.card.ultralight.UltralightCard
import com.codebutler.farebot.transit.TransactionTrip
import com.codebutler.farebot.transit.TransactionTripAbstract
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.transit.nextfareul.generated.resources.Res
import farebot.transit.nextfareul.generated.resources.nextfareul_machine_code
import farebot.transit.nextfareul.generated.resources.nextfareul_product_type
import farebot.transit.nextfareul.generated.resources.nextfareul_ticket_type
import farebot.transit.nextfareul.generated.resources.nextfareul_ticket_type_concession
import farebot.transit.nextfareul.generated.resources.nextfareul_ticket_type_regular
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

data class NextfareUltralightTransitDataCapsule(
    val mProductCode: Int,
    val mSerial: Long,
    val mType: Byte,
    val mBaseDate: Int,
    val mMachineCode: Int,
    val mExpiry: Int,
    val mBalance: Int,
    val trips: List<TransactionTripAbstract>,
)

// Based on reference at http://www.lenrek.net/experiments/compass-tickets/.
abstract class NextfareUltralightTransitData : TransitInfo() {
    abstract val timeZone: TimeZone

    abstract val capsule: NextfareUltralightTransitDataCapsule

    override val balance: TransitBalance?
        get() =
            TransitBalance(
                balance = makeCurrency(capsule.mBalance),
                validTo = parseDateTime(timeZone, capsule.mBaseDate, capsule.mExpiry, 0),
            )

    override val serialNumber: String?
        get() = formatSerial(capsule.mSerial)

    override val trips: List<Trip>
        get() = capsule.trips

    override val info: List<ListItemInterface>?
        get() {
            val items = mutableListOf<ListItem>()
            val ticketTypeValue =
                if (capsule.mType.toInt() == 8) {
                    FormattedString(Res.string.nextfareul_ticket_type_concession)
                } else {
                    FormattedString(Res.string.nextfareul_ticket_type_regular)
                }
            items.add(ListItem(Res.string.nextfareul_ticket_type, ticketTypeValue))

            val productName = getProductName(capsule.mProductCode)
            if (productName != null) {
                items.add(ListItem(Res.string.nextfareul_product_type, productName))
            } else {
                items.add(ListItem(Res.string.nextfareul_product_type, capsule.mProductCode.toString(16)))
            }
            return items
        }

    override suspend fun getAdvancedUi(): FareBotUiTree = uiTree {
        item { title = Res.string.nextfareul_machine_code; value = capsule.mMachineCode.toString(16) }
    }

    protected abstract fun makeCurrency(value: Int): TransitCurrency

    protected abstract fun getProductName(productCode: Int): FormattedString?

    companion object {
        fun parse(
            card: UltralightCard,
            makeTransaction: (raw: ByteArray, baseDate: Int) -> NextfareUltralightTransaction,
        ): NextfareUltralightTransitDataCapsule {
            val page0 = card.getPage(4).data
            val page1 = card.getPage(5).data
            val page3 = card.getPage(7).data
            val lowerBaseDate = page0[3].toInt() and 0xff
            val upperBaseDate = page1[0].toInt() and 0xff
            val mBaseDate = upperBaseDate shl 8 or lowerBaseDate
            val transactions =
                listOf(8, 12).filter { isTransactionValid(card, it) }.map {
                    makeTransaction(card.readPages(it, 4), mBaseDate)
                }
            var trLater: NextfareUltralightTransaction? = null
            for (tr in transactions) {
                if (trLater == null || tr.isSeqNoGreater(trLater)) {
                    trLater = tr
                }
            }
            return NextfareUltralightTransitDataCapsule(
                mExpiry = trLater?.expiry ?: 0,
                mBalance = trLater?.balance ?: 0,
                trips = TransactionTrip.merge(transactions),
                mBaseDate = mBaseDate,
                mSerial = getSerial(card),
                mType = page0[1],
                mProductCode = page1[2].toInt() and 0x7f,
                mMachineCode = page3.byteArrayToIntReversed(0, 2),
            )
        }

        private fun isTransactionValid(
            card: UltralightCard,
            startPage: Int,
        ): Boolean = !card.readPages(startPage, 3).isAllZero()

        fun getSerial(card: UltralightCard): Long {
            val manufData0 = card.getPage(0).data
            val manufData1 = card.getPage(1).data
            val uid = manufData0.byteArrayToLong(1, 2) shl 32 or manufData1.byteArrayToLong(0, 4)
            val serial = uid + 1000000000000000L
            val luhn = Luhn.calculateLuhn(serial.toString())
            return serial * 10 + luhn
        }

        fun formatSerial(serial: Long): String = NumberUtils.formatNumber(serial, " ", 4, 4, 4, 4, 4)

        fun parseDateTime(
            tz: TimeZone,
            baseDate: Int,
            date: Int,
            time: Int,
        ): Instant {
            val year = (baseDate shr 9) + 2000
            val month = baseDate shr 5 and 0xf
            val day = baseDate and 0x1f
            val baseLocalDate = LocalDate(year, month, day)
            val adjustedDate = baseLocalDate.plus(-date, DateTimeUnit.DAY)
            return adjustedDate.atStartOfDayIn(tz) + time.minutes
        }
    }
}
