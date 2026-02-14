/*
 * ErgRefill.kt
 *
 * Copyright 2015-2019 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.erg

import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.transit.Refill
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.erg.record.ErgPurseRecord
import farebot.transit.erg.generated.resources.Res
import farebot.transit.erg.generated.resources.erg_card_name

/**
 * Represents a refill/top-up on an ERG card.
 */
open class ErgRefill(
    val purse: ErgPurseRecord,
    val epochDate: Int,
    private val currencyFactory: (Int) -> TransitCurrency = { TransitCurrency.XXX(it) },
) : Refill() {
    override fun getTimestamp(): Long = ErgTrip.convertTimestamp(epochDate, purse.day, purse.minute)

    override fun getAgencyName(stringResource: StringResource): String = getStringBlocking(Res.string.erg_card_name)

    override fun getShortAgencyName(stringResource: StringResource): String? = getAgencyName(stringResource)

    override fun getAmount(): Long = purse.transactionValue.toLong()

    override fun getAmountString(stringResource: StringResource): String =
        currencyFactory(purse.transactionValue).formatCurrencyString()
}
