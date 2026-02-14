/*
 * TransitInfo.kt
 *
 * Copyright (C) 2011-2012, 2014, 2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2015-2019 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit

import com.codebutler.farebot.base.ui.FareBotUiTree
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.StringResource

abstract class TransitInfo {
    /**
     * The (currency) balance of the card's purse. Most cards have only one purse.
     *
     * Cards with more than one purse must override [balances] instead.
     *
     * @return The balance of the card, or null if it is not known.
     */
    protected open val balance: TransitBalance?
        get() = null

    /**
     * The (currency) balances of all of the card's purses.
     *
     * Cards with multiple "purse" balances must override this property.
     * Cards with a single "purse" balance should override [balance] instead -- the default
     * implementation will automatically up-convert it.
     */
    open val balances: List<TransitBalance>?
        get() {
            val b = balance ?: return null
            return listOf(b)
        }

    /**
     * The serial number of the card. Generally printed on the card itself, or shown on receipts
     * from ticket vending machines.
     */
    abstract val serialNumber: String?

    /**
     * Lists all trips on the card.
     *
     * If the transit card does not store trip information, or the [TransitInfo] implementation
     * does not support reading trips yet, return null.
     *
     * If the [TransitInfo] implementation supports reading trip data, but no trips have been
     * taken on the card, return an empty list.
     */
    open val trips: List<Trip>?
        get() = null

    open val subscriptions: List<Subscription>?
        get() = null

    /**
     * Allows [TransitInfo] implementors to show extra information that doesn't fit within the
     * standard bounds of the interface. By default, this returns null, which hides the "Info" tab.
     */
    open val info: List<ListItemInterface>?
        get() = null

    abstract val cardName: String

    /**
     * You can optionally add a link to an FAQ page for the card.
     */
    open val moreInfoPage: String?
        get() = null

    /**
     * You may optionally link to a page which allows you to view the online services for the card.
     */
    open val onlineServicesPage: String?
        get() = null

    open val warning: String?
        get() = null

    /**
     * Message to display as an empty state when the card has no trips, subscriptions, or
     * other displayable data. Used for serial-only cards to explain why no data is available.
     */
    open val emptyStateMessage: String?
        get() = null

    /**
     * If a [TransitInfo] provider doesn't know some of the stops / stations on a user's card,
     * then it may raise a signal to the user to submit the unknown stations to our web service.
     *
     * @return false if all stations are known (default), true if there are unknown stations
     */
    open val hasUnknownStations: Boolean
        get() = false

    /**
     * Format all balances into a human-readable string.
     */
    fun formatBalanceString(): String {
        val b = balances
        if (b == null || b.isEmpty()) return ""
        return b.joinToString(", ") { tb ->
            val name = tb.name
            val balStr = tb.balance.formatCurrencyString(isBalance = true)
            if (name != null) "$name: $balStr" else balStr
        }
    }

    open fun getAdvancedUi(stringResource: StringResource): FareBotUiTree? = null
}
