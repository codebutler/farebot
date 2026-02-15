/*
 * BonobusTransitInfo.kt
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

package com.codebutler.farebot.transit.bonobus

import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Instant

class BonobusTransitInfo(
    private val mSerial: Long,
    private val mBalance: Int,
    override val trips: List<BonobusTrip>,
    private val mIssueDate: Int,
    private val mExpiryDate: Int,
) : TransitInfo() {
    override val serialNumber: String
        get() = mSerial.toString()

    override val cardName: FormattedString
        get() = BonobusTransitFactory.NAME

    override val balance: TransitBalance
        get() {
            val validFrom = parseDate(mIssueDate)
            val validTo = parseDate(mExpiryDate)
            return TransitBalance(
                balance = TransitCurrency.EUR(mBalance),
                validFrom = validFrom,
                validTo = validTo,
            )
        }

    companion object {
        private fun parseDate(input: Int): Instant {
            val year = (input shr 9) + 2000
            val month = (input shr 5) and 0xf
            val day = input and 0x1f
            return LocalDateTime(year, month, day, 0, 0)
                .toInstant(TimeZone.of("Europe/Madrid"))
        }
    }
}
