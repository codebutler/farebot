/*
 * TransactionViewModel.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2017 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.app.feature.card

import android.content.Context
import androidx.annotation.DrawableRes
import com.codebutler.farebot.R
import com.codebutler.farebot.transit.Refill
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.Trip
import java.text.DateFormat
import java.util.Date
import java.util.Locale

sealed class TransactionViewModel(val context: Context) {

    abstract val date: Date?

    val time: String?
        get() = if (date != null) DateFormat.getTimeInstance(DateFormat.SHORT).format(date) else null

    class TripViewModel(context: Context, val trip: Trip) : TransactionViewModel(context) {

        override val date = Date(trip.timestamp * 1000)

        val route = trip.getRouteName(context.resources)

        val agency = trip.getAgencyName(context.resources)

        val fare = trip.getFareString(context.resources)

        val stations: CharSequence? = trip.getFormattedStations(context)

        @DrawableRes
        val imageResId: Int = when (trip.mode) {
                Trip.Mode.BUS -> R.drawable.ic_transaction_bus_32dp
                Trip.Mode.TRAIN -> R.drawable.ic_transaction_train_32dp
                Trip.Mode.TRAM -> R.drawable.ic_transaction_tram_32dp
                Trip.Mode.METRO -> R.drawable.ic_transaction_metro_32dp
                Trip.Mode.FERRY -> R.drawable.ic_transaction_ferry_32dp
                Trip.Mode.TICKET_MACHINE -> R.drawable.ic_transaction_tvm_32dp
                Trip.Mode.VENDING_MACHINE -> R.drawable.ic_transaction_vend_32dp
                Trip.Mode.POS -> R.drawable.ic_transaction_pos_32dp
                Trip.Mode.BANNED -> R.drawable.ic_transaction_banned_32dp
                Trip.Mode.OTHER -> R.drawable.ic_transaction_unknown_32dp
                else -> R.drawable.ic_transaction_unknown_32dp
        }
    }

    class RefillViewModel(context: Context, refill: Refill) :
        TransactionViewModel(context) {

        override val date: Date = Date(refill.timestamp * 1000)

        val agency = refill.getShortAgencyName(context.resources)

        val amount = "+ ${refill.getAmountString(context.resources)}"
    }

    class SubscriptionViewModel(context: Context, private val subscription: Subscription) :
        TransactionViewModel(context) {

        override val date = null

        val agency = subscription.getShortAgencyName(context.resources)

        val name = subscription.getSubscriptionName(context.resources)

        val valid: CharSequence
            get() {
                val format = DateFormat.getDateInstance(DateFormat.SHORT, Locale.UK)
                val validFrom = format.format(subscription.validFrom)
                val validTo = format.format(subscription.validTo)
                return context.getString(R.string.subscription_valid_format, validFrom, validTo)
            }

        val used = subscription.activation
    }
}
