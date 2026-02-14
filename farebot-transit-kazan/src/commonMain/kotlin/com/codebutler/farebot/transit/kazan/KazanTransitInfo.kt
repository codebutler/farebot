/*
 * KazanTransitInfo.kt
 *
 * Copyright 2018 Google
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

package com.codebutler.farebot.transit.kazan

import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.farebot_transit_kazan.generated.resources.Res
import farebot.farebot_transit_kazan.generated.resources.card_name_kazan

class KazanTransitInfo(
    private val mSerial: Long,
    private val mSub: KazanSubscription,
    private val mTrip: KazanTrip?
) : TransitInfo() {

    override val serialNumber: String
        get() = NumberUtils.zeroPad(mSerial, 10)

    override val balance: TransitBalance?
        get() = mSub.balance

    override val subscriptions: List<Subscription>?
        get() = if (!mSub.isPurse) listOf(mSub) else null

    // Apparently subscriptions do not record trips
    override val trips: List<Trip>?
        get() = mTrip?.let { listOf(it) }

    override val cardName: String
        get() = getStringBlocking(Res.string.card_name_kazan)
}
