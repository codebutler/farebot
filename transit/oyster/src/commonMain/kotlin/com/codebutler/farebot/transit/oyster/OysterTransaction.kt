/*
 * OysterTransaction.kt
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

import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.Transaction
import com.codebutler.farebot.transit.TransitCurrency
import kotlin.time.Instant

class OysterTransaction(
    override val timestamp: Instant,
) : Transaction() {
    // TODO: implement
    override val isTapOff: Boolean
        get() = false

    // TODO: implement
    override val fare: TransitCurrency?
        get() = null

    // TODO: implement
    override val isTapOn: Boolean
        get() = true

    // TODO: implement
    override fun isSameTrip(other: Transaction) = false

    companion object {
        internal fun parseAll(card: ClassicCard): List<OysterTransaction> {
            val result = mutableListOf<OysterTransaction>()
            for (sector in 9..13) {
                val sec = card.getSector(sector) as? DataClassicSector ?: continue
                for (block in 0..2) {
                    // invalid
                    if (block == 0 && sector == 9) continue
                    if (block >= sec.blocks.size) continue
                    result.add(
                        OysterTransaction(
                            OysterUtils.parseTimestamp(sec.getBlock(block).data, 6),
                        ),
                    )
                }
            }
            return result
        }
    }
}
