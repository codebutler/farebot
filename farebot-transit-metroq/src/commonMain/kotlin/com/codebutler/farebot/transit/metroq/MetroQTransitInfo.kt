/*
 * MetroQTransitInfo.kt
 *
 * Copyright 2018 Google
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

package com.codebutler.farebot.transit.metroq

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import farebot.farebot_transit_metroq.generated.resources.Res
import farebot.farebot_transit_metroq.generated.resources.metroq_card_name
import farebot.farebot_transit_metroq.generated.resources.metroq_date1
import farebot.farebot_transit_metroq.generated.resources.metroq_day_pass
import farebot.farebot_transit_metroq.generated.resources.metroq_expiry_date
import farebot.farebot_transit_metroq.generated.resources.metroq_fare_card
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

class MetroQTransitInfo(
    private val serial: Long,
    private val balanceValue: Int,
    private val product: Int,
    private val expiryDate: LocalDate?,
    private val date1: LocalDate?
) : TransitInfo() {

    override val serialNumber: String
        get() = NumberUtils.zeroPad(serial, 8)

    override val cardName: String
        get() = getStringBlocking(Res.string.metroq_card_name)

    override val balance: TransitBalance
        get() {
            val name = when (product) {
                501 -> getStringBlocking(Res.string.metroq_fare_card)
                401 -> getStringBlocking(Res.string.metroq_day_pass)
                else -> product.toString()
            }
            return TransitBalance(
                balance = TransitCurrency.USD(balanceValue),
                name = name,
                validTo = expiryDate?.atStartOfDayIn(TimeZone.of("America/Chicago"))
            )
        }

    override val info: List<ListItemInterface>?
        get() {
            val items = mutableListOf<ListItemInterface>()
            expiryDate?.let {
                items.add(ListItem(Res.string.metroq_expiry_date, it.toString()))
            }
            date1?.let {
                items.add(ListItem(Res.string.metroq_date1, it.toString()))
            }
            return items.ifEmpty { null }
        }
}
