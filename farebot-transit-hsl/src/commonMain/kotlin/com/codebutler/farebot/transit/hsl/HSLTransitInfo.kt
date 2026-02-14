/*
 * HSLTransitInfo.kt
 *
 * Copyright 2013 Lauri Andler <lauri.andler@gmail.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.hsl

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemCategory
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.farebot_transit_hsl.generated.resources.*

class HSLTransitInfo(
    override val serialNumber: String?,
    private val mBalance: Int,
    override val trips: List<Trip>,
    override val subscriptions: List<Subscription>?,
    val applicationVersion: Int?,
    val applicationKeyVersion: Int?,
    val platformType: Int?,
    val securityLevel: Int?,
    val cardNameOverride: String,
) : TransitInfo() {
    override val cardName: String
        get() = cardNameOverride

    override val balance: TransitBalance?
        get() = TransitBalance(balance = TransitCurrency.EUR(mBalance))

    override val info: List<ListItemInterface>
        get() =
            listOfNotNull(
                applicationVersion?.let {
                    ListItem(Res.string.hsl_application_version, it.toString(), ListItemCategory.ADVANCED)
                },
                applicationKeyVersion?.let {
                    ListItem(Res.string.hsl_application_key_version, it.toString(), ListItemCategory.ADVANCED)
                },
                platformType?.let {
                    ListItem(Res.string.hsl_platform_type, it.toString(), ListItemCategory.ADVANCED)
                },
                securityLevel?.let {
                    ListItem(Res.string.hsl_security_level, it.toString(), ListItemCategory.ADVANCED)
                },
            )
}
