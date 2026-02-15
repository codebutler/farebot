/*
 * SeqGoRefill.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.seqgo

import com.codebutler.farebot.base.util.CurrencyFormatter
import com.codebutler.farebot.transit.Refill
import com.codebutler.farebot.transit.seqgo.record.SeqGoTopupRecord
import farebot.transit.seqgo.generated.resources.*
import com.codebutler.farebot.base.util.FormattedString

/**
 * Represents a top-up event on the Go card.
 */
class SeqGoRefill(
    private val topup: SeqGoTopupRecord,
) : Refill() {
    override fun getTimestamp(): Long = topup.timestamp.toEpochMilliseconds() / 1000

    override fun getAgencyName(): FormattedString = FormattedString("")

    override fun getShortAgencyName(): FormattedString =
        FormattedString(
            if (topup.automatic) {
                Res.string.seqgo_refill_automatic
            } else {
                Res.string.seqgo_refill_manual
            },
        )

    override fun getAmount(): Long = topup.credit.toLong()

    override fun getAmountString(): String =
        CurrencyFormatter.formatAmount(getAmount(), "AUD")

    companion object {
        fun create(topup: SeqGoTopupRecord): SeqGoRefill = SeqGoRefill(topup)
    }
}
