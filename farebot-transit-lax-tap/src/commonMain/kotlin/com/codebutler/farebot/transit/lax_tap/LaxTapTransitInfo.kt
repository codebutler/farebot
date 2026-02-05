/*
 * LaxTapTransitInfo.kt
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.lax_tap

import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.nextfare.NextfareTransitInfo
import com.codebutler.farebot.transit.nextfare.NextfareTransitInfoCapsule
import farebot.farebot_transit_lax_tap.generated.resources.Res
import farebot.farebot_transit_lax_tap.generated.resources.lax_tap_card_name
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

/**
 * Los Angeles Transit Access Pass (LAX TAP) card.
 * https://github.com/micolous/metrodroid/wiki/Transit-Access-Pass
 */
class LaxTapTransitInfo(
    capsule: NextfareTransitInfoCapsule
) : NextfareTransitInfo(capsule, currencyFactory = { TransitCurrency.USD(it) }) {

    override val cardName: String
        get() = runBlocking { getString(Res.string.lax_tap_card_name) }

    companion object {
        val NAME: String
            get() = runBlocking { getString(Res.string.lax_tap_card_name) }
    }
}
