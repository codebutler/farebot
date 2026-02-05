/*
 * LockedLeapTransitInfo.kt
 *
 * Copyright 2018-2019 Google
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

package com.codebutler.farebot.transit.tfi_leap

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.farebot_transit_tfi_leap.generated.resources.Res
import farebot.farebot_transit_tfi_leap.generated.resources.transit_leap_card_name
import farebot.farebot_transit_tfi_leap.generated.resources.transit_leap_locked_warning
import farebot.farebot_transit_tfi_leap.generated.resources.transit_leap_warning
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

/**
 * Transit data for a Leap card that could not be unlocked / read.
 */
class LockedLeapTransitInfo : TransitInfo() {

    override val cardName: String
        get() = runBlocking { getString(Res.string.transit_leap_card_name) }

    override val serialNumber: String? = null

    override val balance: TransitBalance? = null

    override val trips: List<Trip>? = null

    override val subscriptions: List<Subscription>? = null

    override val info: List<ListItemInterface>
        get() = listOf(
            ListItem(Res.string.transit_leap_warning, Res.string.transit_leap_locked_warning)
        )
}
