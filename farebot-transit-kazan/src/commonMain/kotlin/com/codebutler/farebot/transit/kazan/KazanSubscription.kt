/*
 * KazanSubscription.kt
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

import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import farebot.farebot_transit_kazan.generated.resources.Res
import farebot.farebot_transit_kazan.generated.resources.kazan_blank
import farebot.farebot_transit_kazan.generated.resources.kazan_unknown_type
import farebot.farebot_transit_kazan.generated.resources.kazan_unknown_unlimited
import kotlinx.coroutines.runBlocking
import kotlin.time.Instant
import org.jetbrains.compose.resources.getString

class KazanSubscription(
    override val validFrom: Instant?,
    override val validTo: Instant?,
    private val mType: Int,
    private val mCounter: Int
) : Subscription() {

    val isPurse: Boolean get() = mType == 0x53

    val isUnlimited: Boolean get() = mType in listOf(0, 0x60)

    val balance: TransitBalance?
        get() = if (!isPurse) null else
            TransitBalance(
                balance = TransitCurrency.RUB(mCounter * 100),
                validFrom = validFrom,
                validTo = validTo
            )

    override val remainingTripCount: Int?
        get() = if (isUnlimited) null else mCounter

    override val subscriptionName: String
        get() = runBlocking {
            when (mType) {
                0 -> getString(Res.string.kazan_blank)
                // Could be unlimited buses, unlimited tram, unlimited trolleybus
                // or unlimited tram+trolleybus
                0x60 -> getString(Res.string.kazan_unknown_unlimited)
                else -> getString(Res.string.kazan_unknown_type, mType.toString(16))
            }
        }
}
