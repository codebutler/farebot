/*
 * ClipperUltralightSubscription.kt
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

package com.codebutler.farebot.transit.clipper

import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.transit.Subscription
import farebot.transit.clipper.generated.resources.Res
import farebot.transit.clipper.generated.resources.clipper_return
import farebot.transit.clipper.generated.resources.clipper_single
import farebot.transit.clipper.generated.resources.clipper_ticket_type_adult
import farebot.transit.clipper.generated.resources.clipper_ticket_type_rtc
import farebot.transit.clipper.generated.resources.clipper_ticket_type_senior
import farebot.transit.clipper.generated.resources.clipper_ticket_type_youth
import kotlin.time.Instant

class ClipperUltralightSubscription(
    private val product: Int,
    private val tripsRemaining: Int,
    private val transferExpiry: Int,
    private val baseDate: Int,
) : Subscription() {
    override val id: Int = 0

    override val subscriptionName: String
        get() =
            when (product and 0xf) {
                0x3 ->
                    getStringBlocking(
                        Res.string.clipper_single,
                        getStringBlocking(Res.string.clipper_ticket_type_adult),
                    )
                0x4 ->
                    getStringBlocking(
                        Res.string.clipper_return,
                        getStringBlocking(Res.string.clipper_ticket_type_adult),
                    )
                0x5 ->
                    getStringBlocking(
                        Res.string.clipper_single,
                        getStringBlocking(Res.string.clipper_ticket_type_senior),
                    )
                0x6 ->
                    getStringBlocking(
                        Res.string.clipper_return,
                        getStringBlocking(Res.string.clipper_ticket_type_senior),
                    )
                0x7 ->
                    getStringBlocking(
                        Res.string.clipper_single,
                        getStringBlocking(Res.string.clipper_ticket_type_rtc),
                    )
                0x8 ->
                    getStringBlocking(
                        Res.string.clipper_return,
                        getStringBlocking(Res.string.clipper_ticket_type_rtc),
                    )
                0x9 ->
                    getStringBlocking(
                        Res.string.clipper_single,
                        getStringBlocking(Res.string.clipper_ticket_type_youth),
                    )
                0xa ->
                    getStringBlocking(
                        Res.string.clipper_return,
                        getStringBlocking(Res.string.clipper_ticket_type_youth),
                    )
                else -> product.toString(16)
            }

    override val remainingTripCount: Int?
        get() = if (tripsRemaining == -1) null else tripsRemaining

    override val subscriptionState: SubscriptionState
        get() =
            when {
                tripsRemaining == -1 -> SubscriptionState.UNUSED
                tripsRemaining == 0 -> SubscriptionState.USED
                tripsRemaining > 0 -> SubscriptionState.STARTED
                else -> SubscriptionState.UNKNOWN
            }

    override val transferEndTimestamp: Instant?
        get() {
            val epoch = ClipperUtil.clipperTimestampToEpochSeconds(transferExpiry * 60L)
            return if (epoch > 0) Instant.fromEpochSeconds(epoch) else null
        }

    override val purchaseTimestamp: Instant?
        get() {
            val epoch = ClipperUtil.clipperTimestampToEpochSeconds((baseDate - 89) * 86400L)
            return if (epoch > 0) Instant.fromEpochSeconds(epoch) else null
        }

    override val validTo: Instant?
        get() {
            val expiryEpoch = ClipperUtil.clipperTimestampToEpochSeconds(baseDate * 86400L)
            return if (expiryEpoch > 0) Instant.fromEpochSeconds(expiryEpoch) else null
        }

    override val agencyName: String
        get() {
            val agencyCode = if (product shr 4 == 0x21) ClipperData.AGENCY_MUNI else product shr 4
            return ClipperData.getAgencyName(agencyCode)
        }

    override val shortAgencyName: String
        get() {
            val agencyCode = if (product shr 4 == 0x21) ClipperData.AGENCY_MUNI else product shr 4
            return ClipperData.getShortAgencyName(agencyCode)
        }

    override val machineId: Int = 0
}
