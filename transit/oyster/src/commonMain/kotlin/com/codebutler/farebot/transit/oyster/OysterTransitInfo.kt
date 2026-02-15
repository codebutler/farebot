/*
 * OysterTransitInfo.kt
 *
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.oyster

import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransactionTrip
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.transit.oyster.generated.resources.Res
import farebot.transit.oyster.generated.resources.oyster_card_name

/**
 * Oyster (Transport for London) on MIFARE Classic
 *
 * This is for old format cards that are **not** labelled with "D".
 *
 * Reference: https://github.com/micolous/metrodroid/wiki/Oyster
 */
class OysterTransitInfo internal constructor(
    private val serial: Int,
    private val purse: OysterPurse?,
    private val transactions: List<OysterTransaction>,
    private val refills: List<OysterRefill>,
    private val passes: List<OysterTravelPass>,
) : TransitInfo() {
    override val cardName: FormattedString
        get() = FormattedString(Res.string.oyster_card_name)

    override val serialNumber: String = OysterTransitFactory.formatSerial(serial)

    override val balance: TransitBalance?
        get() = purse?.let { TransitBalance(balance = it.balance) }

    override val trips: List<Trip>
        get() = TransactionTrip.merge(transactions) + refills

    override val subscriptions: List<Subscription>
        get() = passes

    override val onlineServicesPage: String
        get() = "https://oyster.tfl.gov.uk/"
}
