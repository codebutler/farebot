/*
 * OpalTrip.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2025 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.opal

import com.codebutler.farebot.transit.Trip
import farebot.transit.opal.generated.resources.Res
import farebot.transit.opal.generated.resources.opal_agency_tfnsw
import farebot.transit.opal.generated.resources.opal_agency_tfnsw_short
import kotlin.time.Instant
import com.codebutler.farebot.base.util.FormattedString

class OpalTrip(
    private val timestamp: Instant,
    private val transactionMode: Int,
    private val transactionType: Int,
) : Trip() {
    override val startTimestamp: Instant = timestamp

    override val mode: Mode
        get() =
            when (transactionMode) {
                OpalData.MODE_RAIL -> Mode.TRAIN
                OpalData.MODE_BUS -> Mode.BUS
                else -> Mode.FERRY // MODE_FERRY_LR
            }

    override val agencyName: FormattedString
        get() = FormattedString(Res.string.opal_agency_tfnsw)

    override val shortAgencyName: FormattedString
        get() = FormattedString(Res.string.opal_agency_tfnsw_short)

    override val routeName: FormattedString
        get() = OpalData.getLocalisedAction(transactionType)

    override val isTransfer: Boolean
        get() =
            transactionType in
                listOf(
                    0x02, // ACTION_TRANSFER_SAME_MODE
                    0x03, // ACTION_TRANSFER_DIFF_MODE
                    0x05, // ACTION_MANLY_TRANSFER_SAME_MODE
                    0x06, // ACTION_MANLY_TRANSFER_DIFF_MODE
                )

    override val isRejected: Boolean
        get() = transactionType == 0x0c // ACTION_TAP_ON_REJECTED
}
