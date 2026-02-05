/*
 * EmvLogEntry.kt
 *
 * Copyright 2019 Google
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.calypso.emv

import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.convertBCDtoInteger
import com.codebutler.farebot.base.util.hex
import com.codebutler.farebot.card.iso7816.ISO7816TLV
import com.codebutler.farebot.card.iso7816.UNKNOWN_TAG
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * Represents a single EMV transaction log entry.
 */
class EmvLogEntry(
    private val values: Map<String, ByteArray>
) : Trip() {

    override val startTimestamp: Instant?
        get() {
            val dateBin = values[EmvData.TAG_TRANSACTION_DATE] ?: return null
            val timeBin = values[EmvData.TAG_TRANSACTION_TIME]
            val year = 2000 + NumberUtils.convertBCDtoInteger(dateBin[0])
            val month = NumberUtils.convertBCDtoInteger(dateBin[1])
            val day = NumberUtils.convertBCDtoInteger(dateBin[2])
            if (timeBin != null) {
                val hour = NumberUtils.convertBCDtoInteger(timeBin[0])
                val min = NumberUtils.convertBCDtoInteger(timeBin[1])
                val sec = NumberUtils.convertBCDtoInteger(timeBin[2])
                return LocalDateTime(year, month, day, hour, min, sec)
                    .toInstant(TimeZone.UTC)
            }
            return LocalDateTime(year, month, day, 0, 0, 0)
                .toInstant(TimeZone.UTC)
        }

    override val fare: TransitCurrency?
        get() {
            val amountBin = values[EmvData.TAG_AMOUNT_AUTHORISED] ?: return null
            val amount = amountBin.convertBCDtoInteger()

            val codeBin = values[EmvData.TAG_TRANSACTION_CURRENCY_CODE]
                ?: return TransitCurrency.XXX(amount)
            val code = NumberUtils.convertBCDtoInteger(codeBin.byteArrayToInt())

            val currencyStr = EmvData.numericCodeToString(code)
            return if (currencyStr != null) {
                TransitCurrency(amount, currencyStr)
            } else {
                TransitCurrency.XXX(amount)
            }
        }

    override val mode: Mode get() = Mode.POS

    override val routeName: String?
        get() {
            val extras = values.entries.filter {
                !HANDLED_TAGS.contains(it.key)
            }.mapNotNull {
                val tag = EmvData.TAGMAP[it.key] ?: UNKNOWN_TAG
                val v = tag.interpretTagString(it.value)
                if (v.isEmpty()) null else "${tag.name}=$v"
            }
            return extras.joinToString().ifEmpty { null }
        }

    companion object {
        private val HANDLED_TAGS = listOf(
            EmvData.TAG_AMOUNT_AUTHORISED,
            EmvData.TAG_TRANSACTION_CURRENCY_CODE,
            EmvData.TAG_TRANSACTION_TIME,
            EmvData.TAG_TRANSACTION_DATE
        )

        fun parseEmvTrip(record: ByteArray, format: ByteArray): EmvLogEntry? {
            val values = mutableMapOf<String, ByteArray>()
            var p = 0
            val dol = ISO7816TLV.removeTlvHeader(format)
            ISO7816TLV.pdolIterate(dol).forEach { (id, len) ->
                if (p + len <= record.size) {
                    values[id.hex()] = record.copyOfRange(p, p + len)
                }
                p += len
            }
            return EmvLogEntry(values = values)
        }
    }
}
