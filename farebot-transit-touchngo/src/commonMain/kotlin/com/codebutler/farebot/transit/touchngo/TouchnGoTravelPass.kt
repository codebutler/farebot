/*
 * TouchnGoTravelPass.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright 2019 Google
 * Copyright 2025 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.touchngo

import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.transit.Subscription
import farebot.farebot_transit_touchngo.generated.resources.Res
import farebot.farebot_transit_touchngo.generated.resources.touchngo_travel_pass
import kotlin.time.Instant
import kotlin.time.Duration.Companion.days

/**
 * Represents a Touch 'n Go travel pass subscription (valid for 1 day).
 */
internal class TouchnGoTravelPass(
    override val validFrom: Instant
) : Subscription() {

    override val validTo: Instant
        get() = validFrom + 1.days

    override val subscriptionName: String
        get() = getStringBlocking(Res.string.touchngo_travel_pass)
}
