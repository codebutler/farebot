/*
 * EdyTrip.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014-2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.edy

import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.card.felica.FeliCaUtil
import com.codebutler.farebot.card.felica.FelicaBlock
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import farebot.transit.edy.generated.resources.*
import kotlin.time.Instant

class EdyTrip(
    private val processType: Int,
    private val sequenceNumber: Int,
    private val timestampData: Instant,
    private val transactionAmount: Int,
    private val balance: Int,
    private val stringResource: StringResource,
) : Trip() {
    override val startTimestamp: Instant get() = timestampData

    override val mode: Mode
        get() =
            when (processType) {
                EdyTransitInfo.FELICA_MODE_EDY_DEBIT -> Mode.POS
                EdyTransitInfo.FELICA_MODE_EDY_CHARGE -> Mode.TICKET_MACHINE
                EdyTransitInfo.FELICA_MODE_EDY_GIFT -> Mode.VENDING_MACHINE
                else -> Mode.OTHER
            }

    override val fare: TransitCurrency
        get() =
            if (processType != EdyTransitInfo.FELICA_MODE_EDY_DEBIT) {
                TransitCurrency.JPY(-transactionAmount)
            } else {
                TransitCurrency.JPY(transactionAmount)
            }

    override val agencyName: String
        get() {
            val str =
                if (processType != EdyTransitInfo.FELICA_MODE_EDY_DEBIT) {
                    stringResource.getString(Res.string.felica_process_charge)
                } else {
                    stringResource.getString(Res.string.felica_process_merchandise_purchase)
                }
            return str + " " + stringResource.getString(Res.string.edy_transaction_sequence) +
                sequenceNumber.toString().padStart(8, '0')
        }

    fun getProcessType(): Int = processType

    fun getSequenceNumber(): Int = sequenceNumber

    fun getTimestampData(): Instant = timestampData

    fun getTransactionAmount(): Int = transactionAmount

    fun getBalance(): Int = balance

    companion object {
        fun create(
            block: FelicaBlock,
            stringResource: StringResource,
        ): EdyTrip {
            val data = block.data

            val processType = data[0].toInt()
            val sequenceNumber = FeliCaUtil.toInt(data[1], data[2], data[3])
            val timestampData = EdyUtil.extractDate(data)!!
            val transactionAmount = FeliCaUtil.toInt(data[8], data[9], data[10], data[11])
            val balance = FeliCaUtil.toInt(data[12], data[13], data[14], data[15])

            return EdyTrip(processType, sequenceNumber, timestampData, transactionAmount, balance, stringResource)
        }
    }
}
