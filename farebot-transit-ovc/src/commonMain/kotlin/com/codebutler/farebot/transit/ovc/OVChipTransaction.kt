/*
 * OVChipTransaction.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2012, 2014-2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.ovc

import com.codebutler.farebot.base.util.ByteUtils
import kotlinx.serialization.Serializable

@Serializable
data class OVChipTransaction(
    val transactionSlot: Int,
    val date: Int,
    val time: Int,
    val transfer: Int,
    val company: Int,
    val id: Int,
    val station: Int,
    val machineId: Int,
    val vehicleId: Int,
    val productId: Int,
    val amount: Int,
    val subscriptionId: Int,
    val valid: Int,
    val unknownConstant: Int,
    val unknownConstant2: Int,
    val errorMessage: String
) {
    fun isSameTrip(nextTransaction: OVChipTransaction): Boolean {
        /*
         * Information about checking in and out:
         * http://www.chipinfo.nl/inchecken/
         */
        if (company == nextTransaction.company && transfer == OVChipTransitInfo.PROCESS_CHECKIN
            && nextTransaction.transfer == OVChipTransitInfo.PROCESS_CHECKOUT
        ) {
            if (date == nextTransaction.date) {
                return true
            } else if (date == nextTransaction.date - 1) {
                if (company == OVChipTransitInfo.AGENCY_NS && nextTransaction.time < 240) {
                    return true
                }
                if (company != OVChipTransitInfo.AGENCY_NS) {
                    return true
                }
            }
        }
        return false
    }

    companion object {
        val ID_ORDER: Comparator<OVChipTransaction> = Comparator { t1, t2 ->
            if (t1.id < t2.id) -1 else if (t1.id == t2.id) 0 else 1
        }

        fun create(transactionSlot: Int, data: ByteArray?): OVChipTransaction {
            val d = data ?: ByteArray(32)

            var valid = 1
            var date = 0
            var time = 0
            var unknownConstant = 0
            var transfer = -3
            var company = 0
            var id = 0
            var station = 0
            var machineId = 0
            var vehicleId = 0
            var productId = 0
            var unknownConstant2 = 0
            var amount = 0
            var subscriptionId = -1
            var errorMessage = ""

            if (d[0] == 0x00.toByte()
                && d[1] == 0x00.toByte()
                && d[2] == 0x00.toByte()
                && (d[3].toInt() and 0xF0) == 0x00
            ) {
                valid = 0
            }
            if ((d[3].toInt() and 0x10) != 0x00) valid = 0
            if ((d[3].toInt() and 0x80.toByte().toInt()) != 0x00) valid = 0
            if ((d[2].toInt() and 0x02) != 0x00) valid = 0
            if ((d[2].toInt() and 0x08) != 0x00) valid = 0
            if ((d[2].toInt() and 0x20) != 0x00) valid = 0
            if ((d[2].toInt() and 0x80.toByte().toInt()) != 0x00) valid = 0
            if ((d[1].toInt() and 0x01) != 0x00) valid = 0
            if ((d[1].toInt() and 0x02) != 0x00) valid = 0
            if ((d[1].toInt() and 0x08) != 0x00) valid = 0
            if ((d[1].toInt() and 0x20) != 0x00) valid = 0
            if ((d[1].toInt() and 0x40) != 0x00) valid = 0
            if ((d[1].toInt() and 0x80.toByte().toInt()) != 0x00) valid = 0
            if ((d[0].toInt() and 0x02) != 0x00) valid = 0
            if ((d[0].toInt() and 0x04) != 0x00) valid = 0

            if (valid == 0) {
                errorMessage = "No transaction"
            } else {
                var iBitOffset = 53

                date = (((d[3].toInt() and 0xFF) and 0x0F) shl 10) or (((d[4].toInt() and 0xFF)) shl 2) or
                        (((d[5].toInt() and 0xFF) shr 6) and 0x03)
                time = (((d[5].toInt() and 0xFF) and 0x3F) shl 5) or (((d[6].toInt() and 0xFF) shr 3) and 0x1F)

                if ((d[3].toInt() and 0x20) != 0x00) {
                    unknownConstant = ByteUtils.getBitsFromBuffer(d, iBitOffset, 24)
                    iBitOffset += 24
                }
                if ((d[3].toInt() and 0x40) != 0x00) {
                    transfer = ByteUtils.getBitsFromBuffer(d, iBitOffset, 7)
                    iBitOffset += 7
                }
                if ((d[2].toInt() and 0x01) != 0x00) {
                    company = ByteUtils.getBitsFromBuffer(d, iBitOffset, 16)
                    iBitOffset += 16
                }
                if ((d[2].toInt() and 0x04) != 0x00) {
                    id = ByteUtils.getBitsFromBuffer(d, iBitOffset, 24)
                    iBitOffset += 24
                }
                if ((d[2].toInt() and 0x10) != 0x00) {
                    station = ByteUtils.getBitsFromBuffer(d, iBitOffset, 16)
                    iBitOffset += 16
                }
                if ((d[2].toInt() and 0x40) != 0x00) {
                    machineId = ByteUtils.getBitsFromBuffer(d, iBitOffset, 24)
                    iBitOffset += 24
                }
                if ((d[1].toInt() and 0x04) != 0x00) {
                    vehicleId = ByteUtils.getBitsFromBuffer(d, iBitOffset, 16)
                    iBitOffset += 16
                }
                if ((d[1].toInt() and 0x10) != 0x00) {
                    productId = ByteUtils.getBitsFromBuffer(d, iBitOffset, 5)
                    iBitOffset += 5
                }
                if ((d[0].toInt() and 0x01) != 0x00) {
                    unknownConstant2 = ByteUtils.getBitsFromBuffer(d, iBitOffset, 16)
                    iBitOffset += 16
                }
                if ((d[0].toInt() and 0x08) != 0x00) {
                    amount = ByteUtils.getBitsFromBuffer(d, iBitOffset, 16)
                    iBitOffset += 16
                }
                if ((d[1].toInt() and 0x10) == 0x00) {
                    subscriptionId = ByteUtils.getBitsFromBuffer(d, iBitOffset, 13)
                }
            }

            return OVChipTransaction(
                transactionSlot = transactionSlot,
                date = date,
                time = time,
                transfer = transfer,
                company = company,
                id = id,
                station = station,
                machineId = machineId,
                vehicleId = vehicleId,
                productId = productId,
                amount = amount,
                subscriptionId = subscriptionId,
                valid = valid,
                unknownConstant = unknownConstant,
                unknownConstant2 = unknownConstant2,
                errorMessage = errorMessage
            )
        }
    }
}
