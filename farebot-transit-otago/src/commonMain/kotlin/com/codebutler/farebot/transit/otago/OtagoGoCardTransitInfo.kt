/*
 * OtagoGoCardTransitInfo.kt
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

package com.codebutler.farebot.transit.otago

import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.farebot_transit_otago.generated.resources.Res
import farebot.farebot_transit_otago.generated.resources.otago_card_name
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

class OtagoGoCardTransitInfo(
    private val serial: Long,
    private val balanceValue: Int,
    private val refill: OtagoGoCardRefill?,
    private val tripList: List<OtagoGoCardTrip>
) : TransitInfo() {

    override val serialNumber: String
        get() = serial.toString(16)

    override val cardName: String
        get() = runBlocking { getString(Res.string.otago_card_name) }

    override val balance: TransitBalance
        get() = TransitBalance(balance = TransitCurrency.NZD(balanceValue))

    override val trips: List<Trip>
        get() = listOfNotNull(refill) + tripList
}
