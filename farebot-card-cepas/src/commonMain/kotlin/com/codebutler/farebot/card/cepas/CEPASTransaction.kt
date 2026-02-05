/*
 * CEPASTransaction.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014-2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2011 Sean Cross <sean@chumby.com>
 * Copyright (C) 2012 tbonang <bonang@gmail.com>
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

package com.codebutler.farebot.card.cepas

import kotlinx.serialization.Serializable

@Serializable
data class CEPASTransaction(
    val rawType: Int,
    val amount: Int,
    val timestamp: Int,
    val userData: String
) {
    enum class TransactionType {
        MRT,
        TOP_UP,
        BUS,
        BUS_REFUND,
        CREATION,
        RETAIL,
        SERVICE,
        UNKNOWN
    }

    val type: TransactionType
        get() = when (rawType) {
            48 -> TransactionType.MRT
            117, 3 -> TransactionType.TOP_UP
            49 -> TransactionType.BUS
            118 -> TransactionType.BUS_REFUND
            -16, 5 -> TransactionType.CREATION
            4 -> TransactionType.SERVICE
            1 -> TransactionType.RETAIL
            else -> TransactionType.UNKNOWN
        }

    companion object {
        // CEPAS epoch: January 1, 1995 00:00:00 SGT (UTC+8)
        // = January 1, 1995 00:00:00 UTC (788947200) minus 8 hours (28800 seconds)
        private const val CEPAS_EPOCH = 788947200 - (8 * 3600)

        fun create(rawData: ByteArray): CEPASTransaction {
            val type = rawData[0].toInt()

            var tmp = (0x00ff0000 and (rawData[1].toInt() shl 16)) or
                    (0x0000ff00 and (rawData[2].toInt() shl 8)) or
                    (0x000000ff and rawData[3].toInt())
            if (0 != (rawData[1].toInt() and 0x80)) {
                tmp = tmp or 0xff000000.toInt()
            }
            val amount = tmp

            /* Date is expressed "in seconds", but the epoch is January 1 1995, SGT */
            val date = ((0xff000000.toInt() and (rawData[4].toInt() shl 24)) or
                    (0x00ff0000 and (rawData[5].toInt() shl 16)) or
                    (0x0000ff00 and (rawData[6].toInt() shl 8)) or
                    (0x000000ff and rawData[7].toInt())) +
                    CEPAS_EPOCH

            val userDataBytes = ByteArray(9)
            rawData.copyInto(userDataBytes, 0, 8, 16)
            userDataBytes[8] = 0
            val userDataString = userDataBytes.decodeToString()

            return CEPASTransaction(type, amount, date, userDataString)
        }
    }
}
