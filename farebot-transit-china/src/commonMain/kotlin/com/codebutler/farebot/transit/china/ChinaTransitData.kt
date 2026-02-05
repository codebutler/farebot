/*
 * ChinaTransitData.kt
 *
 * Copyright 2018 Google
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

package com.codebutler.farebot.transit.china

import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.getBitsFromBufferSigned
import com.codebutler.farebot.card.china.ChinaCard
import com.codebutler.farebot.card.iso7816.ISO7816File
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * Shared utilities for parsing China transit card data.
 */
object ChinaTransitData {
    private val TZ = TimeZone.of("Asia/Shanghai")

    /**
     * Parses trips from a China card.
     *
     * @param card The China card to read from
     * @param createTrip Factory function to create trip instances from record data
     * @return List of valid trips
     */
    fun <T : ChinaTripAbstract> parseTrips(
        card: ChinaCard,
        createTrip: (ByteArray) -> T?
    ): List<T> {
        val trips = mutableListOf<T>()
        val historyFile = getFile(card, 0x18)
        for (record in historyFile?.recordList.orEmpty()) {
            val t = createTrip(record)
            if (t == null || !t.isValid)
                continue
            trips.add(t)
        }
        return trips
    }

    /**
     * Parses the balance from a China card.
     * The upper bit is some garbage, so we only read bits 1-31.
     */
    fun parseBalance(card: ChinaCard): Int? =
        card.getBalance(0)?.getBitsFromBufferSigned(1, 31)

    /**
     * Gets a file from the card by file ID.
     * Tries multiple selectors: 0x1001/id, id, and SFI.
     */
    fun getFile(card: ChinaCard, id: Int, trySfi: Boolean = true): ISO7816File? {
        // Try selector 0x1001/id
        val selector1 = "1001/${id.toString(16).padStart(2, '0')}"
        var f = card.getFile(selector1)
        if (f != null) return f

        // Try selector /id
        val selector2 = id.toString(16).padStart(2, '0')
        f = card.getFile(selector2)
        if (f != null) return f

        // Try SFI
        return if (!trySfi) null else card.getSfiFile(id)
    }

    /**
     * Parses a hex-encoded date (YYMMDD in BCD format).
     * Returns null if the value is 0 or null.
     */
    fun parseHexDate(value: Int?): Instant? {
        if (value == null || value == 0)
            return null
        val year = NumberUtils.convertBCDtoInteger(value shr 16)
        val month = NumberUtils.convertBCDtoInteger(value shr 8 and 0xff) - 1
        val day = NumberUtils.convertBCDtoInteger(value and 0xff)

        // Handle 2-digit year, assume 2000s
        val fullYear = if (year < 100) 2000 + year else year

        return try {
            LocalDateTime(
                year = fullYear,
                month = month + 1,
                day = day,
                hour = 0,
                minute = 0,
                second = 0
            ).toInstant(TZ)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parses a hex-encoded date/time (YYYYMMDDHHmmss in BCD format).
     */
    fun parseHexDateTime(value: Long): Instant {
        val year = NumberUtils.convertBCDtoInteger((value shr 40).toInt())
        val month = NumberUtils.convertBCDtoInteger((value shr 32 and 0xffL).toInt()) - 1
        val day = NumberUtils.convertBCDtoInteger((value shr 24 and 0xffL).toInt())
        val hour = NumberUtils.convertBCDtoInteger((value shr 16 and 0xffL).toInt())
        val minute = NumberUtils.convertBCDtoInteger((value shr 8 and 0xffL).toInt())
        val second = NumberUtils.convertBCDtoInteger((value and 0xffL).toInt())

        // Handle 2-digit year, assume 2000s
        val fullYear = if (year < 100) 2000 + year else year

        return LocalDateTime(
            year = fullYear,
            month = month + 1,
            day = day.coerceIn(1, 31),
            hour = hour.coerceIn(0, 23),
            minute = minute.coerceIn(0, 59),
            second = second.coerceIn(0, 59)
        ).toInstant(TZ)
    }
}
