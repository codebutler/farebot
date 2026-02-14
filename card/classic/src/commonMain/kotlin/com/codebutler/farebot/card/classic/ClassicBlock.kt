/*
 * ClassicBlock.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2012, 2014, 2016 Eric Butler <eric@codebutler.com>
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
data class ClassicBlock(
    val type: String,
    val index: Int,
    @Contextual val data: ByteArray,
) {
    /**
     * Whether this block contains only zeros, 0xFF bytes, or is otherwise empty/unused.
     */
    val isEmpty: Boolean
        get() = data.all { it == 0.toByte() } || data.all { it == 0xFF.toByte() }

    companion object {
        const val TYPE_DATA = "data"
        const val TYPE_MANUFACTURER = "manufacturer"
        const val TYPE_TRAILER = "trailer"
        const val TYPE_VALUE = "value"

        fun create(
            type: String,
            index: Int,
            data: ByteArray,
        ): ClassicBlock = ClassicBlock(type, index, data)
    }
}
