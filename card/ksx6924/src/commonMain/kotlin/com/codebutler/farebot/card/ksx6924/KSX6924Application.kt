/*
 * KSX6924Application.kt
 *
 * Copyright 2018 Google
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

package com.codebutler.farebot.card.ksx6924

import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.ui.ListItemRecursive
import com.codebutler.farebot.base.util.hex
import com.codebutler.farebot.base.util.hexString
import com.codebutler.farebot.base.util.isAllFF
import com.codebutler.farebot.base.util.isAllZero
import com.codebutler.farebot.base.util.toHexDump
import com.codebutler.farebot.card.iso7816.ISO7816Application
import com.codebutler.farebot.card.iso7816.ISO7816TLV
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Represents a KSX6924 (T-Money) application on an ISO7816 card.
 *
 * This is used by T-Money in South Korea, and Snapper Plus cards in Wellington, New Zealand.
 *
 * @property application The underlying ISO7816 application data.
 * @property balance The balance data returned by the GET BALANCE command.
 * @property extraRecords Additional proprietary records from the card.
 */
@Serializable
data class KSX6924Application(
    val application: ISO7816Application,
    @Contextual val balance: ByteArray,
    val extraRecords: List<@Contextual ByteArray> = emptyList(),
) {
    /**
     * Returns the transaction records from the card.
     */
    val transactionRecords: List<ByteArray>?
        get() {
            // Try SFI first, then fall back to file path
            val sfiFile = application.getSfiFile(TRANSACTION_FILE)
            if (sfiFile != null) {
                return sfiFile.records.values.toList()
            }

            // Try file path format
            val fileSelector = "${FILE_NAME.hex()}/${TRANSACTION_FILE.hexString}"
            val file = application.getFile(fileSelector)
            return file?.records?.values?.toList()
        }

    /**
     * Returns the purse info data from the FCI.
     */
    private val purseInfoData: ByteArray?
        get() =
            application.appFci?.let { fci ->
                ISO7816TLV.findBERTLV(fci, TAG_PURSE_INFO, false)
            }

    /**
     * Returns the parsed purse info.
     */
    val purseInfo: KSX6924PurseInfo?
        get() = purseInfoData?.let { KSX6924PurseInfo(it) }

    /**
     * Returns the card serial number.
     */
    val serial: String?
        get() = purseInfo?.serial

    /**
     * Returns the raw data for display in the UI.
     */
    val rawData: List<ListItemInterface>
        get() {
            val sli = mutableListOf<ListItemInterface>()
            sli.add(ListItemRecursive.collapsedValue("T-Money Balance", balance.toHexDump()))

            for (i in extraRecords.indices) {
                val d = extraRecords[i]
                val title =
                    if (d.isAllZero() || d.isAllFF()) {
                        "Record $i (empty)"
                    } else {
                        "Record $i"
                    }
                sli.add(ListItemRecursive.collapsedValue(title, d.toHexDump()))
            }

            return sli.toList()
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as KSX6924Application
        if (application != other.application) return false
        if (!balance.contentEquals(other.balance)) return false
        if (extraRecords.size != other.extraRecords.size) return false
        for (i in extraRecords.indices) {
            if (!extraRecords[i].contentEquals(other.extraRecords[i])) return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = application.hashCode()
        result = 31 * result + balance.contentHashCode()
        result = 31 * result + extraRecords.sumOf { it.contentHashCode() }
        return result
    }

    companion object {
        private const val TAG = "KSX6924Application"

        /**
         * Application names (AIDs) that identify a KSX6924 card.
         */
        @OptIn(ExperimentalStdlibApi::class)
        val APP_NAMES: List<ByteArray> =
            listOf(
                // T-Money, Snapper
                "d4100000030001".hexToByteArray(),
                // Cashbee / eB
                "d4100000140001".hexToByteArray(),
                // MOIBA (untested)
                "d4100000300001".hexToByteArray(),
                // K-Cash (untested)
                "d4106509900020".hexToByteArray(),
            )

        @OptIn(ExperimentalStdlibApi::class)
        val FILE_NAME: ByteArray = "d4100000030001".hexToByteArray()

        @OptIn(ExperimentalStdlibApi::class)
        val TAG_PURSE_INFO: ByteArray = "b0".hexToByteArray()

        const val INS_GET_BALANCE: Byte = 0x4c
        const val INS_GET_RECORD: Byte = 0x78
        const val BALANCE_RESP_LEN: Byte = 4
        const val TRANSACTION_FILE = 4

        const val TYPE = "ksx6924"
        const val OLD_TYPE = "tmoney"

        /**
         * Checks if the given application is a KSX6924 application.
         */
        @OptIn(ExperimentalStdlibApi::class)
        fun isKSX6924(app: ISO7816Application): Boolean {
            val appName = app.appName ?: return false
            return APP_NAMES.any { it.contentEquals(appName) }
        }
    }
}
