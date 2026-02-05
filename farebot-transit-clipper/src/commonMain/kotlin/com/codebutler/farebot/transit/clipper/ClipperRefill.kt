/*
 * ClipperRefill.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014, 2016 Eric Butler <eric@codebutler.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.clipper

import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import kotlin.time.Instant

/**
 * Represents a refill (add-value) transaction on a Clipper card.
 *
 * In Metrodroid's architecture (which we follow), refills are a type of Trip
 * with mode TICKET_MACHINE and a negative fare (representing money added).
 */
class ClipperRefill(
    override val startTimestamp: Instant?,
    private val amount: Int,
    private val agency: Int,
    override val machineID: String?
) : Trip() {

    override val fare: TransitCurrency?
        get() = TransitCurrency.USD(-amount)

    override val mode: Mode
        get() = Mode.TICKET_MACHINE

    override val agencyName: String?
        get() = ClipperData.getAgencyName(agency)

    override val shortAgencyName: String?
        get() = ClipperData.getShortAgencyName(agency)

    companion object {
        fun create(timestamp: Long, amount: Long, agency: Long, machineId: Long): ClipperRefill =
            ClipperRefill(
                startTimestamp = if (timestamp != 0L) Instant.fromEpochSeconds(timestamp) else null,
                amount = amount.toInt(),
                agency = agency.toInt(),
                machineID = machineId.toString()
            )
    }
}
