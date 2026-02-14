/*
 * ISO7816File.kt
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google
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

package com.codebutler.farebot.card.iso7816

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Represents a file on an ISO 7816 smart card.
 *
 * @param binaryData Raw binary data of the file (from READ BINARY), or null if not available.
 * @param records Map of record index to record data (from READ RECORD).
 * @param fci File Control Information returned when the file was selected.
 */
@Serializable
data class ISO7816File(
    @Contextual val binaryData: ByteArray? = null,
    val records: Map<Int, @Contextual ByteArray> = emptyMap(),
    @Contextual val fci: ByteArray? = null,
) {
    val recordList: List<ByteArray>
        get() = records.entries.sortedBy { it.key }.map { it.value }

    fun getRecord(index: Int): ByteArray? = records[index]

    companion object {
        fun create(
            binaryData: ByteArray? = null,
            records: Map<Int, ByteArray> = emptyMap(),
            fci: ByteArray? = null,
        ): ISO7816File = ISO7816File(binaryData, records, fci)
    }
}
