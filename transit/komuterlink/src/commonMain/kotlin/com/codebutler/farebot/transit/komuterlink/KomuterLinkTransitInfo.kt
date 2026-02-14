/*
 * KomuterLinkTransitInfo.kt
 *
 * Copyright 2019 Google
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

package com.codebutler.farebot.transit.komuterlink

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.Luhn
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.transit.komuterlink.generated.resources.Res
import farebot.transit.komuterlink.generated.resources.komuterlink_card_name
import farebot.transit.komuterlink.generated.resources.komuterlink_card_number
import farebot.transit.komuterlink.generated.resources.komuterlink_issue_date
import kotlin.time.Instant

class KomuterLinkTransitInfo(
    override val trips: List<Trip>,
    private val mBalance: Int,
    private val mSerial: Long,
    private val mIssueTimestamp: Instant,
    private val mCardNo: Int,
    private val mStoredLuhn: Int,
) : TransitInfo() {
    override val cardName: String
        get() = getStringBlocking(Res.string.komuterlink_card_name)

    override val serialNumber: String
        get() = NumberUtils.zeroPad(mSerial, 10)

    override val balance: TransitBalance
        get() = TransitBalance(balance = TransitCurrency.MYR(mBalance))

    override val subscriptions: List<Subscription>? = null

    override val info: List<ListItemInterface>
        get() {
            // Prefix may be wrong as CardNo is not printed anywhere
            val partialCardNo = "1" + NumberUtils.zeroPad(mCardNo, 10)
            val cardNo = partialCardNo + Luhn.calculateLuhn(partialCardNo)
            return listOf(
                ListItem(Res.string.komuterlink_card_number, cardNo),
                ListItem(Res.string.komuterlink_issue_date, mIssueTimestamp.toString()),
            )
        }
}
