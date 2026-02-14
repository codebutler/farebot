/*
 * IntercardTransitInfo.kt
 *
 * Copyright 2019 Google
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

package com.codebutler.farebot.transit.intercard

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import farebot.farebot_transit_intercard.generated.resources.Res
import farebot.farebot_transit_intercard.generated.resources.card_name_intercard
import farebot.farebot_transit_intercard.generated.resources.last_transaction

class IntercardTransitInfo(
    private val mSerialNumber: Long,
    private val mBalance: Int?, // 10th of cents
    private val mLastTransaction: Int?,
) : TransitInfo() {
    override val cardName: String
        get() = getStringBlocking(Res.string.card_name_intercard)

    override val balance: TransitBalance?
        get() =
            mBalance?.let {
                TransitBalance(balance = parseCurrency(it))
            }

    override val serialNumber: String
        get() = mSerialNumber.toString()

    override val info: List<ListItemInterface>?
        get() {
            val items = mutableListOf<ListItem>()
            mLastTransaction?.let {
                items.add(
                    ListItem(
                        Res.string.last_transaction,
                        parseCurrency(it).formatCurrencyString(true),
                    ),
                )
            }
            return items.ifEmpty { null }
        }

    companion object {
        val NAME: String
            get() = getStringBlocking(Res.string.card_name_intercard)

        // FIXME: Apparently this system may be either in euro or in Swiss Francs.
        // Unfortunately Swiss Franc one still has string "EUR" in file 0, so this
        // suggests a lazy adaptation. Using CHF for now.
        fun parseCurrency(input: Int): TransitCurrency = TransitCurrency(input / 10, "CHF")
    }
}
