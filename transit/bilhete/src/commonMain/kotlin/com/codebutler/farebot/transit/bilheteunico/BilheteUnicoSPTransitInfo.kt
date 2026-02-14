/*
 * BilheteUnicoSPTransitInfo.kt
 *
 * Copyright 2013 Marcelo Liberato <mliberato@gmail.com>
 * Copyright (C) 2013-2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.bilheteunico

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.DateFormatStyle
import com.codebutler.farebot.base.util.formatDate
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.transit.bilhete.generated.resources.Res
import farebot.transit.bilhete.generated.resources.bilhete_card_name
import farebot.transit.bilhete.generated.resources.bilhete_date_1
import farebot.transit.bilhete.generated.resources.bilhete_refill_counter
import farebot.transit.bilhete.generated.resources.bilhete_trips_counter

class BilheteUnicoSPTransitInfo(
    private val credit: Int,
    override val serialNumber: String?,
    override val trips: List<Trip>,
    private val transactionCounter: Int,
    private val refillTransactionCounter: Int,
    private val day2: Int,
) : TransitInfo() {
    override val cardName: String
        get() = getStringBlocking(Res.string.bilhete_card_name)

    override val balance: TransitBalance
        get() = TransitBalance(balance = TransitCurrency.BRL(credit))

    override val info: List<ListItemInterface>
        get() {
            val items =
                mutableListOf<ListItemInterface>(
                    ListItem(Res.string.bilhete_trips_counter, transactionCounter.toString()),
                    ListItem(Res.string.bilhete_refill_counter, refillTransactionCounter.toString()),
                )
            val day2Instant = BilheteUnicoSPTrip.epochDay(day2)
            if (day2Instant != null) {
                items.add(ListItem(Res.string.bilhete_date_1, formatDate(day2Instant, DateFormatStyle.LONG)))
            }
            return items
        }
}
