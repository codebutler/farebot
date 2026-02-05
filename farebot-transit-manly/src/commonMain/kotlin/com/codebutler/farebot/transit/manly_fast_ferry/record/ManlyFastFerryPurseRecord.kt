/*
 * ManlyFastFerryPurseRecord.kt
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

package com.codebutler.farebot.transit.manly_fast_ferry.record

import com.codebutler.farebot.base.util.ByteUtils
import kotlinx.serialization.Serializable

/**
 * Represents a "purse" type record.
 */
@Serializable
data class ManlyFastFerryPurseRecord(
    val day: Int,
    val minute: Int,
    val transactionValue: Int,
    val isCredit: Boolean
) : ManlyFastFerryRegularRecord() {

    companion object {
        fun recordFromBytes(input: ByteArray): ManlyFastFerryPurseRecord? {
            if (input[0] != 0x02.toByte()) {
                throw AssertionError("PurseRecord input[0] != 0x02")
            }

            val isCredit: Boolean = when {
                input[3] == 0x09.toByte() -> false
                input[3] == 0x08.toByte() -> true
                else -> return null // bad record?
            }

            val day = ByteUtils.getBitsFromBuffer(input, 32, 20)
            if (day < 0) {
                throw AssertionError("Day < 0")
            }

            val minute = ByteUtils.getBitsFromBuffer(input, 52, 12)
            if (minute > 1440) {
                throw AssertionError("Minute > 1440")
            }
            if (minute < 0) {
                throw AssertionError("Minute < 0")
            }

            val transactionValue = ByteUtils.byteArrayToInt(input, 8, 4)
            if (transactionValue < 0) {
                throw AssertionError("Value < 0")
            }

            return ManlyFastFerryPurseRecord(day, minute, transactionValue, isCredit)
        }
    }
}
