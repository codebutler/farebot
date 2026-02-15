/*
 * UltralightTypeRaw.kt
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

package com.codebutler.farebot.card.ultralight

import com.codebutler.farebot.base.util.hex

/**
 * Raw card type detection data from protocol commands.
 *
 * This is an internal data structure used to parse the responses from GET_VERSION
 * and AUTH_1 commands into a final [UltralightCard.UltralightType].
 */
internal data class UltralightTypeRaw(
    val versionCmd: ByteArray? = null,
    val repliesToAuth1: Boolean? = null,
) {
    /**
     * Parses the raw protocol responses into a card type.
     */
    fun parse(): UltralightCard.UltralightType {
        if (versionCmd != null) {
            if (versionCmd.size != 8) {
                println(
                    "UltralightTypeRaw: getVersion didn't return 8 bytes, got (${versionCmd.size} instead): ${versionCmd.hex()}",
                )
                return UltralightCard.UltralightType.UNKNOWN
            }

            if (versionCmd[2].toInt() == 0x04) {
                // Datasheet suggests we should do some maths here to allow for future card types,
                // however for all cards, we get an inexact data length. A locked page read does a
                // NAK, but an authorised read will wrap around to page 0x00.
                return when (versionCmd[6].toInt()) {
                    0x0F -> UltralightCard.UltralightType.NTAG213
                    0x11 -> UltralightCard.UltralightType.NTAG215
                    0x13 -> UltralightCard.UltralightType.NTAG216
                    else -> {
                        println(
                            "UltralightTypeRaw: getVersion returned unknown storage size (${versionCmd[6]}): ${versionCmd.hex()}",
                        )
                        UltralightCard.UltralightType.UNKNOWN
                    }
                }
            }

            if (versionCmd[2].toInt() != 0x03) {
                // TODO: PM3 notes that there are a number of NTAG which respond to this command, and look similar to EV1.
                println(
                    "UltralightTypeRaw: getVersion got a tag response with non-EV1 product code (${versionCmd[2]}): ${versionCmd.hex()}",
                )
                return UltralightCard.UltralightType.UNKNOWN
            }

            // EV1 version detection.
            //
            // Datasheet suggests we should do some maths here to allow for future card types,
            // however for the EV1_MF0UL11 we get an inexact data length. PM3 does the check this
            // way as well, and locked page reads all look the same.
            return when (versionCmd[6].toInt()) {
                0x0b -> UltralightCard.UltralightType.EV1_MF0UL11
                0x0e -> UltralightCard.UltralightType.EV1_MF0UL21
                else -> {
                    println(
                        "UltralightTypeRaw: getVersion returned unknown storage size (${versionCmd[6]}): ${versionCmd.hex()}",
                    )
                    UltralightCard.UltralightType.UNKNOWN
                }
            }
        }

        return when (repliesToAuth1) {
            // TODO: PM3 says NTAG 203 (with different memory size) also looks like this.
            false, null -> UltralightCard.UltralightType.MF0ICU1
            true -> UltralightCard.UltralightType.MF0ICU2
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as UltralightTypeRaw

        if (versionCmd != null) {
            if (other.versionCmd == null) return false
            if (!versionCmd.contentEquals(other.versionCmd)) return false
        } else if (other.versionCmd != null) {
            return false
        }
        if (repliesToAuth1 != other.repliesToAuth1) return false

        return true
    }

    override fun hashCode(): Int {
        var result = versionCmd?.contentHashCode() ?: 0
        result = 31 * result + (repliesToAuth1?.hashCode() ?: 0)
        return result
    }
}
