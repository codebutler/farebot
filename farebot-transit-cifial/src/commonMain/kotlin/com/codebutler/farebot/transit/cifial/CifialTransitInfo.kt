/*
 * CifialTransitInfo.kt
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

package com.codebutler.farebot.transit.cifial

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.transit.TransitInfo
import farebot.farebot_transit_cifial.generated.resources.Res
import farebot.farebot_transit_cifial.generated.resources.cifial_card_name
import farebot.farebot_transit_cifial.generated.resources.cifial_hotel_checkin
import farebot.farebot_transit_cifial.generated.resources.cifial_hotel_checkout
import farebot.farebot_transit_cifial.generated.resources.cifial_hotel_room_number
import kotlinx.coroutines.runBlocking
import kotlin.time.Instant
import org.jetbrains.compose.resources.getString

class CifialTransitInfo(
    private val mRoomNumber: String,
    private val mCheckIn: Instant,
    private val mCheckOut: Instant
) : TransitInfo() {

    override val serialNumber: String? get() = null

    override val info: List<ListItemInterface>
        get() = listOf(
            ListItem(Res.string.cifial_hotel_room_number, mRoomNumber.trimStart('0')),
            ListItem(Res.string.cifial_hotel_checkin, mCheckIn.toString()),
            ListItem(Res.string.cifial_hotel_checkout, mCheckOut.toString())
        )

    override val cardName: String get() = runBlocking { getString(Res.string.cifial_card_name) }
}
