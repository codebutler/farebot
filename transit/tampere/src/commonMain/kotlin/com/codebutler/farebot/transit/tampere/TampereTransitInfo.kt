/*
 * TampereTransitInfo.kt
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

package com.codebutler.farebot.transit.tampere

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.transit.tampere.generated.resources.Res
import farebot.transit.tampere.generated.resources.tampere_card_name
import farebot.transit.tampere.generated.resources.tampere_cardholder_name
import farebot.transit.tampere.generated.resources.tampere_date_of_birth
import farebot.transit.tampere.generated.resources.tampere_issue_date
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class TampereTransitInfo(
    override val serialNumber: String?,
    private val mBalance: Int?,
    override val trips: List<Trip>?,
    override val subscriptions: List<TampereSubscription>?,
    private val mHolderName: String?,
    private val mHolderBirthDate: Int?,
    private val mIssueDate: Int?,
) : TransitInfo() {
    override val cardName: String = getStringBlocking(Res.string.tampere_card_name)

    override val balance: TransitBalance?
        get() = mBalance?.let { TransitBalance(balance = TransitCurrency.EUR(it)) }

    override val info: List<ListItemInterface>
        get() =
            listOfNotNull(
                if (mHolderName.isNullOrEmpty()) null else ListItem(Res.string.tampere_cardholder_name, mHolderName),
                if (mHolderBirthDate == 0 || mHolderBirthDate == null) {
                    null
                } else {
                    ListItem(Res.string.tampere_date_of_birth, parseDaystamp(mHolderBirthDate).toString())
                },
                ListItem(Res.string.tampere_issue_date, mIssueDate?.let { parseDaystamp(it) }?.toString()),
            )

    companion object {
        const val NAME = "Tampere"
        const val APP_ID = 0x121ef

        private val TZ = TimeZone.of("Europe/Helsinki")

        // Epoch is 1900-01-01 in Helsinki timezone.
        // We use the LocalDate approach to compute the Instant.
        private val EPOCH_DATE = LocalDate(1900, 1, 1)

        /**
         * Parses a day count (since 1900-01-01 local) into an Instant.
         */
        fun parseDaystamp(day: Int): Instant {
            val epochInstant = EPOCH_DATE.atStartOfDayIn(TZ)
            return epochInstant + (day.toLong()).days
        }

        /**
         * Parses a day + minute count (since 1900-01-01 local) into an Instant.
         */
        fun parseTimestamp(
            day: Int,
            minute: Int,
        ): Instant {
            val epochInstant = EPOCH_DATE.atStartOfDayIn(TZ)
            return epochInstant + (day.toLong()).days + minute.minutes
        }
    }
}
