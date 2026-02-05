/*
 * RkfPurse.kt
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
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.en1545.En1545Container
import com.codebutler.farebot.transit.en1545.En1545FixedInteger
import com.codebutler.farebot.transit.en1545.En1545Parsed
import com.codebutler.farebot.transit.en1545.En1545Parser
data class RkfPurse(
    private val mStatic: En1545Parsed,
    private val mDynamic: En1545Parsed,
    private val mLookup: RkfLookup
) {
    val balance: TransitBalance
        get() {
            val balance = mLookup.parseCurrency(mDynamic.getIntOrZero(VALUE))
            val name = mLookup.getAgencyName(mStatic.getIntOrZero(RkfTransitInfo.COMPANY), true)

            return TransitBalance(
                balance = balance,
                name = name,
                validFrom = mStatic.getTimeStamp(START, mLookup.timeZone),
                validTo = mDynamic.getTimeStamp(END, mLookup.timeZone)
            )
        }

    val transactionNumber: Int
        get() = mDynamic.getIntOrZero(TRANSACTION_NUMBER)

    companion object {
        private const val TAG = "RkfPurse"
        private const val VALUE = "Value"
        private const val START = "Start"
        private const val END = "End"
        private const val TRANSACTION_NUMBER = "PurseTransactionNumber"
        private val TCPU_STATIC_FIELDS = En1545Container(
            RkfTransitInfo.HEADER,
            En1545FixedInteger("PurseSerialNumber", 32),
            En1545FixedInteger.date(START),
            En1545FixedInteger("DataPointer", 4),
            En1545FixedInteger("MinimumValue", 24),
            En1545FixedInteger("AutoLoadValue", 24)
            // v6 has more fields but whatever
        )
        private val TCPU_DYNAMIC_FIELDS = mapOf(
            3 to En1545Container(
                En1545FixedInteger(TRANSACTION_NUMBER, 16),
                En1545FixedInteger.date(END),
                En1545FixedInteger(VALUE, 24),
                RkfTransitInfo.STATUS_FIELD
                // Rest unknown
            ),
            4 to En1545Container(
                En1545FixedInteger(TRANSACTION_NUMBER, 16),
                En1545FixedInteger(VALUE, 24)
                // Rest unknown
            ),
            6 to En1545Container(
                En1545FixedInteger(TRANSACTION_NUMBER, 16),
                En1545FixedInteger(VALUE, 24),
                RkfTransitInfo.STATUS_FIELD
                // Rest unknown
            )
        )

        fun parse(record: ByteArray, lookup: RkfLookup): RkfPurse {
            var version = record.getBitsFromBufferLeBits(8, 6)
            val blockSize = if (version >= 6) 32 else 16
            val static = En1545Parser.parseLeBits(record.copyOfRange(0, blockSize - 1), TCPU_STATIC_FIELDS)
            val blockA = record.copyOfRange(blockSize, blockSize * 2 - 1)
            val blockB = record.copyOfRange(blockSize * 2, blockSize * 3 - 1)
            val block = if (blockA.getBitsFromBufferLeBits(0, 16)
                > blockB.getBitsFromBufferLeBits(0, 16)) blockA else blockB
            // Try something that might be close enough
            if (version < 3)
                version = 3
            if (version > 6 || version == 5)
                version = 6
            val dynamic = En1545Parser.parseLeBits(block, TCPU_DYNAMIC_FIELDS.getValue(version))
            return RkfPurse(mStatic = static, mDynamic = dynamic, mLookup = lookup)
        }
    }
}
