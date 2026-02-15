/*
 * MspGotoTransitInfo.kt
 *
 * Copyright 2018-2019 Google
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

package com.codebutler.farebot.transit.mspgoto

import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.nextfare.NextfareTransitInfo
import com.codebutler.farebot.transit.nextfare.NextfareTransitInfoCapsule
import farebot.transit.msp_goto.generated.resources.Res
import farebot.transit.msp_goto.generated.resources.msp_goto_card_name
import com.codebutler.farebot.base.util.FormattedString

/**
 * Transit data type for Go-To card (Minneapolis / St. Paul, MN).
 * This is a Cubic Nextfare card using USD.
 *
 * Ported from Metrodroid.
 */
class MspGotoTransitInfo(
    capsule: NextfareTransitInfoCapsule,
) : NextfareTransitInfo(
        capsule = capsule,
        currencyFactory = { TransitCurrency.USD(it) },
    ) {
    override val cardName: FormattedString
        get() = FormattedString(Res.string.msp_goto_card_name)
}
