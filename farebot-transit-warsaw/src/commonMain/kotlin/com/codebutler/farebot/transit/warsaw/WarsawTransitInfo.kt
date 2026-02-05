/*
 * WarsawTransitInfo.kt
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

package com.codebutler.farebot.transit.warsaw

import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.farebot_transit_warsaw.generated.resources.Res
import farebot.farebot_transit_warsaw.generated.resources.warsaw_card_name
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

class WarsawTransitInfo(
    private val serial: Pair<Int, Int>,
    private val sectorA: WarsawSector,
    private val sectorB: WarsawSector
) : TransitInfo() {

    companion object {
        val NAME: String get() = runBlocking { getString(Res.string.warsaw_card_name) }
    }

    override val serialNumber: String
        get() = NumberUtils.zeroPad(serial.first, 3) + " " +
                NumberUtils.zeroPad(serial.second, 8)

    override val cardName: String get() = runBlocking { getString(Res.string.warsaw_card_name) }

    override val trips: List<Trip>?
        get() = listOfNotNull(sectorA.trip, sectorB.trip).ifEmpty { null }

    override val subscriptions: List<Subscription>?
        get() = listOfNotNull(maxOf(sectorA, sectorB).subscription).ifEmpty { null }
}
