/*
 * DesfireAuthLog.kt
 *
 * Copyright 2018 Google
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

package com.codebutler.farebot.card.desfire

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DesfireAuthLog(
    @SerialName("key-id")
    val keyId: Int,
    val challenge: ByteArray,
    val response: ByteArray,
    val confirm: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DesfireAuthLog

        if (keyId != other.keyId) return false
        if (!challenge.contentEquals(other.challenge)) return false
        if (!response.contentEquals(other.response)) return false
        if (!confirm.contentEquals(other.confirm)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = keyId
        result = 31 * result + challenge.contentHashCode()
        result = 31 * result + response.contentHashCode()
        result = 31 * result + confirm.contentHashCode()
        return result
    }
}
