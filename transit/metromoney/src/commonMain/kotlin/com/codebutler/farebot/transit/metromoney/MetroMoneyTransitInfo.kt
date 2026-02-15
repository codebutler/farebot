/*
 * MetroMoneyTransitInfo.kt
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

package com.codebutler.farebot.transit.metromoney

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import farebot.transit.metromoney.generated.resources.Res
import farebot.transit.metromoney.generated.resources.card_name_metromoney
import farebot.transit.metromoney.generated.resources.metromoney_date1
import farebot.transit.metromoney.generated.resources.metromoney_date2
import farebot.transit.metromoney.generated.resources.metromoney_date3
import farebot.transit.metromoney.generated.resources.metromoney_date4

class MetroMoneyTransitInfo(
    private val mSerial: Long,
    private val mBalance: Int,
    private val mDate1: String,
    private val mDate2: String,
    private val mDate3: String,
    private val mDate4: String,
) : TransitInfo() {
    override val serialNumber: String
        get() = NumberUtils.zeroPad(mSerial, 10)

    override val cardName: FormattedString
        get() = FormattedString(Res.string.card_name_metromoney)

    override val balance: TransitBalance
        get() = TransitBalance(balance = TransitCurrency(mBalance, "GEL"))

    override val info: List<ListItemInterface>?
        get() {
            val items = mutableListOf<ListItemInterface>()
            if (mDate1.isNotEmpty()) items.add(ListItem(Res.string.metromoney_date1, mDate1))
            if (mDate2.isNotEmpty()) items.add(ListItem(Res.string.metromoney_date2, mDate2))
            if (mDate3.isNotEmpty()) items.add(ListItem(Res.string.metromoney_date3, mDate3))
            if (mDate4.isNotEmpty()) items.add(ListItem(Res.string.metromoney_date4, mDate4))
            return items.ifEmpty { null }
        }
}
