/*
 * VicinityPage.kt
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.card.vicinity

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * A single page of data on an NFC-V (ISO 15693) Vicinity card.
 */
@Serializable
data class VicinityPage(
    val index: Int,
    @Contextual val data: ByteArray,
    val isUnauthorized: Boolean = false,
) {
    companion object {
        fun create(
            index: Int,
            data: ByteArray,
        ): VicinityPage = VicinityPage(index, data)

        fun unauthorized(index: Int): VicinityPage = VicinityPage(index, ByteArray(0), isUnauthorized = true)
    }
}
