/*
 * SampleRefill.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2017 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.app.core.sample

import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.transit.Refill
import java.util.Date

class SampleRefill(private val date: Date) : Refill() {

    override fun getTimestamp(): Long = date.time / 1000

    override fun getAgencyName(stringResource: StringResource): String = "Agency"

    override fun getShortAgencyName(stringResource: StringResource): String = "Agency"

    override fun getAmount(): Long = 40L

    override fun getAmountString(stringResource: StringResource): String = "$40.00"
}
