/*
 * MRTUltralightTransitFactory.kt
 *
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

package com.codebutler.farebot.transit.ultralight

import com.codebutler.farebot.card.ultralight.UltralightCard
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitInfo
import farebot.farebot_transit_ultralight.generated.resources.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

class MRTUltralightTransitFactory : TransitFactory<UltralightCard, MRTUltralightTransitInfo> {

    override fun check(card: UltralightCard): Boolean {
        val page3 = card.getPage(3).data
        return byteArrayToInt(page3, 0, 4) == 0x204f2400
    }

    override fun parseIdentity(card: UltralightCard): TransitIdentity {
        val serial = formatSerial(getSerial(card))
        return TransitIdentity.create(runBlocking { getString(Res.string.ultralight_mrt) }, serial)
    }

    override fun parseInfo(card: UltralightCard): MRTUltralightTransitInfo {
        return MRTUltralightTransitInfo(getSerial(card))
    }

    private fun getSerial(card: UltralightCard): Int {
        val page15 = card.getPage(15).data
        return byteArrayToInt(page15, 0, 4)
    }

    private fun byteArrayToInt(data: ByteArray, offset: Int, length: Int): Int {
        var result = 0
        for (i in 0 until length) {
            result = result shl 8
            result = result or (data[offset + i].toInt() and 0xFF)
        }
        return result
    }

    companion object {
        fun formatSerial(sn: Int): String {
            val formatted = sn.toLong().toString().padStart(12, '0')
            return "0001 ${formatted.substring(0, 4)} ${formatted.substring(4, 8)} ${formatted.substring(8, 12)}"
        }
    }
}

class MRTUltralightTransitInfo(
    private val serial: Int
) : TransitInfo() {
    override val cardName: String = runBlocking { getString(Res.string.ultralight_mrt) }

    override val serialNumber: String = MRTUltralightTransitFactory.formatSerial(serial)
}
