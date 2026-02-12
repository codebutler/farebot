/*
 * MykiTransitFactory.kt
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.myki

import com.codebutler.farebot.base.util.ByteUtils
import com.codebutler.farebot.base.util.Luhn
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.desfire.DesfireCard
import com.codebutler.farebot.card.desfire.StandardDesfireFile
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import farebot.farebot_transit_myki.generated.resources.*

class MykiTransitFactory : TransitFactory<DesfireCard, MykiTransitInfo> {

    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override fun check(card: DesfireCard): Boolean {
        return (card.getApplication(4594) != null) && (card.getApplication(15732978) != null)
    }

    override fun parseIdentity(card: DesfireCard): TransitIdentity {
        var data = (card.getApplication(4594)!!.getFile(15) as StandardDesfireFile).data
        data = ByteUtils.reverseBuffer(data, 0, 16)

        val serialNumber1 = ByteUtils.getBitsFromBuffer(data, 96, 32).toLong()
        val serialNumber2 = ByteUtils.getBitsFromBuffer(data, 64, 32).toLong()
        return TransitIdentity.create(MykiTransitInfo.NAME, formatSerialNumber(serialNumber1, serialNumber2))
    }

    override fun parseInfo(card: DesfireCard): MykiTransitInfo {
        try {
            val data = (card.getApplication(4594)!!.getFile(15) as StandardDesfireFile).data
            val metadata = ByteUtils.reverseBuffer(data, 0, 16)
            val serialNumber1 = ByteUtils.getBitsFromBuffer(metadata, 96, 32)
            val serialNumber2 = ByteUtils.getBitsFromBuffer(metadata, 64, 32)
            return MykiTransitInfo.create(formatSerialNumber(serialNumber1.toLong(), serialNumber2.toLong()))
        } catch (ex: Exception) {
            throw RuntimeException("Error parsing Myki data", ex)
        }
    }

    companion object {
        private val CARD_INFO = CardInfo(
            nameRes = Res.string.myki_card_name,
            cardType = CardType.MifareDesfire,
            region = TransitRegion.AUSTRALIA,
            locationRes = Res.string.myki_location,
            imageRes = Res.drawable.myki_card,
            latitude = -37.8136f,
            longitude = 144.9631f,
            brandColor = 0x89961C,
            credits = listOf("Michael Farrell"),
            serialOnly = true,
            sampleDumpFile = "Myki.json",
        )

        private fun formatSerialNumber(serialNumber1: Long, serialNumber2: Long): String {
            val formattedSerial = "${serialNumber1.toString().padStart(6, '0')}${serialNumber2.toString().padStart(8, '0')}"
            return formattedSerial + Luhn.calculateLuhn(formattedSerial)
        }
    }
}
