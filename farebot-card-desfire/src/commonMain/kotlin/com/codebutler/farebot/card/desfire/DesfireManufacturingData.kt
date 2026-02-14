/*
 * DesfireManufacturingData.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014, 2016 Eric Butler <eric@codebutler.com>
 *
 * Contains improvements ported from Metrodroid (https://github.com/metrodroid/metrodroid)
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

package com.codebutler.farebot.card.desfire

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@OptIn(ExperimentalStdlibApi::class)
@Serializable
data class DesfireManufacturingData(
    val hwVendorID: Int,
    val hwType: Int,
    val hwSubType: Int,
    val hwMajorVersion: Int,
    val hwMinorVersion: Int,
    val hwStorageSize: Int,
    val hwProtocol: Int,
    val swVendorID: Int,
    val swType: Int,
    val swSubType: Int,
    val swMajorVersion: Int,
    val swMinorVersion: Int,
    val swStorageSize: Int,
    val swProtocol: Int,
    @Contextual val uid: ByteArray,
    @Contextual val batchNo: ByteArray,
    val weekProd: Int,
    val yearProd: Int,
) {
    val uidHex: String
        get() = uid.toHexString()

    val batchNoHex: String
        get() = batchNo.toHexString()

    companion object {
        fun create(data: ByteArray): DesfireManufacturingData {
            var offset = 0
            val hwVendorID = data[offset++].toInt() and 0xFF
            val hwType = data[offset++].toInt() and 0xFF
            val hwSubType = data[offset++].toInt() and 0xFF
            val hwMajorVersion = data[offset++].toInt() and 0xFF
            val hwMinorVersion = data[offset++].toInt() and 0xFF
            val hwStorageSize = data[offset++].toInt() and 0xFF
            val hwProtocol = data[offset++].toInt() and 0xFF

            val swVendorID = data[offset++].toInt() and 0xFF
            val swType = data[offset++].toInt() and 0xFF
            val swSubType = data[offset++].toInt() and 0xFF
            val swMajorVersion = data[offset++].toInt() and 0xFF
            val swMinorVersion = data[offset++].toInt() and 0xFF
            val swStorageSize = data[offset++].toInt() and 0xFF
            val swProtocol = data[offset++].toInt() and 0xFF

            val uid = data.copyOfRange(offset, offset + 7)
            offset += 7

            val batchNo = data.copyOfRange(offset, offset + 5)
            offset += 5

            val weekProd = data[offset++].toInt() and 0xFF
            val yearProd = data[offset++].toInt() and 0xFF

            return DesfireManufacturingData(
                hwVendorID = hwVendorID,
                hwType = hwType,
                hwSubType = hwSubType,
                hwMajorVersion = hwMajorVersion,
                hwMinorVersion = hwMinorVersion,
                hwStorageSize = hwStorageSize,
                hwProtocol = hwProtocol,
                swVendorID = swVendorID,
                swType = swType,
                swSubType = swSubType,
                swMajorVersion = swMajorVersion,
                swMinorVersion = swMinorVersion,
                swStorageSize = swStorageSize,
                swProtocol = swProtocol,
                uid = uid,
                batchNo = batchNo,
                weekProd = weekProd,
                yearProd = yearProd,
            )
        }
    }
}
