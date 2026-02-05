/*
 * BilheteUnicoSPTrip.kt
 *
 * Copyright 2018 Google
 * Copyright (C) 2025 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.bilhete_unico

import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

internal class BilheteUnicoSPTrip(
    private val mDay: Int,
    private val mTime: Int,
    private val mTransport: Int,
    private val mLocation: Int,
    private val mLine: Int,
    private val mFare: Int,
) : Trip() {

    override val startTimestamp: Instant?
        get() = epochDayMinute(mDay, mTime)

    override val fare: TransitCurrency?
        get() = TransitCurrency.BRL(mFare)

    override val mode: Mode
        get() = when (mTransport) {
            BUS -> Mode.BUS
            TRAM -> Mode.TRAM
            else -> Mode.OTHER
        }

    override val routeName: String?
        get() = if (mTransport == BUS && mLine == 0x38222) mLocation.toString(16) else mLine.toString(16)

    override val startStation: Station?
        get() = if (mTransport == BUS && mLine == 0x38222) null else Station.unknown(mLocation.toString(16))

    override val agencyName: String?
        get() = mTransport.toString(16)

    companion object {
        private const val BUS = 0xb4
        private const val TRAM = 0x78
        private val SAO_PAULO_TZ = TimeZone.of("America/Sao_Paulo")
        private val EPOCH = LocalDate(2000, 1, 1)

        fun epochDayMinute(day: Int, minute: Int): Instant? {
            if (day == 0) return null
            val date = EPOCH.toEpochDays() + day
            val ld = LocalDate.fromEpochDays(date)
            val hours = minute / 60
            val mins = minute % 60
            return LocalDateTime(ld.year, ld.month, ld.day, hours, mins)
                .toInstant(SAO_PAULO_TZ)
        }

        fun epochDay(day: Int): Instant? {
            if (day == 0) return null
            return epochDayMinute(day, 0)
        }

        fun parse(sector: DataClassicSector): BilheteUnicoSPTrip {
            val block0 = sector.getBlock(0).data
            val block1 = sector.getBlock(1).data

            return BilheteUnicoSPTrip(
                mTransport = block0.getBitsFromBuffer(0, 8),
                mLocation = block0.getBitsFromBuffer(8, 20),
                mLine = block0.getBitsFromBuffer(28, 20),
                mFare = block1.getBitsFromBuffer(36, 16),
                mDay = block1.getBitsFromBuffer(76, 14),
                mTime = block1.getBitsFromBuffer(90, 11),
            )
        }
    }
}
