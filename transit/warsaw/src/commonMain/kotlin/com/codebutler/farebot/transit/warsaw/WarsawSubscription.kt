/*
 * WarsawSubscription.kt
 *
 * Copyright 2018 Google
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

package com.codebutler.farebot.transit.warsaw

import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.base.util.hexString
import com.codebutler.farebot.transit.Subscription
import farebot.transit.warsaw.generated.resources.Res
import farebot.transit.warsaw.generated.resources.warsaw_90_days
import farebot.transit.warsaw.generated.resources.warsaw_unknown
import kotlin.time.Instant

class WarsawSubscription(
    private val validToInstant: Instant,
    private val ticketType: Int,
) : Subscription() {
    override val validTo: Instant get() = validToInstant

    override val subscriptionName: FormattedString
        get() =
            when (ticketType) {
                0xbf6 -> FormattedString(Res.string.warsaw_90_days)
                else -> FormattedString(Res.string.warsaw_unknown, ticketType.hexString)
            }
}
