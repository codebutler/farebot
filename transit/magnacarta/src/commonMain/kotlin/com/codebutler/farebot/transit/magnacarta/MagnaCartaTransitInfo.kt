/*
 * MagnaCartaTransitInfo.kt
 *
 * Copyright 2019 Google
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

package com.codebutler.farebot.transit.magnacarta

import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.transit.magnacarta.generated.resources.Res
import farebot.transit.magnacarta.generated.resources.magnacarta_card_name
import com.codebutler.farebot.base.util.FormattedString

class MagnaCartaTransitInfo(
    private val mBalance: Int?, // cents
) : TransitInfo() {
    override val cardName: FormattedString = FormattedString(Res.string.magnacarta_card_name)

    override val serialNumber: String? = null

    override val balance: TransitBalance?
        get() = mBalance?.let { TransitBalance(balance = TransitCurrency.EUR(it)) }

    override val trips: List<Trip>? = null

    override val subscriptions: List<Subscription>? = null

    companion object {
        const val NAME = "MagnaCarta"
        const val APP_ID_BALANCE = 0xf080f3
    }
}
