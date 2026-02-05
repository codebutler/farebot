/*
 * OpusTransitInfo.kt
 *
 * Copyright 2018 Google
 * Copyright 2025 Eric Butler
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

package com.codebutler.farebot.transit.calypso.opus

import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.card.iso7816.ISO7816Application
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.calypso.CalypsoTransitFactory
import com.codebutler.farebot.transit.calypso.CalypsoTransitInfo
import com.codebutler.farebot.transit.calypso.IntercodeFields
import com.codebutler.farebot.transit.en1545.Calypso1545TransitData
import com.codebutler.farebot.transit.en1545.CalypsoConstants
import com.codebutler.farebot.transit.en1545.CalypsoParseResult
import com.codebutler.farebot.transit.en1545.En1545Bitmap
import com.codebutler.farebot.transit.en1545.En1545Container
import com.codebutler.farebot.transit.en1545.En1545FixedInteger
import com.codebutler.farebot.transit.en1545.En1545Parser
import com.codebutler.farebot.transit.en1545.En1545Repeat
import com.codebutler.farebot.transit.en1545.En1545TransitData
import com.codebutler.farebot.transit.en1545.getBitsFromBuffer

internal class OpusTransitInfo(
    result: CalypsoParseResult
) : CalypsoTransitInfo(result) {

    override val cardName: String = NAME

    companion object {
        const val NAME = "Opus"
        const val NETWORK_ID = 0x124001

        val TICKET_ENV_FIELDS = En1545Container(
            IntercodeFields.TICKET_ENV_FIELDS,
            En1545Bitmap(
                En1545Container(
                    En1545FixedInteger(En1545TransitData.HOLDER_UNKNOWN_A, 3),
                    En1545FixedInteger.dateBCD(En1545TransitData.HOLDER_BIRTH_DATE),
                    En1545FixedInteger(En1545TransitData.HOLDER_UNKNOWN_B, 13),
                    En1545FixedInteger.date(En1545TransitData.HOLDER_PROFILE),
                    En1545FixedInteger(En1545TransitData.HOLDER_UNKNOWN_C, 8)
                ),
                // Possibly part of HolderUnknownB or HolderUnknownC
                En1545FixedInteger(En1545TransitData.HOLDER_UNKNOWN_D, 8)
            )
        )

        val CONTRACT_LIST_FIELDS = En1545Repeat(
            4,
            En1545Bitmap(
                En1545FixedInteger(En1545TransitData.CONTRACTS_PROVIDER, 8),
                En1545FixedInteger(En1545TransitData.CONTRACTS_TARIFF, 16),
                En1545FixedInteger(En1545TransitData.CONTRACTS_UNKNOWN_A, 4),
                En1545FixedInteger(En1545TransitData.CONTRACTS_POINTER, 5)
            )
        )
    }
}

class OpusTransitFactory(stringResource: StringResource) : CalypsoTransitFactory(stringResource) {

    override val name: String
        get() = OpusTransitInfo.NAME

    override fun checkTenv(tenv: ByteArray): Boolean {
        val networkId = tenv.getBitsFromBuffer(13, 24)
        return networkId == OpusTransitInfo.NETWORK_ID
    }

    override fun getSerial(app: ISO7816Application): String? {
        val iccFile = app.sfiFiles[0x02]
            ?: return null
        val record = iccFile.records[1] ?: return null

        // Try bytes 16..20 first
        if (record.size >= 20) {
            val serial = record.byteArrayToLong(16, 4)
            if (serial != 0L) {
                return serial.toString()
            }
        }

        // Fallback to bytes 0..4
        if (record.size >= 4) {
            val serial = record.byteArrayToLong(0, 4)
            if (serial != 0L) {
                return serial.toString()
            }
        }

        return null
    }

    override fun parseTransitInfo(app: ISO7816Application, serial: String?): TransitInfo {
        // Contracts 2 is a copy of contract list on opus
        val contracts = Calypso1545TransitData.getSfiRecords(
            app, CalypsoConstants.SFI_TICKETING_CONTRACTS_1
        )

        val result = Calypso1545TransitData.parse(
            app = app,
            ticketEnvFields = OpusTransitInfo.TICKET_ENV_FIELDS,
            contractListFields = OpusTransitInfo.CONTRACT_LIST_FIELDS,
            serial = serial,
            createSubscription = { data, ctr, _, _ ->
                if (ctr == null) null
                else OpusSubscription(
                    parsed = En1545Parser.parse(data, OpusSubscription.FIELDS),
                    stringResource = stringResource,
                    ctr = ctr
                )
            },
            createTrip = { data ->
                OpusTransaction(
                    parsed = En1545Parser.parse(data, OpusTransaction.FIELDS)
                )
            },
            contracts = contracts
        )

        return OpusTransitInfo(result)
    }

    private fun ByteArray.byteArrayToLong(offset: Int, length: Int): Long {
        var result = 0L
        for (i in 0 until length) {
            result = (result shl 8) or (this[offset + i].toLong() and 0xFF)
        }
        return result
    }
}
