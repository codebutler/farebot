/*
 * SmartRiderUtils.kt
 *
 * Copyright 2016-2022 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.smartrider

import com.codebutler.farebot.transit.Trip
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.time.Duration.Companion.days

/**
 * SmartRider and MyWay cards store timestamps as seconds since 2000-01-01 00:00:00 LOCAL time.
 *
 * To convert to UTC, we need to subtract the local timezone offset from the base epoch.
 * Perth timezone: Australia/Perth (UTC+8) -> subtract 8 hours
 * Canberra timezone: Australia/Sydney (UTC+10/+11) -> subtract 11 hours (uses fixed offset)
 *
 * This matches Metrodroid's approach:
 *   SMARTRIDER_EPOCH = Epoch.utc(2000, MetroTimeZone.PERTH, -8 * 60)
 *   MYWAY_EPOCH = Epoch.utc(2000, MetroTimeZone.SYDNEY, -11 * 60)
 */

/** Unix epoch seconds for 2000-01-01T00:00:00 UTC */
private const val EPOCH_2000_UTC = 946684800L

/** Seconds in an hour */
private const val HOUR_IN_SECONDS = 3600L

/**
 * SmartRider (Perth) epoch: 2000-01-01 00:00:00 local time = 1999-12-31 16:00:00 UTC
 * Perth is UTC+8, so we subtract 8 hours from the UTC epoch.
 */
private const val SMARTRIDER_EPOCH = EPOCH_2000_UTC - (8 * HOUR_IN_SECONDS)

/**
 * MyWay (Canberra/Sydney) epoch: 2000-01-01 00:00:00 local time
 * Uses a fixed -11 hour offset (matching Metrodroid's behavior).
 * Sydney is typically UTC+10 or UTC+11 (DST), but cards use a fixed offset.
 */
private const val MYWAY_EPOCH = EPOCH_2000_UTC - (11 * HOUR_IN_SECONDS)

/** Date epoch for issue/expiry dates: 1997-01-01 */
val DATE_EPOCH = LocalDate(1997, Month.JANUARY, 1)

const val SMARTRIDER_STR = "smartrider"

val PERTH_TIMEZONE = TimeZone.of("Australia/Perth")
val SYDNEY_TIMEZONE = TimeZone.of("Australia/Sydney")

/**
 * Converts a card timestamp (seconds since 2000-01-01 local time) to an [Instant].
 * Uses the appropriate epoch offset based on the card type.
 */
fun convertTime(epochTime: Long, smartRiderType: SmartRiderType): Instant? {
    if (epochTime == 0L) return null
    val epoch = when (smartRiderType) {
        SmartRiderType.MYWAY -> MYWAY_EPOCH
        SmartRiderType.SMARTRIDER -> SMARTRIDER_EPOCH
        SmartRiderType.UNKNOWN -> SMARTRIDER_EPOCH
    }
    return Instant.fromEpochSeconds(epoch + epochTime)
}

/**
 * Converts a day count (days since 1997-01-01) to an [Instant].
 */
fun convertDate(days: Int): Instant {
    val baseInstant = DATE_EPOCH.atStartOfDayIn(TimeZone.UTC)
    return baseInstant + days.days
}

/**
 * Bitfield parser for SmartRider trip records.
 */
class SmartRiderTripBitfield(smartRiderType: SmartRiderType, bitfield: Int) {
    val mode: Trip.Mode = when (bitfield and 0x03) {
        0x00 -> Trip.Mode.BUS
        0x01 -> when (smartRiderType) {
            SmartRiderType.MYWAY -> Trip.Mode.TRAM
            else -> Trip.Mode.TRAIN
        }
        0x02 -> Trip.Mode.FERRY
        else -> Trip.Mode.OTHER
    }
    val isSynthetic = bitfield and 0x04 == 0x04
    val isTransfer = bitfield and 0x08 == 0x08
    val isTapOn = bitfield and 0x10 == 0x10
    val isAutoLoadDiscount = bitfield and 0x40 == 0x40
    val isBalanceNegative = bitfield and 0x80 == 0x80

    override fun toString(): String {
        return "mode=$mode, isSynthetic=$isSynthetic, isTransfer=$isTransfer, isTapOn=$isTapOn, " +
            "isAutoLoadDiscount=$isAutoLoadDiscount, isBalanceNegative=$isBalanceNegative"
    }
}
