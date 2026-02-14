/*
 * EZLinkTransitInfo.kt
 *
 * Copyright 2011 Sean Cross <sean@chumby.com>
 * Copyright 2011-2012 Eric Butler <eric@codebutler.com>
 * Copyright 2012 Toby Bonang
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

package com.codebutler.farebot.transit.ezlink

import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip

class EZLinkTransitInfo(
    override val serialNumber: String?,
    private val mBalance: Int?,
    override val trips: List<Trip>,
    private val stringResource: StringResource,
) : TransitInfo() {
    override val cardName: String
        get() = EZLinkData.getCardIssuer(serialNumber, stringResource)

    // This is stored in cents of SGD
    override val balance: TransitBalance?
        get() = if (mBalance != null) TransitBalance(balance = TransitCurrency.SGD(mBalance)) else null
}
