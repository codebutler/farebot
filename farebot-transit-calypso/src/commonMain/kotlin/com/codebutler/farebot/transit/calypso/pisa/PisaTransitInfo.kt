/*
 * PisaTransitInfo.kt
 *
 * Copyright 2018-2019 Google
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

package com.codebutler.farebot.transit.calypso.pisa

import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.iso7816.ISO7816Application
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.TransitRegion
import com.codebutler.farebot.transit.calypso.CalypsoTransitFactory
import com.codebutler.farebot.transit.calypso.CalypsoTransitInfo
import com.codebutler.farebot.transit.en1545.Calypso1545TransitData
import com.codebutler.farebot.transit.en1545.CalypsoConstants
import com.codebutler.farebot.transit.en1545.CalypsoParseResult
import com.codebutler.farebot.transit.en1545.En1545Container
import com.codebutler.farebot.transit.en1545.En1545FixedHex
import com.codebutler.farebot.transit.en1545.En1545FixedInteger
import com.codebutler.farebot.transit.en1545.En1545TransitData
import com.codebutler.farebot.transit.en1545.getBitsFromBuffer
import farebot.farebot_transit_calypso.generated.resources.*

internal class PisaTransitInfo(
    result: CalypsoParseResult
) : CalypsoTransitInfo(result) {

    override val cardName: String = NAME

    companion object {
        const val NAME = "Carta Mobile"
        const val PISA_NETWORK_ID = 0x380100
    }
}

class PisaTransitFactory(stringResource: StringResource) : CalypsoTransitFactory(stringResource) {

    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override val name: String
        get() = PisaTransitInfo.NAME

    override fun checkTenv(tenv: ByteArray): Boolean {
        val networkId = tenv.getBitsFromBuffer(5, 24)
        return networkId == PisaTransitInfo.PISA_NETWORK_ID
    }

    override fun parseTransitInfo(app: ISO7816Application, serial: String?): TransitInfo {
        val result = Calypso1545TransitData.parse(
            app = app,
            ticketEnvFields = TICKET_ENV_FIELDS,
            contractListFields = null,
            serial = serial,
            createSubscription = { data, ctr, _, _ -> PisaSubscription.parse(data, stringResource, ctr) },
            createTrip = { data -> PisaTransaction.parse(data) },
            createSpecialEvent = { data -> PisaSpecialEvent.parse(data) }
        )

        return PisaTransitInfo(result)
    }

    override fun getSerial(app: ISO7816Application): String? {
        return app.sfiFiles[CalypsoConstants.SFI_TICKETING_ENVIRONMENT]?.records?.get(2)?.let {
            it.decodeToString().trim { c -> c == '\u0000' || c.isWhitespace() }
        }
    }

    private val TICKET_ENV_FIELDS = En1545Container(
        En1545FixedInteger(En1545TransitData.ENV_VERSION_NUMBER, 5),
        En1545FixedInteger(En1545TransitData.ENV_NETWORK_ID, 24),
        En1545FixedHex(En1545TransitData.ENV_UNKNOWN_A, 44),
        En1545FixedInteger.date(En1545TransitData.ENV_APPLICATION_ISSUE),
        En1545FixedInteger.date(En1545TransitData.ENV_APPLICATION_VALIDITY_END),
        En1545FixedInteger.dateBCD(En1545TransitData.HOLDER_BIRTH_DATE)
    )

    companion object {
        private val CARD_INFO = CardInfo(
            nameRes = Res.string.card_name_carta_mobile,
            cardType = CardType.ISO7816,
            region = TransitRegion.ITALY,
            locationRes = Res.string.card_location_pisa_italy,
            imageRes = Res.drawable.cartamobile,
            latitude = 43.7228f,
            longitude = 10.4017f,
            brandColor = 0x0BADDB,
        )
    }
}
