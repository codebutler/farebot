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

package com.codebutler.farebot.transit.seq_go

import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.Trip
import farebot.farebot_transit_seqgo.generated.resources.*
import kotlinx.coroutines.runBlocking
import kotlin.time.Instant
import org.jetbrains.compose.resources.getString

/**
 * Represents trip events on Go Card.
 */
class SeqGoTrip(
    private val journeyId: Int,
    private val modeValue: Mode,
    private val startTime: Instant?,
    private val endTime: Instant?,
    private val startStationId: Int,
    private val endStationId: Int,
    private val startStationValue: Station?,
    private val endStationValue: Station?
) : Trip() {

    override val startTimestamp: Instant? get() = startTime

    override val endTimestamp: Instant? get() = endTime

    override val mode: Mode get() = modeValue

    override val agencyName: String
        get() = when (mode) {
            Mode.FERRY -> runBlocking { getString(Res.string.seqgo_agency_transdev) }
            Mode.TRAIN -> {
                if (startStationId == 9 || endStationId == 9) {
                    runBlocking { getString(Res.string.seqgo_agency_airtrain) }
                } else {
                    runBlocking { getString(Res.string.seqgo_agency_qr) }
                }
            }
            else -> runBlocking { getString(Res.string.seqgo_agency_translink) }
        }

    override val startStation: Station?
        get() = startStationValue

    override val endStation: Station?
        get() = endStationValue

    // Expose for SeqGoTransitFactory
    fun getEndTime(): Instant? = endTime
    fun getStartStationId(): Int = startStationId
    fun getEndStationId(): Int = endStationId

    class Builder {
        private var journeyId: Int = 0
        private var mode: Mode = Mode.OTHER
        private var startTime: Instant? = null
        private var endTime: Instant? = null
        private var startStationId: Int = 0
        private var endStationId: Int = 0
        private var startStation: Station? = null
        private var endStation: Station? = null

        fun journeyId(journeyId: Int) = apply { this.journeyId = journeyId }
        fun mode(mode: Mode) = apply { this.mode = mode }
        fun startTime(startTime: Instant?) = apply { this.startTime = startTime }
        fun endTime(endTime: Instant?) = apply { this.endTime = endTime }
        fun startStationId(startStationId: Int) = apply { this.startStationId = startStationId }
        fun endStationId(endStationId: Int) = apply { this.endStationId = endStationId }
        fun startStation(station: Station?) = apply { this.startStation = station }
        fun endStation(station: Station?) = apply { this.endStation = station }

        fun build(): SeqGoTrip = SeqGoTrip(
            journeyId = journeyId,
            modeValue = mode,
            startTime = startTime,
            endTime = endTime,
            startStationId = startStationId,
            endStationId = endStationId,
            startStationValue = startStation,
            endStationValue = endStation
        )
    }

    companion object {
        fun builder(): Builder = Builder()
    }
}
