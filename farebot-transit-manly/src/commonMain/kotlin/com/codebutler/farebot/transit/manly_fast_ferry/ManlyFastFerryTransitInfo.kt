/*
 * ManlyFastFerryTransitInfo.kt
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

package com.codebutler.farebot.transit.manly_fast_ferry

import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.base.ui.FareBotUiTree
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.base.util.DateFormatStyle
import com.codebutler.farebot.base.util.formatDate
import kotlin.time.Instant
import farebot.farebot_transit_manly.generated.resources.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

/**
 * Transit data type for Manly Fast Ferry Smartcard (Sydney, AU).
 *
 * This transit card is a system made by ERG Group (now Videlli Limited / Vix Technology).
 *
 * Note: This is a distinct private company who run their own ferry service to Manly, separate to
 * Transport for NSW's Manly Ferry service.
 *
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/Manly-Fast-Ferry
 */
class ManlyFastFerryTransitInfo(
    private val serialNumberValue: String,
    private val tripList: List<Trip>,
    private val epochDate: Instant,
    private val balanceValue: Int
) : TransitInfo() {

    companion object {
        const val NAME = "Manly Fast Ferry"

        fun create(
            serialNumber: String,
            trips: List<Trip>,
            epochDate: Instant,
            balance: Int
        ): ManlyFastFerryTransitInfo {
            return ManlyFastFerryTransitInfo(serialNumber, trips, epochDate, balance)
        }
    }

    override val serialNumber: String = serialNumberValue

    override val trips: List<Trip> = tripList

    override val balance: TransitBalance
        get() = TransitBalance(balance = TransitCurrency.AUD(balanceValue))

    override val cardName: String = runBlocking { getString(Res.string.manly_card_name) }

    override fun getAdvancedUi(stringResource: StringResource): FareBotUiTree? {
        val uiBuilder = FareBotUiTree.builder(stringResource)
        val epochFormatted = formatDate(epochDate, DateFormatStyle.LONG)
        uiBuilder.item().title(Res.string.card_epoch).value(epochFormatted)
        return uiBuilder.build()
    }
}
