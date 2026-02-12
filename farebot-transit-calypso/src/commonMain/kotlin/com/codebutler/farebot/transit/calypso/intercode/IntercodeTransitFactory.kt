/*
 * IntercodeTransitFactory.kt
 *
 * Copyright 2018 Google
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

package com.codebutler.farebot.transit.calypso.intercode

import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.iso7816.ISO7816Application
import com.codebutler.farebot.card.iso7816.ISO7816Card
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.TransitRegion
import com.codebutler.farebot.transit.calypso.CalypsoTransitFactory
import com.codebutler.farebot.transit.calypso.IntercodeFields
import com.codebutler.farebot.transit.en1545.Calypso1545TransitData
import com.codebutler.farebot.transit.en1545.CalypsoConstants
import com.codebutler.farebot.transit.en1545.En1545Parsed
import com.codebutler.farebot.transit.en1545.En1545TransitData
import com.codebutler.farebot.transit.en1545.getBitsFromBuffer
import farebot.farebot_transit_calypso.generated.resources.*

class IntercodeTransitFactory(stringResource: StringResource) : CalypsoTransitFactory(stringResource) {

    override val allCards: List<CardInfo>
        get() = ALL_CARDS

    override val name: String
        get() = "Intercode"

    override fun parseIdentity(card: ISO7816Card): TransitIdentity {
        val app = findCalypsoApp(card)!!
        val tenvFile = app.sfiFiles[CalypsoConstants.SFI_TICKETING_ENVIRONMENT] ?: return TransitIdentity.create(name, getSerial(app))
        val tenv = tenvFile.records.entries.sortedBy { it.key }.firstOrNull()?.value ?: return TransitIdentity.create(name, getSerial(app))
        val netId = try {
            tenv.getBitsFromBuffer(13, 24)
        } catch (_: Exception) {
            0
        }
        val cardName = IntercodeTransitInfo.getCardName(netId, tenv)
        return TransitIdentity.create(cardName, getSerial(app))
    }

    override fun checkTenv(tenv: ByteArray): Boolean {
        return try {
            val netId = tenv.getBitsFromBuffer(13, 24)
            IntercodeTransitInfo.isIntercode(netId)
        } catch (_: Exception) {
            false
        }
    }

    override fun getSerial(app: ISO7816Application): String? {
        val iccFile = app.sfiFiles[0x02] ?: return null
        val record = iccFile.records[1] ?: return null

        val tenvFile = app.sfiFiles[0x07] ?: return null
        val tenv = tenvFile.records.entries.sortedBy { it.key }.firstOrNull()?.value ?: return null
        val netId = try {
            tenv.getBitsFromBuffer(13, 24)
        } catch (_: Exception) {
            0
        }

        if (netId == 0x250502) {
            // OuRA card has a special serial format
            return record.toHexString(20, 6).substring(1, 11)
        }

        if (record.size >= 20) {
            val serial = record.byteArrayToLong(16, 4)
            if (serial != 0L) return serial.toString()
        }

        if (record.size >= 4) {
            val serial = record.byteArrayToLong(0, 4)
            if (serial != 0L) return serial.toString()
        }

        return null
    }

    override fun parseTransitInfo(app: ISO7816Application, serial: String?): TransitInfo {
        val ticketEnv = Calypso1545TransitData.parseTicketEnv(
            app, IntercodeFields.TICKET_ENV_HOLDER_FIELDS
        )
        val netID = ticketEnv.getIntOrZero(En1545TransitData.ENV_NETWORK_ID)

        val result = Calypso1545TransitData.parse(
            app = app,
            ticketEnvFields = IntercodeFields.TICKET_ENV_HOLDER_FIELDS,
            contractListFields = IntercodeFields.CONTRACT_LIST_FIELDS,
            serial = serial,
            createSubscription = { data, counter, list, listnum ->
                createSubscription(data, list, listnum, netID, counter)
            },
            createTrip = { data -> IntercodeTransaction.parse(data, netID) },
            createSpecialEvent = { data -> IntercodeTransaction.parse(data, netID) }
        )

        return IntercodeTransitInfo(result)
    }

    private fun createSubscription(
        data: ByteArray,
        contractList: En1545Parsed?,
        listNum: Int?,
        netID: Int,
        counter: Int?
    ): IntercodeSubscription? {
        if (contractList == null || listNum == null)
            return null
        val tariff = contractList.getInt(En1545TransitData.CONTRACTS_TARIFF, listNum) ?: return null
        return IntercodeSubscription.parse(data, tariff shr 4 and 0xff, netID, counter, stringResource)
    }

    private fun ByteArray.byteArrayToLong(offset: Int, length: Int): Long {
        var result = 0L
        for (i in 0 until length) {
            if (offset + i >= size) return 0L
            result = (result shl 8) or (this[offset + i].toLong() and 0xFF)
        }
        return result
    }

    private fun ByteArray.toHexString(offset: Int, length: Int): String {
        val sb = StringBuilder()
        for (i in offset until (offset + length).coerceAtMost(size)) {
            val b = this[i].toInt() and 0xFF
            sb.append(HEX_CHARS[b shr 4])
            sb.append(HEX_CHARS[b and 0x0F])
        }
        return sb.toString()
    }

    private val HEX_CHARS = "0123456789abcdef".toCharArray()

    companion object {
        private val ALL_CARDS = listOf(
            CardInfo(
                nameRes = Res.string.card_name_navigo,
                cardType = CardType.ISO7816,
                region = TransitRegion.FRANCE,
                locationRes = Res.string.card_location_paris_france,
                imageRes = Res.drawable.navigo,
                latitude = 48.8566f,
                longitude = 2.3522f,
                brandColor = 0x92D6FE,
            ),
            CardInfo(
                nameRes = Res.string.card_name_oura,
                cardType = CardType.ISO7816,
                region = TransitRegion.FRANCE,
                locationRes = Res.string.card_location_grenoble_france,
                imageRes = Res.drawable.oura,
                latitude = 45.1885f,
                longitude = 5.7245f,
                brandColor = 0x005AA7,
            ),
            CardInfo(
                nameRes = Res.string.card_name_pastel,
                cardType = CardType.ISO7816,
                region = TransitRegion.FRANCE,
                locationRes = Res.string.card_location_toulouse_france,
                preview = true,
                imageRes = Res.drawable.pastel,
                latitude = 43.6047f,
                longitude = 1.4442f,
                brandColor = 0x285999,
            ),
            CardInfo(
                nameRes = Res.string.card_name_pass_pass,
                cardType = CardType.ISO7816,
                region = TransitRegion.FRANCE,
                locationRes = Res.string.card_location_hauts_de_france,
                preview = true,
                imageRes = Res.drawable.passpass,
                latitude = 50.6292f,
                longitude = 3.0573f,
                brandColor = 0x4E2D8D,
            ),
            CardInfo(
                nameRes = Res.string.card_name_transgironde,
                cardType = CardType.ISO7816,
                region = TransitRegion.FRANCE,
                locationRes = Res.string.card_location_gironde_france,
                preview = true,
                imageRes = Res.drawable.transgironde,
                latitude = 44.8378f,
                longitude = -0.5792f,
                brandColor = 0xE39A45,
            ),
            CardInfo(
                nameRes = Res.string.card_name_tam,
                cardType = CardType.ISO7816,
                region = TransitRegion.FRANCE,
                locationRes = Res.string.card_location_montpellier_france,
                imageRes = Res.drawable.tam_montpellier,
                latitude = 43.6108f,
                longitude = 3.8767f,
                brandColor = 0x357828,
            ),
            CardInfo(
                nameRes = Res.string.card_name_korrigo,
                cardType = CardType.ISO7816,
                region = TransitRegion.FRANCE,
                locationRes = Res.string.card_location_brittany_france,
                imageRes = Res.drawable.korrigo,
                latitude = 48.1173f,
                longitude = -1.6778f,
                brandColor = 0xAB7423,
            ),
            CardInfo(
                nameRes = Res.string.card_name_envibus,
                cardType = CardType.ISO7816,
                region = TransitRegion.FRANCE,
                locationRes = Res.string.card_location_sophia_antipolis_france,
                imageRes = Res.drawable.envibus,
                latitude = 43.6163f,
                longitude = 7.0552f,
                brandColor = 0xE1047A,
            ),
        )
    }
}
