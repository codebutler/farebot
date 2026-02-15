/*
 * LeapTransitInfo.kt
 *
 * Copyright 2018-2019 Google
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

package com.codebutler.farebot.transit.tfileap

import com.codebutler.farebot.base.mdst.MdstStationLookup
import com.codebutler.farebot.base.ui.HeaderListItem
import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.DateFormatStyle
import com.codebutler.farebot.base.util.Luhn
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.formatDateTime
import com.codebutler.farebot.base.util.getBitsFromBufferSigned
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.transit.tfi_leap.generated.resources.Res
import farebot.transit.tfi_leap.generated.resources.transit_leap_accumulator_agency
import farebot.transit.tfi_leap.generated.resources.transit_leap_accumulator_region
import farebot.transit.tfi_leap.generated.resources.transit_leap_accumulator_total
import farebot.transit.tfi_leap.generated.resources.transit_leap_card_issuer
import farebot.transit.tfi_leap.generated.resources.transit_leap_card_name
import farebot.transit.tfi_leap.generated.resources.transit_leap_daily_accumulators
import farebot.transit.tfi_leap.generated.resources.transit_leap_initialisation_date
import farebot.transit.tfi_leap.generated.resources.transit_leap_issue_date
import farebot.transit.tfi_leap.generated.resources.transit_leap_period_start
import farebot.transit.tfi_leap.generated.resources.transit_leap_weekly_accumulators
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import com.codebutler.farebot.base.util.FormattedString

/**
 * Transit data for a TFI Leap card (Dublin, Ireland).
 *
 * Fare cap explanation: https://about.leapcard.ie/fare-capping
 *
 * There are two types of caps:
 * - Daily travel spend
 * - Weekly travel spend
 *
 * There are then two levels of caps:
 * - Single-operator spend (each operator has different thresholds)
 * - All-operator spend (sum of all fares)
 *
 * Certain services are excluded from the caps.
 */
class LeapTransitInfo(
    override val serialNumber: String,
    override val trips: List<Trip>,
    private val balanceValue: Int,
    private val initDate: Instant,
    private val expiryDate: Instant,
    private val issueDate: Instant,
    private val issuerId: Int,
    private val dailyAccumulators: AccumulatorBlock,
    private val weeklyAccumulators: AccumulatorBlock,
) : TransitInfo() {
    override val cardName: FormattedString
        get() = FormattedString(Res.string.transit_leap_card_name)

    override val balance: TransitBalance
        get() =
            TransitBalance(
                balance = TransitCurrency.EUR(balanceValue),
                validTo = expiryDate,
            )

    override val subscriptions: List<Subscription>? = null

    override val info: List<ListItemInterface>
        get() {
            val items = mutableListOf<ListItemInterface>()
            items.add(
                ListItem(
                    Res.string.transit_leap_initialisation_date,
                    formatDateTime(initDate, DateFormatStyle.LONG, DateFormatStyle.SHORT),
                ),
            )
            items.add(
                ListItem(
                    Res.string.transit_leap_issue_date,
                    formatDateTime(issueDate, DateFormatStyle.LONG, DateFormatStyle.SHORT),
                ),
            )
            items.add(ListItem(Res.string.transit_leap_card_issuer, issuerId.toString()))
            items.add(HeaderListItem(Res.string.transit_leap_daily_accumulators))
            items.addAll(dailyAccumulators.toListItems())
            items.add(HeaderListItem(Res.string.transit_leap_weekly_accumulators))
            items.addAll(weeklyAccumulators.toListItems())
            return items
        }

    companion object {
        internal const val APP_ID = 0xaf1122
        private const val BLOCK_SIZE = 0x180
        private val TZ_DUBLIN = TimeZone.of("Europe/Dublin")

        /**
         * The Leap epoch is 1997-01-01 00:00:00 in Dublin time.
         * Timestamps on the card are seconds since this epoch.
         */
        private val LEAP_EPOCH: Instant = LocalDateTime(1997, 1, 1, 0, 0).toInstant(TZ_DUBLIN)

        fun parseDate(
            file: ByteArray,
            offset: Int,
        ): Instant {
            val sec = file.byteArrayToInt(offset, 4)
            return Instant.fromEpochSeconds(LEAP_EPOCH.epochSeconds + sec.toLong())
        }

        fun parseBalance(
            file: ByteArray,
            offset: Int,
        ): Int = file.getBitsFromBufferSigned(offset * 8, 24)

        internal fun chooseBlock(
            file: ByteArray,
            txidoffset: Int,
        ): Int {
            val txIdA = file.byteArrayToInt(txidoffset, 2)
            val txIdB = file.byteArrayToInt(BLOCK_SIZE + txidoffset, 2)
            return if (txIdA > txIdB) 0 else BLOCK_SIZE
        }

        internal fun getSerial(
            file2: ByteArray,
            file6: ByteArray,
        ): String {
            val serial = file2.byteArrayToInt(0x25, 4)
            val initDate = parseDate(file6, 1)
            val localDateTime = initDate.toLocalDateTime(TZ_DUBLIN)
            // luhn checksum of number without date is always 6
            val checkDigit = (Luhn.calculateLuhn(serial.toString()) + 6) % 10
            return (
                NumberUtils.formatNumber(serial.toLong(), " ", 5, 4) + checkDigit + " " +
                    NumberUtils.zeroPad(localDateTime.month.ordinal + 1, 2) +
                    NumberUtils.zeroPad(localDateTime.year % 100, 2)
            )
        }
    }
}

/**
 * Represents a fare capping accumulator block (daily or weekly).
 */
class AccumulatorBlock(
    private val accumulators: List<Pair<Int, Int>>, // agency, value
    private val accumulatorRegion: Int?,
    private val accumulatorScheme: Int?,
    private val accumulatorStart: Instant,
) {
    constructor(file: ByteArray, offset: Int) : this(
        accumulatorStart = LeapTransitInfo.parseDate(file, offset),
        accumulatorRegion = file[offset + 4].toInt(),
        accumulatorScheme = file.byteArrayToInt(offset + 5, 3),
        accumulators =
            (0..3).map { i ->
                Pair(
                    file.byteArrayToInt(offset + 8 + 2 * i, 2),
                    LeapTransitInfo.parseBalance(file, offset + 0x10 + 3 * i),
                )
            },
        // 4 bytes hash
    )

    fun toListItems(): List<ListItemInterface> {
        val items = mutableListOf<ListItemInterface>()
        items.add(
            ListItem(
                Res.string.transit_leap_period_start,
                formatDateTime(accumulatorStart, DateFormatStyle.LONG, DateFormatStyle.SHORT),
            ),
        )
        items.add(ListItem(Res.string.transit_leap_accumulator_region, accumulatorRegion.toString()))
        items.add(
            ListItem(
                Res.string.transit_leap_accumulator_total,
                TransitCurrency.EUR(accumulatorScheme ?: 0).formatCurrencyString(true),
            ),
        )
        for ((agency, value) in accumulators) {
            if (value != 0) {
                val operatorName =
                    MdstStationLookup.getOperatorName(
                        LeapTrip.LEAP_STR,
                        agency,
                    ) ?: agency.toString()
                items.add(
                    ListItem(
                        Res.string.transit_leap_accumulator_agency,
                        TransitCurrency.EUR(value).formatCurrencyString(true),
                        operatorName,
                    ),
                )
            }
        }
        return items
    }
}
