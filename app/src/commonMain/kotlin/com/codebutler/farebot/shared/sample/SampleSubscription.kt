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

package com.codebutler.farebot.shared.sample

import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.transit.Subscription
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.time.Instant

class SampleSubscription : Subscription() {
    override val id: Int = 1

    override val validFrom: Instant get() = LocalDate(2017, 6, 1).atStartOfDayIn(TimeZone.UTC)

    override val validTo: Instant get() = LocalDate(2017, 7, 1).atStartOfDayIn(TimeZone.UTC)

    override val agencyName: FormattedString? get() = FormattedString("Municipal Robot Railway")

    override val shortAgencyName: FormattedString? get() = FormattedString("Muni")

    override val machineId: Int = 1

    override val subscriptionName: FormattedString? get() = FormattedString("Monthly Pass")
}
