/*
 * CEPASPurse.kt
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

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class CEPASPurse(
    val id: Int,
    val cepasVersion: Byte,
    val purseStatus: Byte,
    val purseBalance: Int,
    val autoLoadAmount: Int,
    @Contextual val can: ByteArray?,
    @Contextual val csn: ByteArray?,
    val purseExpiryDate: Int,
    val purseCreationDate: Int,
    val lastCreditTransactionTRP: Int,
    @Contextual val lastCreditTransactionHeader: ByteArray?,
    val logfileRecordCount: Byte,
    val issuerDataLength: Int,
    val lastTransactionTRP: Int,
    val lastTransactionRecord: CEPASTransaction?,
    @Contextual val issuerSpecificData: ByteArray?,
    val lastTransactionDebitOptionsByte: Byte,
    val isValid: Boolean,
    val errorMessage: String?,
) {
    companion object {
        fun create(
            id: Int,
            cepasVersion: Byte,
            purseStatus: Byte,
            purseBalance: Int,
            autoLoadAmount: Int,
            can: ByteArray,
            csn: ByteArray,
            purseExpiryDate: Int,
            purseCreationDate: Int,
            lastCreditTransactionTRP: Int,
            lastCreditTransactionHeader: ByteArray,
            logfileRecordCount: Byte,
            issuerDataLength: Int,
            lastTransactionTRP: Int,
            lastTransactionRecord: CEPASTransaction,
            issuerSpecificData: ByteArray,
            lastTransactionDebitOptionsByte: Byte,
        ): CEPASPurse =
            CEPASPurse(
                id,
                cepasVersion,
                purseStatus,
                purseBalance,
                autoLoadAmount,
                can,
                csn,
                purseExpiryDate,
                purseCreationDate,
                lastCreditTransactionTRP,
                lastCreditTransactionHeader,
                logfileRecordCount,
                issuerDataLength,
                lastTransactionTRP,
                lastTransactionRecord,
                issuerSpecificData,
                lastTransactionDebitOptionsByte,
                true,
                null,
            )

        fun create(
            purseId: Int,
            errorMessage: String,
        ): CEPASPurse =
            CEPASPurse(
                purseId,
                0,
                0,
                0,
                0,
                null,
                null,
                0,
                0,
                0,
                null,
                0,
                0,
                0,
                null,
                null,
                0,
                false,
                errorMessage,
            )

        @Suppress("NAME_SHADOWING")
        fun create(
            purseId: Int,
            purseData: ByteArray,
        ): CEPASPurse {
            var purseData = purseData
            val isValid: Boolean
            val errorMessage: String

            @Suppress("SENSELESS_COMPARISON")
            if (purseData == null) {
                purseData = ByteArray(128)
                isValid = false
                errorMessage = ""
            } else {
                isValid = true
                errorMessage = ""
            }

            val cepasVersion = purseData[0]
            val purseStatus = purseData[1]

            var tmp =
                (0x00ff0000 and (purseData[2].toInt() shl 16)) or
                    (0x0000ff00 and (purseData[3].toInt() shl 8)) or
                    (0x000000ff and purseData[4].toInt())
            if (0 != (purseData[2].toInt() and 0x80)) {
                tmp = tmp or 0xff000000.toInt()
            }
            val purseBalance = tmp

            tmp = (0x00ff0000 and (purseData[5].toInt() shl 16)) or
                (0x0000ff00 and (purseData[6].toInt() shl 8)) or
                (0x000000ff and purseData[7].toInt())
            if (0 != (purseData[5].toInt() and 0x80)) {
                tmp = tmp or 0xff000000.toInt()
            }
            val autoLoadAmount = tmp

            val can = ByteArray(8)
            purseData.copyInto(can, 0, 8, 8 + can.size)

            val csn = ByteArray(8)
            purseData.copyInto(csn, 0, 16, 16 + csn.size)

            // CEPAS epoch: January 1, 1995 00:00:00 SGT (UTC+8)
            val cepasEpoch = 788947200 - (8 * 3600)
            val purseExpiryDate =
                cepasEpoch +
                    (86400 * ((0xff00 and (purseData[24].toInt() shl 8)) or (0x00ff and purseData[25].toInt())))
            val purseCreationDate =
                cepasEpoch +
                    (86400 * ((0xff00 and (purseData[26].toInt() shl 8)) or (0x00ff and purseData[27].toInt())))

            val lastCreditTransactionTRP =
                (0xff000000.toInt() and (purseData[28].toInt() shl 24)) or
                    (0x00ff0000 and (purseData[29].toInt() shl 16)) or
                    (0x0000ff00 and (purseData[30].toInt() shl 8)) or
                    (0x000000ff and purseData[31].toInt())

            val lastCreditTransactionHeader = ByteArray(8)
            purseData.copyInto(lastCreditTransactionHeader, 0, 32, 40)

            val logfileRecordCount = purseData[40]

            val issuerDataLength = 0x00ff and purseData[41].toInt()

            val lastTransactionTRP =
                (0xff000000.toInt() and (purseData[42].toInt() shl 24)) or
                    (0x00ff0000 and (purseData[43].toInt() shl 16)) or
                    (0x0000ff00 and (purseData[44].toInt() shl 8)) or
                    (0x000000ff and purseData[45].toInt())

            val tmpTransaction = ByteArray(16)
            purseData.copyInto(tmpTransaction, 0, 46, 46 + tmpTransaction.size)
            val lastTransactionRecord = CEPASTransaction.create(tmpTransaction)

            val issuerSpecificData = ByteArray(issuerDataLength)
            purseData.copyInto(issuerSpecificData, 0, 62, 62 + issuerSpecificData.size)

            val lastTransactionDebitOptionsByte = purseData[62 + issuerDataLength]

            return CEPASPurse(
                purseId,
                cepasVersion,
                purseStatus,
                purseBalance,
                autoLoadAmount,
                can,
                csn,
                purseExpiryDate,
                purseCreationDate,
                lastCreditTransactionTRP,
                lastCreditTransactionHeader,
                logfileRecordCount,
                issuerDataLength,
                lastTransactionTRP,
                lastTransactionRecord,
                issuerSpecificData,
                lastTransactionDebitOptionsByte,
                isValid,
                errorMessage,
            )
        }
    }
}
