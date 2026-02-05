/*
 * SnapperTransitInfo.kt
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
 *
 * Reference: https://github.com/micolous/metrodroid/wiki/Snapper
 */

package com.codebutler.farebot.transit.snapper

import com.codebutler.farebot.transit.serialonly.SerialOnlyTransitInfo
import farebot.farebot_transit_snapper.generated.resources.Res
import farebot.farebot_transit_snapper.generated.resources.snapper_card_name
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

/**
 * Transit data type for Snapper cards (Wellington, New Zealand).
 *
 * Snapper is a contactless smart card used for public transit in Wellington.
 * It uses the KSX6924 (T-Money compatible) protocol.
 *
 * Full balance and trip data requires KSX6924 protocol support which is
 * not yet available in FareBot.
 *
 * Ported from Metrodroid.
 */
class SnapperTransitInfo : SerialOnlyTransitInfo() {

    override val cardName: String
        get() = runBlocking { getString(Res.string.snapper_card_name) }

    override val serialNumber: String? = null

    override val reason: Reason = Reason.MORE_RESEARCH_NEEDED

    companion object {
        fun getCardName(): String = runBlocking { getString(Res.string.snapper_card_name) }
    }
}
