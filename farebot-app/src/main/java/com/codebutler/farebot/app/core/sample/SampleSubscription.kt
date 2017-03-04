/*
 * SampleSubscription.kt
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
import com.codebutler.farebot.app.core.kotlin.date
import com.codebutler.farebot.transit.Subscription
import java.util.Date

class SampleSubscription : Subscription() {

    override fun getId(): Int = 1

    override fun getValidFrom(): Date = date(2017, 6)

    override fun getValidTo(): Date = date(2017, 7)

    override fun getAgencyName(resources: Resources): String = "Municipal Robot Railway"

    override fun getShortAgencyName(resources: Resources): String = "Robots on Rails"

    override fun getMachineId(): Int = 1

    override fun getSubscriptionName(resources: Resources): String = "Monthly Pass"

    override fun getActivation(): String = ""
}
