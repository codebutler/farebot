/*
 * KievTransitInfo.kt
 *
 * Copyright 2018 Google
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

package com.codebutler.farebot.transit.kiev

import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.transit.TransitInfo
import farebot.farebot_transit_kiev.generated.resources.Res
import farebot.farebot_transit_kiev.generated.resources.kiev_card_name

class KievTransitInfo(
    private val mSerial: String,
    override val trips: List<KievTrip>,
) : TransitInfo() {
    override val serialNumber: String
        get() = formatSerial(mSerial)

    override val cardName: String
        get() = getStringBlocking(Res.string.kiev_card_name)

    companion object {
        val NAME: String
            get() = getStringBlocking(Res.string.kiev_card_name)

        fun formatSerial(serial: String): String = NumberUtils.groupString(serial, " ", 4, 4, 4)
    }
}
