/*
 * SeqGoTrip.kt
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

import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.Trip
import farebot.transit.seqgo.generated.resources.*
import kotlin.time.Instant

/**
 * Represents trip events on Go Card.
 */
class SeqGoTrip(
    private val journeyId: Int = 0,
    private val modeValue: Mode = Mode.OTHER,
    private val startTime: Instant? = null,
    private val endTime: Instant? = null,
    private val startStationId: Int = 0,
    private val endStationId: Int = 0,
    private val startStationValue: Station? = null,
    private val endStationValue: Station? = null,
) : Trip() {
    override val startTimestamp: Instant? get() = startTime

    override val endTimestamp: Instant? get() = endTime

    override val mode: Mode get() = modeValue

    override val agencyName: FormattedString
        get() =
            when (mode) {
                Mode.FERRY -> FormattedString(Res.string.seqgo_agency_transdev)
                Mode.TRAIN -> {
                    if (startStationId == 9 || endStationId == 9) {
                        FormattedString(Res.string.seqgo_agency_airtrain)
                    } else {
                        FormattedString(Res.string.seqgo_agency_qr)
                    }
                }
                else -> FormattedString(Res.string.seqgo_agency_translink)
            }

    override val startStation: Station?
        get() = startStationValue

    override val endStation: Station?
        get() = endStationValue

    // Expose for SeqGoTransitFactory
    fun getEndTime(): Instant? = endTime

    fun getStartStationId(): Int = startStationId

    fun getEndStationId(): Int = endStationId

}
