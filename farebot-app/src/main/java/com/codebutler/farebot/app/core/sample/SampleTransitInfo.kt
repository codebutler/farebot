/*
 * SampleTransitInfo.kt
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

package com.codebutler.farebot.app.core.sample

import android.content.Context
import android.content.res.Resources
import com.codebutler.farebot.app.core.kotlin.date
import com.codebutler.farebot.base.ui.FareBotUiTree
import com.codebutler.farebot.transit.Refill
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip

class SampleTransitInfo : TransitInfo() {

    override fun getBalanceString(resources: Resources): String = "$42.50"

    override fun getSerialNumber(): String? = "1234567890"

    override fun getTrips(): List<Trip> = listOf<Trip>(
            SampleTrip(date(2017, 6, 4, 19, 0)),
            SampleTrip(date(2017, 6, 5, 8, 0)),
            SampleTrip(date(2017, 6, 5, 16, 9))
    )

    override fun getRefills(): List<Refill> = listOf<Refill>(
            SampleRefill(date(2017, 6, 5, 16, 4))
    )

    override fun getSubscriptions(): List<Subscription> = listOf(
            SampleSubscription()
    )

    override fun getCardName(resources: Resources): String = "Sample Transit"

    override fun getAdvancedUi(context: Context): FareBotUiTree {
        val uiBuilder = FareBotUiTree.builder(context)

        val section1Builder = uiBuilder.item().title("Sample Card Section 1")

        section1Builder.item()
                .title("Example Item 1")
                .value("Value")

        section1Builder.item()
                .title("Example Item 2")
                .value("Value")

        val section2Builder = section1Builder.item().title("Sample Card Section 2")

        section2Builder.item()
                .title("Example Item 1")
                .value("Value")

        return uiBuilder.build()
    }
}

