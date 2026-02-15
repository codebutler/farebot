/*
 * RavKavTransitInfo.kt
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

package com.codebutler.farebot.transit.calypso.ravkav

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.iso7816.ISO7816Application
import com.codebutler.farebot.card.iso7816.ISO7816TLV
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.TransitRegion
import com.codebutler.farebot.transit.calypso.CalypsoTransitFactory
import com.codebutler.farebot.transit.calypso.CalypsoTransitInfo
import com.codebutler.farebot.transit.en1545.Calypso1545TransitData
import com.codebutler.farebot.transit.en1545.CalypsoParseResult
import com.codebutler.farebot.transit.en1545.En1545Container
import com.codebutler.farebot.transit.en1545.En1545FixedHex
import com.codebutler.farebot.transit.en1545.En1545FixedInteger
import com.codebutler.farebot.transit.en1545.En1545Parser
import com.codebutler.farebot.transit.en1545.En1545TransitData
import com.codebutler.farebot.transit.en1545.getBitsFromBuffer
import farebot.transit.calypso.generated.resources.*

// Reference: https://github.com/L1L1/cardpeek/blob/master/dot_cardpeek_dir/scripts/calypso/c376n3.lua
internal class RavKavTransitInfo(
    result: CalypsoParseResult,
) : CalypsoTransitInfo(result) {
    override val cardName: FormattedString = FormattedString(NAME)

    override val info: List<ListItemInterface>
        get() {
            val items = mutableListOf<ListItemInterface>()
            val holderId = result.ticketEnv.getIntOrZero(En1545TransitData.HOLDER_ID_NUMBER)
            if (holderId == 0) {
                items.add(ListItem(Res.string.calypso_card_type, Res.string.calypso_card_type_anonymous))
            } else {
                items.add(ListItem(Res.string.calypso_card_type, Res.string.calypso_card_type_personal))
                items.add(ListItem(Res.string.calypso_holder_id, holderId.toString()))
            }
            return items
        }

    companion object {
        const val NAME = "Rav-Kav"
        const val NETWORK_ID_1 = 0x37602
        const val NETWORK_ID_2 = 0x37603

        val TICKET_ENV_FIELDS =
            En1545Container(
                En1545FixedInteger(En1545TransitData.ENV_VERSION_NUMBER, 3),
                En1545FixedInteger(En1545TransitData.ENV_NETWORK_ID, 20),
                En1545FixedInteger(En1545TransitData.ENV_UNKNOWN_A, 26),
                En1545FixedInteger.date(En1545TransitData.ENV_APPLICATION_ISSUE),
                En1545FixedInteger.date(En1545TransitData.ENV_APPLICATION_VALIDITY_END),
                En1545FixedInteger("PayMethod", 3),
                En1545FixedInteger.dateBCD(En1545TransitData.HOLDER_BIRTH_DATE),
                En1545FixedHex(En1545TransitData.ENV_UNKNOWN_B, 44),
                En1545FixedInteger(En1545TransitData.HOLDER_ID_NUMBER, 30),
            )
    }
}

class RavKavTransitFactory : CalypsoTransitFactory() {
    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override val name: FormattedString
        get() = FormattedString(RavKavTransitInfo.NAME)

    override fun checkTenv(tenv: ByteArray): Boolean {
        val networkId = tenv.getBitsFromBuffer(3, 20)
        return networkId == RavKavTransitInfo.NETWORK_ID_1 || networkId == RavKavTransitInfo.NETWORK_ID_2
    }

    override fun getSerial(app: ISO7816Application): String? {
        val fci = app.appFci ?: return null
        val bf0c = ISO7816TLV.findBERTLV(fci, "bf0c", keepHeader = true) ?: return null
        val c7 = ISO7816TLV.findBERTLV(bf0c, "c7") ?: return null
        if (c7.size < 8) return null
        return c7.byteArrayToLong(4, 4).toString()
    }

    override fun parseTransitInfo(
        app: ISO7816Application,
        serial: String?,
    ): TransitInfo {
        val result =
            Calypso1545TransitData.parse(
                app = app,
                ticketEnvFields = RavKavTransitInfo.TICKET_ENV_FIELDS,
                contractListFields = null,
                serial = serial,
                createSubscription = { data, ctr, _, _ ->
                    RavKavSubscription(
                        parsed = En1545Parser.parse(data, RavKavSubscription.FIELDS),
                        counter = ctr,
                    )
                },
                createTrip = { data ->
                    val transaction =
                        RavKavTransaction(
                            parsed = En1545Parser.parse(data, RavKavTransaction.FIELDS),
                        )
                    if (transaction.shouldBeDropped()) null else transaction
                },
            )

        return RavKavTransitInfo(result)
    }

    private fun ByteArray.byteArrayToLong(
        offset: Int,
        length: Int,
    ): Long {
        var result = 0L
        for (i in 0 until length) {
            result = (result shl 8) or (this[offset + i].toLong() and 0xFF)
        }
        return result
    }

    companion object {
        private val CARD_INFO =
            CardInfo(
                nameRes = Res.string.card_name_ravkav,
                cardType = CardType.ISO7816,
                region = TransitRegion.ISRAEL,
                locationRes = Res.string.card_location_israel,
                imageRes = Res.drawable.ravkav_card,
                latitude = 32.0853f,
                longitude = 34.7818f,
                brandColor = 0x99A400,
                credits = listOf("Metrodroid Project", "Vladimir Serbinenko"),
            )
    }
}
