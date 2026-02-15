/*
 * RawClassicSector.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.card.classic.raw

import com.codebutler.farebot.card.classic.ClassicSector
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.card.classic.InvalidClassicSector
import com.codebutler.farebot.card.classic.UnauthorizedClassicSector
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class RawClassicSector(
    val type: String,
    val index: Int,
    val blocks: List<RawClassicBlock>? = null,
    @Contextual val keyA: ByteArray? = null,
    @Contextual val keyB: ByteArray? = null,
    val errorMessage: String? = null,
) {
    fun parse(): ClassicSector =
        when (type) {
            TYPE_DATA -> {
                val parsedBlocks = blocks!!.map { it.parse() }
                DataClassicSector.create(index, parsedBlocks)
            }
            TYPE_INVALID -> InvalidClassicSector.create(index, errorMessage!!)
            TYPE_UNAUTHORIZED -> UnauthorizedClassicSector.create(index)
            else -> throw RuntimeException("Unknown type")
        }

    companion object {
        const val TYPE_DATA = "data"
        const val TYPE_INVALID = "invalid"
        const val TYPE_UNAUTHORIZED = "unauthorized"

        fun createData(
            index: Int,
            blocks: List<RawClassicBlock>,
            keyA: ByteArray? = null,
            keyB: ByteArray? = null,
        ): RawClassicSector = RawClassicSector(TYPE_DATA, index, blocks, keyA, keyB, null)

        fun createInvalid(
            index: Int,
            errorMessage: String,
            keyA: ByteArray? = null,
            keyB: ByteArray? = null,
        ): RawClassicSector = RawClassicSector(TYPE_INVALID, index, null, keyA, keyB, errorMessage)

        fun createUnauthorized(
            index: Int,
            keyA: ByteArray? = null,
            keyB: ByteArray? = null,
        ): RawClassicSector = RawClassicSector(TYPE_UNAUTHORIZED, index, null, keyA, keyB, null)
    }
}
