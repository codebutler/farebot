/*
 * AdelaideTransitFactory.kt
 *
 * Copyright 2015 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google
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

package com.codebutler.farebot.transit.adelaide

import com.codebutler.farebot.base.util.DefaultStringResource
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.base.util.byteArrayToLongReversed
import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.card.desfire.DesfireCard
import com.codebutler.farebot.card.desfire.StandardDesfireFile
import com.codebutler.farebot.transit.TransactionTrip
import com.codebutler.farebot.transit.Transaction
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.calypso.IntercodeFields
import com.codebutler.farebot.transit.en1545.En1545Parser
import farebot.farebot_transit_adelaide.generated.resources.Res
import farebot.farebot_transit_adelaide.generated.resources.card_name_adelaide
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

class AdelaideTransitFactory(
    private val stringResource: StringResource = DefaultStringResource()
) : TransitFactory<DesfireCard, AdelaideTransitInfo> {

    override fun check(card: DesfireCard): Boolean {
        return card.getApplication(APP_ID) != null
    }

    override fun parseIdentity(card: DesfireCard): TransitIdentity {
        return TransitIdentity.create(
            runBlocking { getString(Res.string.card_name_adelaide) },
            AdelaideTransitInfo.formatSerial(getSerial(card.tagId))
        )
    }

    override fun parseInfo(card: DesfireCard): AdelaideTransitInfo {
        val app = card.getApplication(APP_ID)!!

        // 0 = TICKETING_ENVIRONMENT
        val envFile = app.getFile(0) as? StandardDesfireFile
        val parsed = if (envFile != null) {
            En1545Parser.parse(envFile.data, IntercodeFields.TICKET_ENV_FIELDS)
        } else null

        val transactionList = mutableListOf<Transaction>()

        // Transaction log files: 3-6, 9, 0xa, 0xb
        for (fileId in intArrayOf(3, 4, 5, 6, 9, 0xa, 0xb)) {
            val file = app.getFile(fileId) as? StandardDesfireFile ?: continue
            val data = file.data
            if (data.getBitsFromBuffer(0, 14) == 0) continue
            transactionList.add(AdelaideTransaction(data))
        }

        val subs = mutableListOf<AdelaideSubscription>()
        var purse: AdelaideSubscription? = null

        // Contract files: 0x10-0x13
        for (fileId in intArrayOf(0x10, 0x11, 0x12, 0x13)) {
            val file = app.getFile(fileId) as? StandardDesfireFile ?: continue
            val data = file.data
            if (data.getBitsFromBuffer(0, 7) == 0) continue
            val sub = AdelaideSubscription.parse(data, stringResource)
            if (sub.isPurse) {
                purse = sub
            } else {
                subs.add(sub)
            }
        }

        return AdelaideTransitInfo(
            purse = purse,
            serial = getSerial(card.tagId),
            subscriptions = if (subs.isNotEmpty()) subs else null,
            trips = TransactionTrip.merge(transactionList)
        )
    }

    companion object {
        private const val APP_ID = 0xb006f2

        private fun getSerial(tagId: ByteArray): Long =
            tagId.byteArrayToLongReversed(1, 6)
    }
}
