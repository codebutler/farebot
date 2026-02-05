/*
 * OysterPurse.kt
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

import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.base.util.getBitsFromBufferSignedLeBits
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.TransitCurrency

class OysterPurse(
    val value: Int,
    private val sequence: Int,
    private val subsequence: Int
) : Comparable<OysterPurse> {

    val balance: TransitCurrency
        get() = TransitCurrency.GBP(value)

    constructor(record: ByteArray) : this(
        subsequence = record.getBitsFromBuffer(4, 4),
        sequence = record.byteArrayToInt(1, 1),
        value = record.getBitsFromBufferSignedLeBits(25, 15)
    )

    override fun compareTo(other: OysterPurse): Int {
        val c = sequence.compareTo(other.sequence)
        return when {
            c != 0 -> c
            else -> subsequence.compareTo(other.subsequence)
        }
    }

    companion object {
        internal fun parse(a: ByteArray?, b: ByteArray?): OysterPurse? {
            val purseA = a?.let { OysterPurse(it) }
            val purseB = b?.let { OysterPurse(it) }
            return when {
                purseA == null -> purseB
                purseB == null -> purseA
                else -> maxOf(purseA, purseB)
            }
        }

        internal fun parse(card: ClassicCard): OysterPurse? {
            val sector1 = card.getSector(1) as? DataClassicSector ?: return null
            return parse(sector1.getBlock(1).data, sector1.getBlock(2).data)
        }
    }
}
