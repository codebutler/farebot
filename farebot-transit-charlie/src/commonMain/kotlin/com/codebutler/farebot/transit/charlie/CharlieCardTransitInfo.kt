/*
 * CharlieCardTransitInfo.kt
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

package com.codebutler.farebot.transit.charlie

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import farebot.farebot_transit_charlie.generated.resources.Res
import farebot.farebot_transit_charlie.generated.resources.charlie_2nd_card_number
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * CharlieCard, Boston, USA (MBTA).
 *
 * Epoch: 2003-01-01 00:00 in America/New_York (UTC-5), so 2003-01-01T05:00:00Z.
 * Timestamps are in minutes from this epoch.
 */

private val CHARLIE_EPOCH: Instant = // 2003-01-01 midnight in New York
    Instant.fromEpochMilliseconds(
        LocalDate(2003, 1, 1).atStartOfDayIn(TimeZone.of("America/New_York")).toEpochMilliseconds(),
    )

class CharlieCardTransitInfo internal constructor(
    private val serial: Long,
    private val secondSerial: Long,
    private val mBalance: Int,
    private val startDate: Int,
    override val trips: List<CharlieCardTrip>,
) : TransitInfo() {
    override val cardName: String = CharlieCardTransitFactory.NAME

    override val serialNumber: String = CharlieCardTransitFactory.formatSerial(serial)

    override val balances: List<TransitBalance>
        get() {
            val start = parseTimestamp(startDate)
            // After 2011, all cards expire 10 years after issue (11 years minus 1 day from epoch).
            // Using 11 * 365 days as an approximation (matching Metrodroid behavior).
            val expiry = start + (11 * 365).days
            // Cards not used for 2 years will also expire.
            val lastTrip = trips.flatMap { listOfNotNull(it.startTimestamp, it.endTimestamp) }.maxOrNull()
            val lastUseExpiry = lastTrip?.let { it + (2 * 365).days }

            val effectiveExpiry = if (lastUseExpiry != null && lastUseExpiry < expiry) lastUseExpiry else expiry

            return listOf(
                TransitBalance(
                    balance = TransitCurrency.USD(mBalance),
                    validFrom = start,
                    validTo = effectiveExpiry,
                ),
            )
        }

    override val subscriptions: List<Subscription>? = null

    override val hasUnknownStations: Boolean = true

    override val info: List<ListItemInterface>?
        get() =
            if (secondSerial == 0L || secondSerial == 0xffffffffL) {
                null
            } else {
                listOf(ListItem(Res.string.charlie_2nd_card_number, "A" + NumberUtils.zeroPad(secondSerial, 10)))
            }

    companion object {
        internal fun parseTimestamp(timestamp: Int): Instant = CHARLIE_EPOCH + timestamp.minutes
    }
}
