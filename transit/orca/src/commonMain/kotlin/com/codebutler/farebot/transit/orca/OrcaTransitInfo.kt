/*
 * OrcaTransitInfo.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014-2016 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
 *
 * Thanks to:
 * Karl Koscher <supersat@cs.washington.edu>
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

package com.codebutler.farebot.transit.orca

import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.transit.orca.generated.resources.*

class OrcaTransitInfo(
    override val trips: List<Trip>,
    private val serialNumberData: Int,
    private val balanceValue: Int,
) : TransitInfo() {
    override val cardName: String = getStringBlocking(Res.string.transit_orca_card_name)

    override val balance: TransitBalance
        get() = TransitBalance(balance = TransitCurrency.USD(balanceValue))

    override val serialNumber: String = serialNumberData.toString()

    override val subscriptions: List<Subscription>? = null

    override val hasUnknownStations: Boolean = true
}
