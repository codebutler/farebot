/*
 * AtHopTransitFactory.kt
 *
 * Copyright 2015 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.serialonly

import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.card.desfire.DesfireCard
import com.codebutler.farebot.card.desfire.StandardDesfireFile
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity

class AtHopTransitFactory : TransitFactory<DesfireCard, AtHopTransitInfo> {

    companion object {
        private const val APP_ID_SERIAL = 0xffffff
        internal const val NAME = "AT HOP"

        internal fun getSerial(card: DesfireCard): Int? {
            val file = card.getApplication(APP_ID_SERIAL)?.getFile(8) as? StandardDesfireFile
                ?: return null
            return file.data.getBitsFromBuffer(61, 32)
        }

        internal fun formatSerial(serial: Int?): String? =
            if (serial != null)
                "7824 6702 " + NumberUtils.formatNumber(serial.toLong(), " ", 4, 4, 3)
            else
                null
    }

    override fun check(card: DesfireCard): Boolean =
        card.getApplication(0x4055) != null && card.getApplication(APP_ID_SERIAL) != null

    override fun parseIdentity(card: DesfireCard): TransitIdentity =
        TransitIdentity.create(NAME, formatSerial(getSerial(card)))

    override fun parseInfo(card: DesfireCard): AtHopTransitInfo =
        AtHopTransitInfo(mSerial = getSerial(card))
}
