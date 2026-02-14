/*
 * ClipperUtil.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2018 Google
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

package com.codebutler.farebot.transit.clipper

/**
 * Clipper epoch is January 1, 1900 00:00:00 UTC.
 *
 * Clipper timestamps are seconds since 1900-01-01 UTC. To convert to Unix epoch
 * (1970-01-01 UTC), we subtract the number of seconds between these two dates.
 *
 * From 1900-01-01 to 1970-01-01 is exactly 70 years, which includes 17 leap years
 * (1904, 1908, 1912, 1916, 1920, 1924, 1928, 1932, 1936, 1940, 1944, 1948, 1952,
 * 1956, 1960, 1964, 1968), giving us:
 *   70 * 365 + 17 = 25567 days
 *   25567 * 86400 = 2208988800 seconds
 */
internal object ClipperUtil {
    /**
     * Number of seconds between Clipper epoch (1900-01-01) and Unix epoch (1970-01-01).
     * Used to convert Clipper timestamps to Unix timestamps.
     */
    const val CLIPPER_EPOCH_SECONDS = 2208988800L

    /**
     * Convert a Clipper timestamp (seconds since Jan 1, 1900) to Unix epoch seconds.
     */
    fun clipperTimestampToEpochSeconds(clipperSeconds: Long): Long {
        if (clipperSeconds == 0L) return 0L
        return clipperSeconds - CLIPPER_EPOCH_SECONDS
    }
}
