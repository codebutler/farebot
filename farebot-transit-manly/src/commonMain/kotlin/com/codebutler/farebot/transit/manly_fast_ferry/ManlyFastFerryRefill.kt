/*
 * ManlyFastFerryRefill.kt
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

package com.codebutler.farebot.transit.manly_fast_ferry

import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.transit.Refill
import com.codebutler.farebot.transit.manly_fast_ferry.record.ManlyFastFerryPurseRecord
import kotlin.time.Instant
import com.codebutler.farebot.base.util.CurrencyFormatter

/**
 * Describes top-up amounts "purse credits".
 */
class ManlyFastFerryRefill(
    private val purse: ManlyFastFerryPurseRecord,
    private val epoch: Instant
) : Refill() {

    companion object {
        fun create(purse: ManlyFastFerryPurseRecord, epoch: Instant): ManlyFastFerryRefill {
            return ManlyFastFerryRefill(purse, epoch)
        }
    }

    override fun getTimestamp(): Long {
        val offset = purse.day.toLong() * 86400 + purse.minute.toLong() * 60
        return epoch.epochSeconds + offset
    }

    override fun getAgencyName(stringResource: StringResource): String {
        // There is only one agency on the card, don't show anything.
        return ""
    }

    override fun getShortAgencyName(stringResource: StringResource): String? {
        // There is only one agency on the card, don't show anything.
        return null
    }

    override fun getAmount(): Long {
        return purse.transactionValue.toLong()
    }

    override fun getAmountString(stringResource: StringResource): String {
        return CurrencyFormatter.formatAmount(getAmount(), "AUD")
    }
}
