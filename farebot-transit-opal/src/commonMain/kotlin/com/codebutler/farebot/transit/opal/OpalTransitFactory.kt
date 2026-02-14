/*
 * OpalTransitFactory.kt
 *
 * Copyright 2015 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.opal

import com.codebutler.farebot.base.util.ByteUtils
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.desfire.DesfireCard
import com.codebutler.farebot.card.desfire.StandardDesfireFile
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import farebot.farebot_transit_opal.generated.resources.*

/**
 * Transit data type for Opal (Sydney, AU).
 *
 * This uses the publicly-readable file on the card (7) in order to get the data.
 *
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/Opal
 */
class OpalTransitFactory(
    private val stringResource: StringResource,
) : TransitFactory<DesfireCard, OpalTransitInfo> {
    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override fun check(card: DesfireCard): Boolean = card.getApplication(0x314553) != null

    override fun parseIdentity(card: DesfireCard): TransitIdentity {
        var data = (card.getApplication(0x314553)!!.getFile(0x07) as StandardDesfireFile).data
        data = ByteUtils.reverseBuffer(data, 0, 5)

        val lastDigit = ByteUtils.getBitsFromBuffer(data, 4, 4)
        val serialNumber = ByteUtils.getBitsFromBuffer(data, 8, 32)
        return TransitIdentity.create(OpalTransitInfo.NAME, formatSerialNumber(serialNumber, lastDigit))
    }

    override fun parseInfo(card: DesfireCard): OpalTransitInfo {
        try {
            var data = (card.getApplication(0x314553)!!.getFile(0x07) as StandardDesfireFile).data

            data = ByteUtils.reverseBuffer(data, 0, 16)

            val checksum = ByteUtils.getBitsFromBuffer(data, 0, 16)
            val weeklyTrips = ByteUtils.getBitsFromBuffer(data, 16, 4)
            val autoTopup = ByteUtils.getBitsFromBuffer(data, 20, 1) == 0x01
            val actionType = ByteUtils.getBitsFromBuffer(data, 21, 4)
            val vehicleType = ByteUtils.getBitsFromBuffer(data, 25, 3)
            val minute = ByteUtils.getBitsFromBuffer(data, 28, 11)
            val day = ByteUtils.getBitsFromBuffer(data, 39, 15)
            val iRawBalance = ByteUtils.getBitsFromBuffer(data, 54, 21)
            val transactionNumber = ByteUtils.getBitsFromBuffer(data, 75, 16)
            // Skip bit here
            val lastDigit = ByteUtils.getBitsFromBuffer(data, 92, 4)
            val serialNumber = ByteUtils.getBitsFromBuffer(data, 96, 32)

            val balance = ByteUtils.unsignedToTwoComplement(iRawBalance, 20)

            return OpalTransitInfo(
                serialNumber = formatSerialNumber(serialNumber, lastDigit),
                balanceValue = balance,
                checksum = checksum,
                weeklyTrips = weeklyTrips,
                autoTopup = autoTopup,
                lastTransaction = actionType,
                lastTransactionMode = vehicleType,
                minute = minute,
                day = day,
                lastTransactionNumber = transactionNumber,
                stringResource = stringResource,
            )
        } catch (ex: Exception) {
            throw RuntimeException("Error parsing Opal data", ex)
        }
    }

    companion object {
        private val CARD_INFO =
            CardInfo(
                nameRes = Res.string.transit_opal_card_name,
                cardType = CardType.MifareDesfire,
                region = TransitRegion.AUSTRALIA,
                locationRes = Res.string.location_sydney,
                imageRes = Res.drawable.opal_card,
                latitude = -33.8688f,
                longitude = 151.2093f,
                brandColor = 0x3298D6,
                credits = listOf("Michael Farrell"),
                sampleDumpFile = "Opal.json",
                extraNoteRes = Res.string.card_note_opal,
            )

        private fun formatSerialNumber(
            serialNumber: Int,
            lastDigit: Int,
        ): String =
            NumberUtils.formatNumber(
                3085_2200_0000_0000L + (serialNumber * 10L) + lastDigit,
                " ",
                4,
                4,
                4,
                4,
            )
    }
}
