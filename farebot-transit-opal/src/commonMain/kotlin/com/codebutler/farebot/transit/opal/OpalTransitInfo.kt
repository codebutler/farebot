/*
 * OpalTransitInfo.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.opal

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.farebot_transit_opal.generated.resources.*
import kotlinx.datetime.DateTimeUnit
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant

/**
 * Transit data type for Opal (Sydney, AU).
 *
 * This uses the publicly-readable file on the card (7) in order to get the data.
 *
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/Opal
 */
class OpalTransitInfo(
    override val serialNumber: String,
    private val balanceValue: Int, // cents
    private val checksum: Int,
    val weeklyTrips: Int,
    private val autoTopup: Boolean,
    val lastTransaction: Int,
    val lastTransactionMode: Int,
    private val minute: Int,
    private val day: Int,
    val lastTransactionNumber: Int,
    private val stringResource: StringResource,
) : TransitInfo() {

    companion object {
        const val NAME = "Opal"
        // Opal epoch is 1980-01-01 00:00:00 Sydney local time
        private val OPAL_EPOCH_DATE = LocalDate(1980, 1, 1)
        private val SYDNEY_TZ = TimeZone.of("Australia/Sydney")
        private val OPAL_AUTOMATIC_TOP_UP = OpalSubscription.instance
    }

    override val cardName: String = NAME

    override val balance: TransitBalance
        get() = TransitBalance(balance = TransitCurrency.AUD(balanceValue))

    override val subscriptions: List<Subscription>?
        get() {
            // Opal has no concept of "subscriptions" (travel pass), only automatic top up.
            if (autoTopup) {
                return listOf<Subscription>(OPAL_AUTOMATIC_TOP_UP)
            }
            return emptyList()
        }

    override val trips: List<Trip>
        get() = listOf(
            OpalTrip(
                timestamp = lastTransactionTime,
                transactionMode = lastTransactionMode,
                transactionType = lastTransaction,
                stringResource = stringResource,
            )
        )

    override val onlineServicesPage: String
        get() = "https://m.opal.com.au/"

    override val info: List<ListItemInterface>
        get() = listOf(
            ListItem(
                stringResource.getString(Res.string.opal_weekly_trips),
                weeklyTrips.toString()
            ),
        )

    val lastTransactionTime: Instant
        get() {
            // Day and minute are stored as Sydney local time offsets from 1980-01-01 00:00
            // We need to convert to UTC Instant while respecting Sydney DST rules
            val epochDate = OPAL_EPOCH_DATE
            val localDate = epochDate.plus(day, DateTimeUnit.DAY)
            val hours = minute / 60
            val mins = minute % 60
            val localTime = LocalTime(hours, mins)
            val localDateTime = localDate.atTime(localTime)
            return localDateTime.toInstant(SYDNEY_TZ)
        }

    /**
     * Raw debugging fields for the card.
     * This is a simplified version of Metrodroid's getRawFields(RawLevel) that always returns the fields.
     */
    val rawFields: List<ListItemInterface>
        get() = listOf(
            ListItem(
                stringResource.getString(Res.string.opal_checksum),
                checksum.toString()
            )
        )
}
