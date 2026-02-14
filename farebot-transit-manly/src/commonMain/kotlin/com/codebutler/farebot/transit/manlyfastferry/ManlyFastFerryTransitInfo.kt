/*
 * ManlyFastFerryTransitInfo.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.manlyfastferry

import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.erg.ErgTransitInfo
import com.codebutler.farebot.transit.erg.ErgTransitInfoCapsule
import farebot.farebot_transit_manly.generated.resources.Res
import farebot.farebot_transit_manly.generated.resources.manly_card_name

/**
 * Transit data type for Manly Fast Ferry Smartcard (Sydney, AU).
 *
 * This transit card is a system made by ERG Group (now Videlli Limited / Vix Technology).
 *
 * Note: This is a distinct private company who run their own ferry service to Manly, separate to
 * Transport for NSW's Manly Ferry service.
 *
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/Manly-Fast-Ferry
 */
class ManlyFastFerryTransitInfo(
    capsule: ErgTransitInfoCapsule,
) : ErgTransitInfo(capsule, { TransitCurrency.AUD(it) }) {
    override val cardName: String
        get() = getStringBlocking(Res.string.manly_card_name)

    companion object {
        internal const val AGENCY_ID = 0x0227
    }
}
