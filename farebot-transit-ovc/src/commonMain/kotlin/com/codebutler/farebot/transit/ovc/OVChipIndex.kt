/*
 * OVChipIndex.kt
 *
 * Copyright 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright 2012 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.ovc

import com.codebutler.farebot.base.ui.HeaderListItem
import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.transit.en1545.getBitsFromBuffer
import kotlinx.serialization.Serializable

@ConsistentCopyVisibility
@Serializable
data class OVChipIndex internal constructor(
    val recentTransactionSlot: Boolean,  // Most recent transaction slot (0xFB0 (false) or 0xFD0 (true))
    val recentInfoSlot: Boolean,  // Most recent card information index slot (0x5C0 (true) or 0x580(false))
    val recentSubscriptionSlot: Boolean,   // Most recent subscription index slot (0xF10 (false) or 0xF30(true))
    val recentTravelhistorySlot: Boolean, // Most recent travel history index slot (0xF50 (false) or 0xF70 (true))
    val recentCreditSlot: Boolean,         // Most recent credit index slot (0xF90(false) or 0xFA0(true))
    val subscriptionIndex: List<Int>
) {
    fun getRawFields(): List<ListItemInterface> =
        listOf(
            HeaderListItem("Recent Slots"),
            ListItem("Transaction Slot", if (recentTransactionSlot) "B" else "A"),
            ListItem("Info Slot", if (recentInfoSlot) "B" else "A"),
            ListItem("Subscription Slot", if (recentSubscriptionSlot) "B" else "A"),
            ListItem("Travelhistory Slot", if (recentTravelhistorySlot) "B" else "A"),
            ListItem("Credit Slot", if (recentCreditSlot) "B" else "A"))

    companion object {
        fun parse(data: ByteArray): OVChipIndex {
            val firstSlot = data.copyOfRange(0, data.size / 2)
            val secondSlot = data.copyOfRange(data.size / 2, data.size)

            val iIDa3 = firstSlot.getBitsFromBuffer(10, 16)
            val iIDb3 = secondSlot.getBitsFromBuffer(10, 16)

            val buffer = if (iIDb3 > iIDa3) secondSlot else firstSlot

            val indexes = buffer.getBitsFromBuffer(31 * 8, 3)

            val subscriptionIndex = (0..11).map { i -> buffer.getBitsFromBuffer(108 + i * 4, 4) }

            return OVChipIndex(recentTransactionSlot = iIDb3 <= iIDa3,
                    recentSubscriptionSlot = indexes and 0x04 != 0x00,
                    recentTravelhistorySlot = indexes and 0x02 != 0x00,
                    recentCreditSlot = indexes and 0x01 != 0x00,
                    recentInfoSlot = buffer[3].toInt() shr 5 and 0x01 != 0,
                    subscriptionIndex = subscriptionIndex)
        }
    }
}
