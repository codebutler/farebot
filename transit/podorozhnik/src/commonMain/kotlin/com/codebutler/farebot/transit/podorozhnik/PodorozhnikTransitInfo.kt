/*
 * PodorozhnikTransitInfo.kt
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.podorozhnik

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.transit.podorozhnik.generated.resources.*
import kotlin.time.Instant

/**
 * Podorozhnik cards (Saint Petersburg, Russia).
 */
class PodorozhnikTransitInfo(
    override val serialNumber: String?,
    private val balanceValue: Int?,
    private val tripList: List<Trip>,
    private val groundCounter: Int?,
    private val subwayCounter: Int?,
) : TransitInfo() {
    override val cardName: FormattedString
        get() = FormattedString(Res.string.podorozhnik_card_name)

    override val trips: List<Trip> = tripList

    override val subscriptions: List<Subscription>? = null

    override val balance: TransitBalance?
        get() {
            val b = balanceValue ?: return null
            return TransitBalance(
                balance = TransitCurrency.RUB(b),
                name = FormattedString("Podorozhnik"),
            )
        }

    override val info: List<ListItemInterface>?
        get() {
            if (groundCounter == null || subwayCounter == null) return null
            return listOf(
                ListItem(
                    Res.string.podorozhnik_ground_trips,
                    groundCounter.toString(),
                ),
                ListItem(
                    Res.string.podorozhnik_subway_trips,
                    subwayCounter.toString(),
                ),
            )
        }

    companion object {
        /**
         * Podorozhnik epoch: 2010-01-01T00:00:00 Moscow time (UTC+3).
         * In UTC this is 2009-12-31T21:00:00Z.
         * The card stores timestamps in minutes from this epoch.
         */
        private val PODOROZHNIK_EPOCH = Instant.parse("2009-12-31T21:00:00Z")

        fun convertDate(mins: Int): Instant =
            Instant.fromEpochSeconds(PODOROZHNIK_EPOCH.epochSeconds + mins.toLong() * 60L)
    }
}
