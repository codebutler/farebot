/*
 * KROCAPTransitInfo.kt
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

package com.codebutler.farebot.transit.krocap

import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.card.iso7816.ISO7816TLV
import com.codebutler.farebot.card.ksx6924.KROCAPData
import com.codebutler.farebot.transit.serialonly.SerialOnlyTransitInfo
import kotlinx.serialization.Serializable

/**
 * Reader for South Korean One Card All Pass Config DF FCI.
 *
 * This is only used as a fall-back if KSX6924Application is not available.
 * See [KROCAPTransitFactory] for selection logic.
 *
 * Reference: https://github.com/micolous/metrodroid/wiki/South-Korea#a0000004520001
 */
@Serializable
data class KROCAPTransitInfo(
    private val pdata: ByteArray,
) : SerialOnlyTransitInfo() {
    override val reason: Reason
        get() = Reason.MORE_RESEARCH_NEEDED

    override val serialNumber: String?
        get() = getSerial(pdata)

    override val cardName: FormattedString
        get() = NAME

    override val extraInfo: List<ListItemInterface>?
        get() = ISO7816TLV.infoBerTLV(pdata, KROCAPData.TAGMAP)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as KROCAPTransitInfo
        return pdata.contentEquals(other.pdata)
    }

    override fun hashCode(): Int = pdata.contentHashCode()

    companion object {
        val NAME = FormattedString("One Card All Pass")

        @OptIn(ExperimentalStdlibApi::class)
        private fun getSerial(pdata: ByteArray): String? {
            val tagBytes = KROCAPData.TAG_SERIAL_NUMBER.hexToByteArray()
            return ISO7816TLV.findBERTLV(pdata, tagBytes, false)?.toHexString()
        }
    }
}
