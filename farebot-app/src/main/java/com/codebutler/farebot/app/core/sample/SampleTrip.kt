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

import android.content.res.Resources
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.Trip
import java.util.Date

class SampleTrip(val date : Date) : Trip() {

    override fun getTimestamp(): Long = date.time / 1000

    override fun getExitTimestamp(): Long = date.time / 1000

    override fun getRouteName(resources: Resources): String? = "Route Name"

    override fun getAgencyName(resources: Resources): String? = "Agency"

    override fun getShortAgencyName(resources: Resources): String? = "Agency"

    override fun getBalanceString(): String? = "$42.000"

    override fun getStartStationName(resources: Resources): String? = "Start Station"

    override fun getStartStation(): Station? = Station.create("Name", "Name", "", "")

    override fun hasFare(): Boolean = true

    override fun getFareString(resources: Resources): String? = "$4.20"

    override fun getEndStationName(resources: Resources): String? = "End Station"

    override fun getEndStation(): Station? = Station.create("Name", "Name", "", "")

    override fun getMode(): Mode? = Mode.METRO

    override fun hasTime(): Boolean = true
}
