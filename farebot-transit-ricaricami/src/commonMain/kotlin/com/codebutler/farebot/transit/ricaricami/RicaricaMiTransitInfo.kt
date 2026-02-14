/*
 * RicaricaMiTransitInfo.kt
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

package com.codebutler.farebot.transit.ricaricami

import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.en1545.En1545Parsed
import farebot.farebot_transit_ricaricami.generated.resources.Res
import farebot.farebot_transit_ricaricami.generated.resources.ricaricami_card_name

class RicaricaMiTransitInfo(
    override val trips: List<Trip>,
    override val subscriptions: List<Subscription>,
    private val ticketEnvParsed: En1545Parsed,
    private val contractList1: En1545Parsed,
    private val contractList2: En1545Parsed
) : TransitInfo() {

    override val serialNumber: String? get() = null

    override val cardName: String get() = getStringBlocking(Res.string.ricaricami_card_name)
}
