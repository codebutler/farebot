/*
 * TampereSubscription.kt
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

package com.codebutler.farebot.transit.tampere

import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.transit.Subscription
import farebot.transit.tampere.generated.resources.Res
import farebot.transit.tampere.generated.resources.tampere_subscription
import kotlin.time.Instant

class TampereSubscription(
    private val mStart: Int? = null,
    private val mEnd: Int? = null,
    private val mType: Int,
) : Subscription() {
    override val validFrom: Instant?
        get() = mStart?.let { TampereTransitInfo.parseDaystamp(it) }

    override val validTo: Instant?
        get() = mEnd?.let { TampereTransitInfo.parseDaystamp(it) }

    override val subscriptionName: FormattedString
        get() = FormattedString(Res.string.tampere_subscription, mType.toString())
}
