/*
 * MRTJTransitFactory.kt
 *
 * Copyright 2019 Bondan Sumbodo <sybond@gmail.com>
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

package com.codebutler.farebot.transit.mrtj

import com.codebutler.farebot.card.felica.FelicaCard
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import farebot.farebot_transit_mrtj.generated.resources.Res
import farebot.farebot_transit_mrtj.generated.resources.mrtj_longname
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

class MRTJTransitFactory : TransitFactory<FelicaCard, MRTJTransitInfo> {

    companion object {
        private const val SYSTEMCODE_MRTJ = 0x9373
        private const val SERVICE_MRTJ_ID = 0x100B
        private const val SERVICE_MRTJ_BALANCE = 0x10D7
    }

    override fun check(card: FelicaCard): Boolean {
        return card.getSystem(SYSTEMCODE_MRTJ) != null
    }

    override fun parseIdentity(card: FelicaCard): TransitIdentity {
        return TransitIdentity.create(runBlocking { getString(Res.string.mrtj_longname) }, "")
    }

    override fun parseInfo(card: FelicaCard): MRTJTransitInfo {
        val serviceBalance = card.getSystem(SYSTEMCODE_MRTJ)?.getService(SERVICE_MRTJ_BALANCE)
        val dataBalance = serviceBalance?.blocks?.get(0)?.data

        val currentBalance = if (dataBalance != null) {
            byteArrayToIntReversed(dataBalance, 0, 4)
        } else 0

        val transactionCounter = if (dataBalance != null) {
            byteArrayToInt(dataBalance, 13, 3)
        } else 0

        val lastTransAmount = if (dataBalance != null) {
            byteArrayToIntReversed(dataBalance, 4, 4)
        } else 0

        return MRTJTransitInfo(
            currentBalance = currentBalance,
            transactionCounter = transactionCounter,
            lastTransAmount = lastTransAmount
        )
    }

    private fun byteArrayToIntReversed(data: ByteArray, offset: Int, length: Int): Int {
        var result = 0
        for (i in (length - 1) downTo 0) {
            result = result shl 8
            result = result or (data[offset + i].toInt() and 0xFF)
        }
        return result
    }

    private fun byteArrayToInt(data: ByteArray, offset: Int, length: Int): Int {
        var result = 0
        for (i in 0 until length) {
            result = result shl 8
            result = result or (data[offset + i].toInt() and 0xFF)
        }
        return result
    }
}
