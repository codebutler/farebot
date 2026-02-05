/*
 * HafilatTransitInfo.kt
 *
 * Copyright 2015 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.hafilat

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.farebot_transit_hafilat.generated.resources.Res
import farebot.farebot_transit_hafilat.generated.resources.card_name_hafilat
import farebot.farebot_transit_hafilat.generated.resources.issue_date
import farebot.farebot_transit_hafilat.generated.resources.machine_id
import farebot.farebot_transit_hafilat.generated.resources.purse_serial_number
import farebot.farebot_transit_hafilat.generated.resources.ticket_type
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

class HafilatTransitInfo(
    override val trips: List<Trip>,
    override val subscriptions: List<Subscription>?,
    private val purse: HafilatSubscription?,
    private val serial: Long
) : TransitInfo() {

    override val serialNumber: String
        get() = formatSerial(serial)

    override val cardName: String
        get() = runBlocking { getString(Res.string.card_name_hafilat) }

    override val info: List<ListItemInterface>?
        get() {
            val items = mutableListOf<ListItem>()
            if (purse != null) {
                purse.subscriptionName?.let {
                    items.add(ListItem(Res.string.ticket_type, it))
                }
                purse.machineId?.let {
                    items.add(ListItem(Res.string.machine_id, it.toString()))
                }
                purse.purchaseTimestamp?.let {
                    items.add(ListItem(Res.string.issue_date, it.toString()))
                }
                purse.id?.let {
                    items.add(ListItem(Res.string.purse_serial_number, it.toString(16)))
                }
            }
            return items.ifEmpty { null }
        }

    companion object {
        fun formatSerial(serial: Long): String {
            val base = serial.toString().padStart(15, '0')
            return "01-$base"
        }
    }
}
