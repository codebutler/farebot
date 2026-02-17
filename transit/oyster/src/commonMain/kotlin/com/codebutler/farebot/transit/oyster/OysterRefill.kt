/*
 * OysterRefill.kt
 *
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.oyster

import com.codebutler.farebot.base.util.getBitsFromBufferLeBits
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import kotlin.time.Instant

class OysterRefill(
    override val startTimestamp: Instant,
    private val amount: Int,
) : Trip() {
    override val fare: TransitCurrency
        get() = TransitCurrency.GBP(-amount)

    override val mode: Mode
        get() = Mode.TICKET_MACHINE

    companion object {
        internal fun parseAll(card: ClassicCard): List<OysterRefill> {
            val result = mutableListOf<OysterRefill>()
            val sector5 = card.getSector(5) as? DataClassicSector ?: return result
            for (block in 0..2) {
                if (block >= sector5.blocks.size) continue
                val data = sector5.getBlock(block).data
                result.add(
                    OysterRefill(
                        startTimestamp = OysterUtils.parseTimestamp(data),
                        // estimate: max top-up requires 14 bits
                        amount = data.getBitsFromBufferLeBits(74, 14),
                    ),
                )
            }
            return result
        }
    }
}
