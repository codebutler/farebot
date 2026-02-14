/*
 * SeqGoData.kt
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

package com.codebutler.farebot.transit.seqgo

import com.codebutler.farebot.transit.Trip

/**
 * Constants used in Go card
 */
object SeqGoData {
    private const val VEHICLE_FARE_MACHINE = 1
    private const val VEHICLE_BUS = 4
    private const val VEHICLE_RAIL = 5
    private const val VEHICLE_FERRY = 18

    // TODO: Gold Coast Light Rail
    val VEHICLES: Map<Int, Trip.Mode> =
        mapOf(
            VEHICLE_FARE_MACHINE to Trip.Mode.TICKET_MACHINE,
            VEHICLE_RAIL to Trip.Mode.TRAIN,
            VEHICLE_FERRY to Trip.Mode.FERRY,
            VEHICLE_BUS to Trip.Mode.BUS,
        )
}
