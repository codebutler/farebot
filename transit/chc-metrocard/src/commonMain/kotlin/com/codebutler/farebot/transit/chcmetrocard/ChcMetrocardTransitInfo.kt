/*
 * ChcMetrocardTransitInfo.kt
 *
 * Copyright 2018-2019 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.chcmetrocard

import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.erg.ErgTransitInfo
import com.codebutler.farebot.transit.erg.ErgTransitInfoCapsule
import farebot.transit.chc_metrocard.generated.resources.Res
import farebot.transit.chc_metrocard.generated.resources.chc_metrocard_card_name

/**
 * Transit data type for pre-2016 Metrocard (Christchurch, NZ).
 *
 * This transit card is a system made by ERG Group (now Videlli Limited / Vix Technology).
 *
 * The post-2016 version of this card is a DESFire card made by INIT.
 *
 * Documentation: https://github.com/micolous/metrodroid/wiki/Metrocard-%28Christchurch%29
 */
class ChcMetrocardTransitInfo(
    capsule: ErgTransitInfoCapsule,
) : ErgTransitInfo(capsule, { TransitCurrency.NZD(it) }) {
    override val cardName: FormattedString
        get() = FormattedString(Res.string.chc_metrocard_card_name)

    override val serialNumber: String?
        get() = capsule.cardSerial?.let { internalFormatSerialNumber(it) }

    companion object {
        internal const val AGENCY_ID = 0x0136

        private fun internalFormatSerialNumber(serial: ByteArray): String {
            var result = 0
            for (b in serial) {
                result = (result shl 8) or (b.toInt() and 0xFF)
            }
            return result.toString()
        }
    }
}
