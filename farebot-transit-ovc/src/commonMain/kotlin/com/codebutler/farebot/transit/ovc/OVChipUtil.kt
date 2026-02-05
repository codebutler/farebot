/*
 * OVChipUtil.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012-2013 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2012, 2014-2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.ovc

import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import com.codebutler.farebot.base.util.CurrencyFormatter

object OVChipUtil {

    private val OVC_EPOCH: Instant = LocalDateTime(1997, 1, 1, 0, 0, 0).toInstant(TimeZone.UTC)

    fun convertDate(date: Int): Instant {
        return convertDate(date, 0)
    }

    fun convertDate(date: Int, time: Int): Instant {
        val offset = date.toLong() * 86400 + (time / 60).toLong() * 3600 + (time % 60).toLong() * 60
        return Instant.fromEpochSeconds(OVC_EPOCH.epochSeconds + offset)
    }

    fun convertAmount(amount: Int): String {
        return CurrencyFormatter.formatAmount(amount, "EUR")
    }
}
