/*
 * SeqGoTransitInfo.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.seqgo

import com.codebutler.farebot.transit.Refill
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.transit.seqgo.generated.resources.*
import com.codebutler.farebot.base.util.FormattedString

/**
 * Transit data type for Go card (Brisbane / South-East Queensland, AU), used by Translink.
 *
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/Go-%28SEQ%29
 */
class SeqGoTransitInfo(
    private val serialNumberValue: String,
    private val tripList: List<Trip>,
    private val refillList: List<Refill>,
    private val unknownStations: Boolean,
    private val balanceValue: Int,
) : TransitInfo() {
    override val balance: TransitBalance
        get() = TransitBalance(balance = TransitCurrency.AUD(balanceValue))

    override val cardName: FormattedString = FormattedString(Res.string.seqgo_card_name)

    override val serialNumber: String = serialNumberValue

    override val trips: List<Trip> = tripList

    val refills: List<Refill> = refillList

    override val hasUnknownStations: Boolean = unknownStations

    override val moreInfoPage: String
        get() = "https://micolous.github.io/metrodroid/seqgo"

    override val onlineServicesPage: String
        get() = "https://gocard.translink.com.au/"

    companion object {
        val NAME: FormattedString get() = FormattedString(Res.string.seqgo_card_name)

        fun create(
            serialNumber: String,
            trips: List<Trip>,
            refills: List<Refill>,
            hasUnknownStations: Boolean,
            balance: Int,
        ): SeqGoTransitInfo = SeqGoTransitInfo(serialNumber, trips, refills, hasUnknownStations, balance)
    }
}
