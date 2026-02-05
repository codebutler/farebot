/*
 * OctopusData.kt
 *
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
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
package com.codebutler.farebot.transit.octopus

import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * Octopus balance offset data.
 *
 * Octopus cards store balances with an offset that changed on 2017-10-01 when the
 * negative balance limit was increased from -$35 to -$50:
 * https://www.octopus.com.hk/en/consumer/customer-service/faq/get-your-octopus/about-octopus.html#3532
 * https://www.octopus.com.hk/en/consumer/customer-service/faq/get-your-octopus/about-octopus.html#3517
 */
object OctopusData {
    private val OCTOPUS_TZ = TimeZone.of("Asia/Hong_Kong")

    private val OCTOPUS_OFFSETS: List<Pair<Instant, Int>> = listOf(
        // Original offset from 1997
        LocalDateTime(1997, 1, 1, 0, 0).toInstant(OCTOPUS_TZ) to 350,

        // Negative balance amount change effective 2017-10-01, which changes the offset
        LocalDateTime(2017, 10, 1, 0, 0).toInstant(OCTOPUS_TZ) to 500
    )

    private const val SHENZHEN_OFFSET = 350

    // Shenzhen Tong issues different cards now, so do not know if the new balance applies to
    // that card as well.

    private fun getOffset(scanTime: Instant, offsets: List<Pair<Instant, Int>>): Int {
        var offset = offsets.first().second

        for ((offsetStart, offsetValue) in offsets) {
            if (scanTime > offsetStart) {
                offset = offsetValue
            } else {
                break
            }
        }

        return offset
    }

    fun getOctopusOffset(scanTime: Instant): Int = getOffset(scanTime, OCTOPUS_OFFSETS)

    fun getShenzhenOffset(@Suppress("UNUSED_PARAMETER") scanTime: Instant): Int = SHENZHEN_OFFSET
}
