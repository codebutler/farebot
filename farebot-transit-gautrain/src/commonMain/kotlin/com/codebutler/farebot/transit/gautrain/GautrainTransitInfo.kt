/*
 * GautrainTransitInfo.kt
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

package com.codebutler.farebot.transit.gautrain

import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.base.util.getBitsFromBufferSigned
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.en1545.En1545FixedInteger
import farebot.farebot_transit_gautrain.generated.resources.Res
import farebot.farebot_transit_gautrain.generated.resources.gautrain_card_name

/**
 * Gautrain (Gauteng, South Africa) transit card.
 *
 * Uses MIFARE Classic with OVChip-derived data format and EN1545 field encoding.
 */
class GautrainTransitInfo internal constructor(
    private val serial: Long,
    override val trips: List<Trip>,
    override val subscriptions: List<Subscription>,
    private val expdate: Int,
    private val mBalanceBlocks: List<GautrainBalanceBlock>,
) : TransitInfo() {
    override val cardName: String
        get() = getStringBlocking(Res.string.gautrain_card_name)

    override val serialNumber: String = formatSerial(serial)

    override val balances: List<TransitBalance>
        get() {
            val maxBal = mBalanceBlocks.maxByOrNull { it.txn }
            val currency = GautrainLookup.parseCurrency(maxBal?.balance ?: 0)
            val expiry = En1545FixedInteger.parseDate(expdate, GautrainLookup.timeZone)
            return listOf(
                TransitBalance(
                    balance = currency,
                    validTo = expiry,
                ),
            )
        }

    companion object {
        internal fun formatSerial(serial: Long) = NumberUtils.zeroPad(serial, 10)
    }
}

/**
 * Parsed balance block from sector 39 of a Gautrain card.
 * Contains balance value and transaction sequence number.
 */
internal data class GautrainBalanceBlock(
    val balance: Int,
    val txn: Int,
) {
    companion object {
        fun parse(input: ByteArray): GautrainBalanceBlock =
            GautrainBalanceBlock(
                balance = input.getBitsFromBufferSigned(75, 16) xor 0x7fff.inv(),
                txn = input.getBitsFromBuffer(30, 16),
            )
    }
}

/**
 * Minimal OVChip index parser for Gautrain.
 * Matches Metrodroid's OVChipIndex format.
 */
internal data class GautrainIndex(
    val recentSubscriptionSlot: Boolean,
    val subscriptionIndex: List<Int>,
) {
    companion object {
        fun parse(buffer: ByteArray): GautrainIndex {
            val firstSlot = buffer.copyOfRange(0, buffer.size / 2)
            val secondSlot = buffer.copyOfRange(buffer.size / 2, buffer.size)

            val iIDa3 = firstSlot.getBitsFromBuffer(10, 16)
            val iIDb3 = secondSlot.getBitsFromBuffer(10, 16)

            val recent = if (iIDb3 > iIDa3) secondSlot else firstSlot
            val indexes = recent.getBitsFromBuffer(31 * 8, 3)
            val subscriptionIndex =
                (0..11).map { i ->
                    recent.getBitsFromBuffer(108 + i * 4, 4)
                }

            return GautrainIndex(
                recentSubscriptionSlot = indexes and 0x04 != 0x00,
                subscriptionIndex = subscriptionIndex,
            )
        }
    }
}
