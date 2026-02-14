/*
 * IntercodeLookupGironde.kt
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

package com.codebutler.farebot.transit.calypso.intercode

import com.codebutler.farebot.base.util.getStringBlocking
import farebot.farebot_transit_calypso.generated.resources.Res
import farebot.farebot_transit_calypso.generated.resources.card_name_transgironde
import farebot.farebot_transit_calypso.generated.resources.gironde_line

internal object IntercodeLookupGironde : IntercodeLookupSTR("gironde"), IntercodeLookupSingle {
    override val cardName: String = getStringBlocking(Res.string.card_name_transgironde)

    override fun getRouteName(
        routeNumber: Int?,
        routeVariant: Int?,
        agency: Int?,
        transport: Int?,
    ): String? {
        if (routeNumber == null) {
            return null
        }
        if (agency == TRANSGIRONDE) {
            return getStringBlocking(Res.string.gironde_line, routeNumber)
        }
        return super.getRouteName(routeNumber, routeNumber, agency, transport)
    }

    private const val TRANSGIRONDE = 16
}
