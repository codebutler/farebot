/*
 * FelicaService.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014, 2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.card.felica

import kotlinx.serialization.Serializable

@Serializable
data class FelicaService(
    val serviceCode: Int,
    val blocks: List<FelicaBlock>,
    val skipped: Boolean = false
) {
    /**
     * Get a block by its address.
     * Returns null if the block is not found.
     */
    fun getBlock(address: Int): FelicaBlock? =
        blocks.firstOrNull { it.address.toInt() == address }

    companion object {
        fun create(serviceCode: Int, blocks: List<FelicaBlock>): FelicaService {
            return FelicaService(serviceCode, blocks)
        }

        fun skipped(serviceCode: Int): FelicaService {
            return FelicaService(serviceCode, emptyList(), skipped = true)
        }
    }
}
