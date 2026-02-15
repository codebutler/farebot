/*
 * LisboaVivaSubscription.kt
 *
 * Copyright 2018 Google
 * Copyright 2025 Eric Butler
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

package com.codebutler.farebot.transit.calypso.lisboaviva

import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.en1545.En1545Container
import com.codebutler.farebot.transit.en1545.En1545FixedHex
import com.codebutler.farebot.transit.en1545.En1545FixedInteger
import com.codebutler.farebot.transit.en1545.En1545Lookup
import com.codebutler.farebot.transit.en1545.En1545Parsed
import com.codebutler.farebot.transit.en1545.En1545Parser
import com.codebutler.farebot.transit.en1545.En1545Subscription
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

internal class LisboaVivaSubscription private constructor(
    override val parsed: En1545Parsed,
    override val lookup: En1545Lookup,
    private val counter: Int?,
) : En1545Subscription() {
    private val isZapping: Boolean
        get() =
            contractTariff == LisboaVivaLookup.ZAPPING_TARIFF &&
                contractProvider == LisboaVivaLookup.INTERAGENCY31_AGENCY

    override val cost: TransitCurrency?
        get() =
            if (isZapping && counter != null) {
                TransitCurrency(counter, "EUR")
            } else {
                null
            }

    override val validTo: Instant?
        get() {
            val vf = validFrom ?: return super.validTo
            val period = parsed.getIntOrZero(CONTRACT_PERIOD)
            val tz = lookup.timeZone
            val startDate = vf.toLocalDateTime(tz).date
            when (parsed.getIntOrZero(CONTRACT_PERIOD_UNITS)) {
                0x109 -> {
                    // Days
                    val endDate = startDate.plus(period - 1, DateTimeUnit.DAY)
                    return endDate.atStartOfDayIn(tz)
                }
                0x10a -> {
                    // Calendar months
                    val ymStart = startDate.year * 12 + (startDate.month.ordinal)
                    val ymEnd = ymStart + period
                    val endYear = ymEnd / 12
                    val endMonth1 = (ymEnd % 12) + 1
                    val firstDayOfEndMonth = LocalDate(endYear, endMonth1, 1)
                    val lastDay = firstDayOfEndMonth.minus(1, DateTimeUnit.DAY)
                    return lastDay.atStartOfDayIn(tz)
                }
            }
            return super.validTo
        }

    override val agencyName: FormattedString?
        get() =
            if (contractProvider == LisboaVivaLookup.INTERAGENCY31_AGENCY) {
                null
            } else {
                super.agencyName
            }

    override val shortAgencyName: FormattedString?
        get() =
            if (contractProvider == LisboaVivaLookup.INTERAGENCY31_AGENCY) {
                null
            } else {
                super.shortAgencyName
            }

    companion object {
        private const val CONTRACT_PERIOD_UNITS = "ContractPeriodUnits"
        private const val CONTRACT_PERIOD = "ContractPeriod"

        private val CONTRACT_FIELDS =
            En1545Container(
                En1545FixedInteger(CONTRACT_PROVIDER, 7),
                En1545FixedInteger(CONTRACT_TARIFF, 16),
                En1545FixedInteger(CONTRACT_UNKNOWN_A, 2),
                En1545FixedInteger.date(CONTRACT_START),
                En1545FixedInteger(CONTRACT_SALE_AGENT, 5),
                En1545FixedInteger(CONTRACT_UNKNOWN_B, 19),
                En1545FixedInteger(CONTRACT_PERIOD_UNITS, 16),
                En1545FixedInteger.date(CONTRACT_END),
                En1545FixedInteger(CONTRACT_PERIOD, 7),
                En1545FixedHex(CONTRACT_UNKNOWN_C, 38),
            )

        fun parse(
            data: ByteArray,
            counter: Int?,
        ): LisboaVivaSubscription? {
            if (data.all { it == 0.toByte() }) return null
            val parsed = En1545Parser.parse(data, CONTRACT_FIELDS)
            return LisboaVivaSubscription(parsed, LisboaVivaLookup, counter)
        }
    }
}
