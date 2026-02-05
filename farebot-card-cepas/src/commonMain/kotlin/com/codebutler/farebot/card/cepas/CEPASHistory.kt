/*
 * CEPASHistory.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014-2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2011 Sean Cross <sean@chumby.com>
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
data class CEPASHistory(
    val id: Int,
    val transactions: List<CEPASTransaction>?,
    val isValid: Boolean,
    val errorMessage: String?
) {
    companion object {
        fun create(id: Int, transactions: List<CEPASTransaction>): CEPASHistory {
            return CEPASHistory(id, transactions, true, null)
        }

        fun create(purseId: Int, errorMessage: String): CEPASHistory {
            return CEPASHistory(purseId, null, false, errorMessage)
        }

        fun create(purseId: Int, historyData: ByteArray?): CEPASHistory {
            if (historyData == null) {
                return CEPASHistory(purseId, emptyList(), false, null)
            }
            val recordSize = 16
            val transactions = mutableListOf<CEPASTransaction>()
            var i = 0
            while (i < historyData.size) {
                val tempData = ByteArray(recordSize)
                historyData.copyInto(tempData, 0, i, i + tempData.size)
                transactions.add(CEPASTransaction.create(tempData))
                i += recordSize
            }
            return CEPASHistory(purseId, transactions, true, null)
        }
    }
}
