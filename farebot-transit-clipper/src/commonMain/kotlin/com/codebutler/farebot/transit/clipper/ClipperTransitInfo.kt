/*
 * ClipperTransitInfo.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014-2016 Eric Butler <eric@codebutler.com>
 *
 * Thanks to:
 * An anonymous contributor for reverse engineering Clipper data and providing
 * most of the code here.
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

package com.codebutler.farebot.transit.clipper

import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.farebot_transit_clipper.generated.resources.Res
import farebot.farebot_transit_clipper.generated.resources.transit_clipper_card_name
import kotlinx.coroutines.runBlocking
import kotlin.time.Instant
import org.jetbrains.compose.resources.getString

class ClipperTransitInfo(
    override val serialNumber: String,
    override val trips: List<Trip>,
    private val balanceValue: Int,
    private val expiryTimestamp: Instant? = null
) : TransitInfo() {

    override val cardName: String
        get() = runBlocking { getString(Res.string.transit_clipper_card_name) }

    override val balance: TransitBalance
        get() = TransitBalance(
            balance = TransitCurrency.USD(balanceValue),
            validTo = expiryTimestamp
        )

    override val subscriptions: List<Subscription>? = null

    fun getBalance(): Int = balanceValue

    companion object {
        fun create(
            serialNumber: String,
            trips: List<Trip>,
            balance: Int,
            expiryTimestamp: Instant? = null
        ): ClipperTransitInfo = ClipperTransitInfo(serialNumber, trips, balance, expiryTimestamp)
    }
}
