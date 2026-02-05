/*
 * ManlyFastFerryBalanceRecord.kt
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

/**
 * Represents a "preamble" type record.
 */
class ManlyFastFerryBalanceRecord private constructor() : ManlyFastFerryRecord(), Comparable<ManlyFastFerryBalanceRecord> {

    var balance: Int = 0
        private set
    var version: Int = 0
        private set

    override fun compareTo(other: ManlyFastFerryBalanceRecord): Int {
        // So sorting works, we reverse the order so highest number is first.
        return other.version.compareTo(this.version)
    }

    companion object {
        fun recordFromBytes(input: ByteArray): ManlyFastFerryBalanceRecord {
            if (input[0] != 0x01.toByte()) {
                throw AssertionError()
            }

            val record = ManlyFastFerryBalanceRecord()
            record.version = ByteUtils.byteArrayToInt(input, 2, 1)
            record.balance = ByteUtils.byteArrayToInt(input, 11, 4)

            return record
        }
    }
}
