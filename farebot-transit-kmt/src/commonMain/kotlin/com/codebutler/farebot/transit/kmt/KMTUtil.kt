/*
 * KMTUtil.kt
 *
 * Authors:
 * Bondan Sumbodo <sybond@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.codebutler.farebot.transit.kmt

import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import com.codebutler.farebot.card.felica.FeliCaUtil

internal object KMTUtil {

    val TIME_ZONE: TimeZone = TimeZone.of("Asia/Jakarta")

    // Pre-2019 epoch: 2000-01-01 01:00:00 Jakarta time (UTC+7, so 2000-01-01T01:00+07:00)
    private val KMT_EPOCH1: Instant = LocalDateTime(2000, 1, 1, 1, 0, 0).toInstant(TIME_ZONE)

    // Post-2019 epoch: 2000-01-01 07:00:00 Jakarta time (UTC+7, so 2000-01-01T07:00+07:00)
    private val KMT_EPOCH2: Instant = LocalDateTime(2000, 1, 1, 7, 0, 0).toInstant(TIME_ZONE)

    // Transition date: 2019-01-01 00:00:00 Jakarta time
    private val KMT_TRANSITION: Instant = LocalDateTime(2019, 1, 1, 0, 0, 0).toInstant(TIME_ZONE)

    fun extractDate(data: ByteArray): Instant? {
        val fulloffset = FeliCaUtil.toInt(data[0], data[1], data[2], data[3])
        if (fulloffset == 0) {
            return null
        }
        // Try epoch2 first; if result is before transition, use epoch1
        val result2 = Instant.fromEpochSeconds(KMT_EPOCH2.epochSeconds + fulloffset.toLong())
        if (result2 < KMT_TRANSITION) {
            return Instant.fromEpochSeconds(KMT_EPOCH1.epochSeconds + fulloffset.toLong())
        }
        return result2
    }
}
