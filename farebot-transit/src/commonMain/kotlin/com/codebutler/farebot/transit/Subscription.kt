/*
 * Subscription.kt
 *
 * Copyright (C) 2011 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2011-2012, 2015-2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2019 Google
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

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import farebot.farebot_transit.generated.resources.*
import kotlin.time.Instant

/**
 * Represents subscriptions on a card. Subscriptions can be used to represent a number of different
 * things "loaded" on to the card.
 *
 * Travel Pass or Season Pass: a card may, for example, allow travel passes that allow unlimited
 * travel (on certain modes of transport, or with certain operating companies) for a period of time
 * (eg: 7 days, 30 days, 1 year...)
 *
 * Automatic top-up: a card may be linked to a credit card or other payment instrument, which will
 * be used to "top-up" or "refill" a card in the event a trip takes the balance below $0.
 */
abstract class Subscription {

    /**
     * An identifier for the subscription number.
     * If null, this will not be displayed.
     */
    open val id: Int?
        get() = null

    /**
     * When the subscription starts.
     * If null is returned, then the subscription has no start date.
     */
    open val validFrom: Instant?
        get() = null

    /**
     * When the subscription ends.
     * If null is returned, then the subscription has never been used, or there is no date limit.
     */
    open val validTo: Instant?
        get() = null

    /**
     * The machine ID for the terminal that sold the subscription, or null if unknown.
     */
    open val machineId: Int?
        get() = null

    /**
     * A name (or description) of the subscription.
     * eg: "Travel Ten", "Multi-trip", "City Pass"...
     */
    open val subscriptionName: String?
        get() = null

    /**
     * The number of passengers that the subscription is valid for. If a value greater than 1 is
     * supplied, then this will be displayed in the UI.
     */
    open val passengerCount: Int
        get() = 1

    open val subscriptionState: SubscriptionState
        get() = SubscriptionState.UNKNOWN

    /**
     * Full name of the agency for the subscription.
     * By default, this returns null (and doesn't display any information).
     */
    open val agencyName: String?
        get() = null

    /**
     * Short name of the agency for the subscription, for use in compact displays.
     * By default, this returns [agencyName].
     */
    open val shortAgencyName: String?
        get() = agencyName

    /**
     * Where a subscription can be sold by a third party (such as a retailer), this is the name of
     * the retailer.
     * By default, this returns null (and doesn't display any information).
     */
    open val saleAgencyName: String?
        get() = null

    /**
     * The timestamp that the subscription was purchased at, or null if not known.
     */
    open val purchaseTimestamp: Instant?
        get() = null

    /**
     * The timestamp that the subscription was last used at, or null if not known.
     */
    open val lastUseTimestamp: Instant?
        get() = null

    /**
     * The method by which this subscription was purchased. If [PaymentMethod.UNKNOWN], then
     * nothing will be displayed.
     */
    open val paymentMethod: PaymentMethod
        get() = PaymentMethod.UNKNOWN

    /**
     * The total number of remaining trips in this subscription.
     * If unknown or there is no limit to the number of trips, return null (default).
     */
    open val remainingTripCount: Int?
        get() = null

    /**
     * The total number of trips in this subscription.
     * If unknown or there is no limit to the number of trips, return null (default).
     */
    open val totalTripCount: Int?
        get() = null

    /**
     * The total number of remaining days that this subscription can be used on.
     * This is distinct to [validTo] -- this is for subscriptions where it can be used
     * on distinct but non-sequential days.
     */
    open val remainingDayCount: Int?
        get() = null

    /**
     * Where a subscription limits the number of trips in a day that may be taken, this value
     * indicates the number of trips remaining on the day of last use.
     */
    open val remainingTripsInDayCount: Int?
        get() = null

    /**
     * An array of zone numbers for which the subscription is valid.
     * Returns null if there are no restrictions, or the restrictions are unknown (default).
     */
    open val zones: IntArray?
        get() = null

    /**
     * For networks that allow transfers (ie: multiple vehicles may be used as part of a single trip
     * and charged at a flat rate), this shows the latest time that transfers may be made.
     */
    open val transferEndTimestamp: Instant?
        get() = null

    /**
     * The cost of the subscription, or null if unknown (default).
     */
    open val cost: TransitCurrency?
        get() = null

    /**
     * Extra information that doesn't fit within the standard bounds of the interface.
     * By default, this attempts to collect less common attributes and put them here.
     */
    open val info: List<ListItemInterface>?
        get() {
            val items = mutableListOf<ListItem>()

            saleAgencyName?.let {
                items.add(ListItem(Res.string.subscription_seller_agency, it))
            }

            machineId?.let {
                items.add(ListItem(Res.string.subscription_machine_id, it.toString()))
            }

            purchaseTimestamp?.let {
                items.add(ListItem(Res.string.subscription_purchase_date, it.toString()))
            }

            lastUseTimestamp?.let {
                items.add(ListItem(Res.string.subscription_last_used, it.toString()))
            }

            cost?.let {
                items.add(ListItem(Res.string.subscription_cost, it.formatCurrencyString(isBalance = true)))
            }

            id?.let {
                items.add(ListItem(Res.string.subscription_id, it.toString()))
            }

            if (paymentMethod != PaymentMethod.UNKNOWN) {
                items.add(ListItem(Res.string.subscription_payment_method, paymentMethod.description))
            }

            transferEndTimestamp?.let { transferEnd ->
                if (lastUseTimestamp != null) {
                    items.add(ListItem(Res.string.subscription_free_transfers_until, transferEnd.toString()))
                }
            }

            if (lastUseTimestamp != null) {
                remainingTripsInDayCount?.let { trips ->
                    items.add(ListItem(Res.string.subscription_remaining_trips_today, "$trips"))
                }
            }

            zones?.let { z ->
                if (z.isNotEmpty()) {
                    val zonesString = z.joinToString(", ")
                    val label = if (z.size == 1) Res.string.subscription_travel_zone else Res.string.subscription_travel_zones
                    items.add(ListItem(label, zonesString))
                }
            }

            return items.ifEmpty { null }
        }

    fun formatRemainingTrips(): String? {
        val remainingTrips = remainingTripCount
        val totalTrips = totalTripCount

        return when {
            remainingTrips != null && totalTrips != null ->
                "$remainingTrips of $totalTrips trips remaining"
            remainingTrips != null ->
                "$remainingTrips trips remaining"
            else -> null
        }
    }

    fun formatValidity(): String? {
        val from = validFrom
        val to = validTo
        return when {
            from != null && to != null -> "Valid from $from to $to"
            to != null -> "Valid to $to"
            from != null -> "Valid from $from"
            else -> null
        }
    }

    enum class SubscriptionState {
        /** No state is known, display no UI for the state. */
        UNKNOWN,
        /** The subscription is present on the card, but currently disabled. */
        INACTIVE,
        /** The subscription has been purchased, but never used. */
        UNUSED,
        /** The subscription has been purchased, and has started. */
        STARTED,
        /** The subscription has been "used up". */
        USED,
        /** The subscription has expired. */
        EXPIRED
    }

    /**
     * Describes payment methods for a [Subscription].
     */
    enum class PaymentMethod(val description: String) {
        UNKNOWN("Unknown"),
        CASH("Cash"),
        CREDIT_CARD("Credit card"),
        DEBIT_CARD("Debit card"),
        CHEQUE("Cheque"),
        /** The payment is made using stored balance on the transit card itself. */
        TRANSIT_CARD("Transit card"),
        /** The subscription costs nothing (gratis). */
        FREE("Free")
    }
}
