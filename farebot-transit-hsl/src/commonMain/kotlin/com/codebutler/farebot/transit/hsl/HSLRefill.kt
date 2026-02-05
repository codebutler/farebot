/*
 * HSLRefill.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014, 2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.hsl

import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.transit.Refill
import farebot.farebot_transit_hsl.generated.resources.*
import com.codebutler.farebot.base.util.CurrencyFormatter

class HSLRefill(
    private val timestamp: Long,
    private val amount: Long
) : Refill() {

    override fun getTimestamp(): Long = timestamp

    override fun getAmount(): Long = amount

    override fun getAgencyName(stringResource: StringResource): String =
        stringResource.getString(Res.string.hsl_balance_refill)

    override fun getShortAgencyName(stringResource: StringResource): String =
        stringResource.getString(Res.string.hsl_balance_refill)

    override fun getAmountString(stringResource: StringResource): String {
        return CurrencyFormatter.formatAmount(amount, "EUR")
    }

    companion object {
        fun create(timestamp: Long, amount: Long): HSLRefill =
            HSLRefill(timestamp, amount)
    }
}
