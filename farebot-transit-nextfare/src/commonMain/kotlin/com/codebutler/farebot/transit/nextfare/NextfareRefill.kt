/*
 * NextfareRefill.kt
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

package com.codebutler.farebot.transit.nextfare

import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.transit.Refill
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.nextfare.record.NextfareTopupRecord
import farebot.farebot_transit_nextfare.generated.resources.Res
import farebot.farebot_transit_nextfare.generated.resources.nextfare_agency_name

/**
 * Represents a refill/top-up on a Nextfare card.
 */
open class NextfareRefill(
    private val record: NextfareTopupRecord,
    private val currencyFactory: (Int) -> TransitCurrency = { TransitCurrency.XXX(it) }
) : Refill() {

    override fun getTimestamp(): Long = record.timestamp.epochSeconds

    override fun getAgencyName(stringResource: StringResource): String =
        getStringBlocking(Res.string.nextfare_agency_name)

    override fun getShortAgencyName(stringResource: StringResource): String? = getAgencyName(stringResource)

    override fun getAmount(): Long = record.credit.toLong()

    override fun getAmountString(stringResource: StringResource): String =
        currencyFactory(record.credit).formatCurrencyString()
}
