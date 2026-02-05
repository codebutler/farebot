/*
 * RawCEPASPurse.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
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

package com.codebutler.farebot.card.cepas.raw

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import com.codebutler.farebot.card.cepas.CEPASPurse

@Serializable
data class RawCEPASPurse(
    val id: Int,
    @Contextual val data: ByteArray?,
    val errorMessage: String?
) {
    val isValid: Boolean
        get() = data != null

    fun parse(): CEPASPurse {
        if (isValid) {
            return CEPASPurse.create(id, data!!)
        }
        return CEPASPurse.create(id, errorMessage!!)
    }

    fun logfileRecordCount(): Byte = data!![40]

    companion object {
        fun create(id: Int, data: ByteArray): RawCEPASPurse {
            return RawCEPASPurse(id, data, null)
        }

        fun create(id: Int, errorMessage: String): RawCEPASPurse {
            return RawCEPASPurse(id, null, errorMessage)
        }
    }
}
