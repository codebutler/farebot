/*
 * RkfTicket.kt
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

package com.codebutler.farebot.transit.rkf

import com.codebutler.farebot.base.util.getBitsFromBufferLeBits
import com.codebutler.farebot.transit.en1545.En1545Container
import com.codebutler.farebot.transit.en1545.En1545Field
import com.codebutler.farebot.transit.en1545.En1545FixedInteger
import com.codebutler.farebot.transit.en1545.En1545Parsed
import com.codebutler.farebot.transit.en1545.En1545Subscription

class RkfTicket(
    override val parsed: En1545Parsed,
    override val lookup: RkfLookup,
) : En1545Subscription() {

    companion object {
        fun parse(
            record: RkfTctoRecord,
            lookup: RkfLookup,
        ): RkfTicket {
            val version = record.chunks[0][0].getBitsFromBufferLeBits(8, 6)
            val maxTxn =
                record.chunks
                    .filter { it[0][0] == 0x88.toByte() }
                    .map {
                        it[0].getBitsFromBufferLeBits(
                            8,
                            12,
                        )
                    }.maxOrNull()
            val flat =
                record.chunks
                    .filter {
                        it[0][0] != 0x88.toByte() ||
                            it[0].getBitsFromBufferLeBits(8, 12) == maxTxn
                    }.flatten()
            val parsed = En1545Parsed()
            for (tag in flat) {
                val fields = getFields(tag[0], version) ?: continue
                parsed.appendLeBits(tag, fields)
            }
            return RkfTicket(parsed, lookup)
        }

        @Suppress("UNUSED_PARAMETER")
        private fun getFields(
            id: Byte,
            version: Int,
        ): En1545Field? =
            when (id.toInt() and 0xff) {
                0x87 ->
                    En1545Container(
                        RkfTransitInfo.ID_FIELD, // verified
                        RkfTransitInfo.VERSION_FIELD, // verified
                    )
                0x88 ->
                    En1545Container(
                        RkfTransitInfo.ID_FIELD, // verified
                        En1545FixedInteger("TransactionNumber", 12), // verified
                    )
                0x89 ->
                    En1545Container(
                        RkfTransitInfo.ID_FIELD, // verified
                        En1545FixedInteger(CONTRACT_PROVIDER, 12), // verified
                        En1545FixedInteger(CONTRACT_TARIFF, 12),
                        En1545FixedInteger(CONTRACT_SALE_DEVICE, 16),
                        En1545FixedInteger(CONTRACT_SERIAL_NUMBER, 32),
                        RkfTransitInfo.STATUS_FIELD,
                    )
                0x96 ->
                    En1545Container(
                        RkfTransitInfo.ID_FIELD, // verified
                        En1545FixedInteger.date(CONTRACT_START), // verified
                        En1545FixedInteger.timePacked16(CONTRACT_START), // verified
                        En1545FixedInteger.date(CONTRACT_END), // verified
                        En1545FixedInteger.timePacked16(CONTRACT_END), // verified
                        En1545FixedInteger(CONTRACT_DURATION, 8), // verified, days
                        En1545FixedInteger.date("Limit"),
                        En1545FixedInteger("PeriodJourneys", 8),
                        En1545FixedInteger("RestrictDay", 8),
                        En1545FixedInteger("RestrictTimecode", 8),
                    )
                else -> null
            }
    }
}
