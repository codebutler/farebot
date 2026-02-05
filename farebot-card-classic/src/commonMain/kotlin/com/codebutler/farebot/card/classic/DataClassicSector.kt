/*
 * DataClassicSector.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
 *
 * Contains improvements ported from Metrodroid (https://github.com/metrodroid/metrodroid)
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

package com.codebutler.farebot.card.classic

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class DataClassicSector(
    override val index: Int,
    val blocks: List<ClassicBlock>,
    @Contextual val keyA: ByteArray? = null,
    @Contextual val keyB: ByteArray? = null
) : ClassicSector {

    fun getBlock(index: Int): ClassicBlock = blocks[index]

    fun readBlocks(startBlock: Int, blockCount: Int): kotlin.ByteArray {
        var readBlocks = 0
        val data = kotlin.ByteArray(blockCount * 16)
        for (i in startBlock until (startBlock + blockCount)) {
            val blockData = getBlock(i).data
            blockData.copyInto(data, readBlocks * 16)
            readBlocks++
        }
        return data
    }

    /**
     * Access bits parsed from the sector trailer block (block 3 of a standard sector).
     * Returns null if the sector has no trailer block.
     */
    val accessBits: ClassicAccessBits?
        get() {
            val trailer = blocks.lastOrNull() ?: return null
            if (trailer.type != ClassicBlock.TYPE_TRAILER) return null
            if (trailer.data.size < 10) return null
            return ClassicAccessBits(trailer.data.copyOfRange(6, 9))
        }

    companion object {
        fun create(
            sectorIndex: Int,
            classicBlocks: List<ClassicBlock>,
            keyA: ByteArray? = null,
            keyB: ByteArray? = null
        ): ClassicSector =
            DataClassicSector(sectorIndex, classicBlocks, keyA, keyB)
    }
}
