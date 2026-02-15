/*
 * BipTransitInfo.kt
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

package com.codebutler.farebot.transit.bip

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.transit.bip.generated.resources.Res
import farebot.transit.bip.generated.resources.bip_card_holders_id
import farebot.transit.bip.generated.resources.bip_card_holders_name
import farebot.transit.bip.generated.resources.bip_card_type
import farebot.transit.bip.generated.resources.bip_card_type_anonymous
import farebot.transit.bip.generated.resources.bip_card_type_personal
import com.codebutler.farebot.base.util.FormattedString

private const val NAME = "bip!"

class BipTransitInfo(
    private val mSerial: Long,
    private val mBalance: Int,
    override val trips: List<Trip>,
    private val mHolderId: Int,
    private val mHolderName: String?,
) : TransitInfo() {
    override val serialNumber: String
        get() = mSerial.toString()

    override val cardName: FormattedString
        get() = FormattedString(NAME)

    override val balance: TransitBalance
        get() = TransitBalance(balance = TransitCurrency.CLP(mBalance))

    override val info: List<ListItemInterface>
        get() =
            listOfNotNull(
                ListItem(
                    Res.string.bip_card_type,
                    if (mHolderId == 0) {
                        FormattedString(Res.string.bip_card_type_anonymous)
                    } else {
                        FormattedString(Res.string.bip_card_type_personal)
                    },
                ),
                if (mHolderName != null) {
                    ListItem(Res.string.bip_card_holders_name, mHolderName)
                } else {
                    null
                },
                if (mHolderId != 0) {
                    ListItem(Res.string.bip_card_holders_id, mHolderId.toString())
                } else {
                    null
                },
            )
}
