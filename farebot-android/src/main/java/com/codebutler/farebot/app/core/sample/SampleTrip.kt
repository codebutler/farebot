/*
 * SampleTrip.kt
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

import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import kotlin.time.Instant
import java.util.Date

class SampleTrip(private val date: Date) : Trip() {

    override val startTimestamp: Instant get() = Instant.fromEpochMilliseconds(date.time)

    override val endTimestamp: Instant get() = Instant.fromEpochMilliseconds(date.time)

    override val routeName: String get() = "Route Name"

    override val agencyName: String get() = "Agency"

    override val shortAgencyName: String get() = "Agency"

    override val fare: TransitCurrency get() = TransitCurrency.USD(420)

    override val startStation: Station get() = Station.create("Name", "Name", "", "")

    override val endStation: Station get() = Station.create("Name", "Name", "", "")

    override val mode: Mode get() = Mode.METRO
}
