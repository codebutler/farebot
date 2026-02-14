/*
 * MRTJTransitInfo.kt
 *
 * Copyright 2019 Bondan Sumbodo <sybond@gmail.com>
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

package com.codebutler.farebot.transit.mrtj

import com.codebutler.farebot.base.ui.HeaderListItem
import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.farebot_transit_mrtj.generated.resources.Res
import farebot.farebot_transit_mrtj.generated.resources.mrtj_last_transaction_amount
import farebot.farebot_transit_mrtj.generated.resources.mrtj_longname
import farebot.farebot_transit_mrtj.generated.resources.mrtj_other_data
import farebot.farebot_transit_mrtj.generated.resources.mrtj_transaction_counter

class MRTJTransitInfo(
    private val currentBalance: Int,
    private val transactionCounter: Int,
    private val lastTransAmount: Int,
) : TransitInfo() {
    override val balance: TransitBalance
        get() = TransitBalance(balance = TransitCurrency.IDR(currentBalance))

    override val serialNumber: String? = null

    override val cardName: String = getStringBlocking(Res.string.mrtj_longname)

    override val trips: List<Trip> = emptyList()

    override val info: List<ListItemInterface>
        get() =
            listOf(
                HeaderListItem(Res.string.mrtj_other_data),
                ListItem(Res.string.mrtj_transaction_counter, transactionCounter.toString()),
                ListItem(
                    Res.string.mrtj_last_transaction_amount,
                    TransitCurrency.IDR(lastTransAmount).formatCurrencyString(isBalance = false),
                ),
            )
}
