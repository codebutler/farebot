/*
 * OrcaTransitFactory.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014-2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2015 Sean CyberKitsune McClenaghan <cyberkitsune09@gmail.com>
 * Copyright (C) 2018 Michael Farrell <micolous+git@gmail.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
 *
 * Thanks to:
 * Karl Koscher <supersat@cs.washington.edu>
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

package com.codebutler.farebot.transit.orca

import com.codebutler.farebot.base.util.ByteUtils
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.desfire.DesfireCard
import com.codebutler.farebot.card.desfire.RecordDesfireFile
import com.codebutler.farebot.card.desfire.StandardDesfireFile
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransactionTrip
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import com.codebutler.farebot.transit.Trip
import farebot.farebot_transit_orca.generated.resources.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

class OrcaTransitFactory(private val stringResource: StringResource) : TransitFactory<DesfireCard, OrcaTransitInfo> {

    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override fun check(card: DesfireCard): Boolean {
        return card.getApplication(APP_ID) != null
    }

    override fun parseIdentity(card: DesfireCard): TransitIdentity {
        try {
            val data = (card.getApplication(0xffffff)!!.getFile(0x0f) as StandardDesfireFile).data
            val cardName = runBlocking { getString(Res.string.transit_orca_card_name) }
            return TransitIdentity.create(cardName, ByteUtils.byteArrayToInt(data, 4, 4).toString())
        } catch (ex: Exception) {
            throw RuntimeException("Error parsing ORCA serial", ex)
        }
    }

    override fun parseInfo(card: DesfireCard): OrcaTransitInfo {
        val serialNumber: Int
        val balance: Int

        try {
            val data = (card.getApplication(0xffffff)!!.getFile(0x0f) as StandardDesfireFile).data
            serialNumber = ByteUtils.byteArrayToInt(data, 4, 4)
        } catch (ex: Exception) {
            throw RuntimeException("Error parsing ORCA serial", ex)
        }

        try {
            val data = (card.getApplication(APP_ID)!!.getFile(0x04) as StandardDesfireFile).data
            balance = ByteUtils.byteArrayToInt(data, 41, 2)
        } catch (ex: Exception) {
            throw RuntimeException("Error parsing ORCA balance", ex)
        }

        val trips = parseTrips(card, 0x02, false) + parseTrips(card, 0x03, true)

        return OrcaTransitInfo(trips, serialNumber, balance)
    }

    private fun parseTrips(card: DesfireCard, fileId: Int, isTopup: Boolean): List<Trip> {
        val file = card.getApplication(APP_ID)?.getFile(fileId)
        if (file !is RecordDesfireFile) return emptyList()

        val transactions = file.records.map { record ->
            OrcaTransaction.parse(record.data, isTopup, stringResource)
        }
        return TransactionTrip.merge(transactions)
    }

    companion object {
        const val APP_ID = 0x3010f2

        private val CARD_INFO = CardInfo(
            name = "ORCA",
            cardType = CardType.MifareDesfire,
            region = TransitRegion.USA,
            locationId = Res.string.location_seattle
        )
    }
}
