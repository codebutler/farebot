/*
 * VeneziaTransitInfo.kt
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

package com.codebutler.farebot.transit.calypso.venezia

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.iso7816.ISO7816Application
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
import com.codebutler.farebot.transit.en1545.En1545TransitData
import farebot.farebot_transit_calypso.generated.resources.*

internal class VeneziaTransitInfo(
    result: CalypsoParseResult
) : CalypsoTransitInfo(result) {

    override val cardName: String = NAME

    private val profileNumber: Int
        get() = result.ticketEnv.getIntOrZero("HolderProfileNumber")

    @Suppress("unused")
    private val profileDescription: String
        get() = when (profileNumber) {
            117 -> "Normal"
            else -> "Unknown ($profileNumber)"
        }

    override val info: List<ListItemInterface>
        get() {
            val profileValue = when (profileNumber) {
                117 -> Res.string.calypso_profile_normal
                else -> null
            }
            return if (profileValue != null) {
                listOf(ListItem(Res.string.calypso_profile, profileValue))
            } else {
                val unknownProfile = getStringBlocking(Res.string.calypso_profile_unknown, profileNumber)
                listOf(ListItem(Res.string.calypso_profile, unknownProfile))
            }
        }

    companion object {
        const val NAME = "Venezia Unica"
    }
}

class VeneziaTransitFactory(stringResource: StringResource) : CalypsoTransitFactory(stringResource) {

    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override val name: String
        get() = VeneziaTransitInfo.NAME

    override fun checkTenv(tenv: ByteArray): Boolean {
        val v = ((tenv[0].toInt() and 0xFF) shl 24) or
                ((tenv[1].toInt() and 0xFF) shl 16) or
                ((tenv[2].toInt() and 0xFF) shl 8) or
                (tenv[3].toInt() and 0xFF)
        return v == 0x7d0
    }

    override fun parseTransitInfo(app: ISO7816Application, serial: String?): TransitInfo {
        val result = Calypso1545TransitData.parse(
            app = app,
            ticketEnvFields = TICKET_ENV_FIELDS,
            contractListFields = null,
            serial = serial,
            createSubscription = { data, ctr, _, _ -> VeneziaSubscription.parse(data, stringResource, ctr) },
            createTrip = { data -> VeneziaTransaction.parse(data) },
            createSpecialEvent = null
        )

        return VeneziaTransitInfo(result)
    }

    override fun getSerial(app: ISO7816Application): String? {
        val iccRecord = app.sfiFiles[0x02]?.records?.get(1) ?: return null
        if (iccRecord.size < 13) return null

        var serial = 0L
        for (i in 9..12) {
            serial = (serial shl 8) or (iccRecord[i].toLong() and 0xFF)
        }
        return serial.toString()
    }

    private val TICKET_ENV_FIELDS = En1545Container(
        En1545FixedHex(En1545TransitData.ENV_UNKNOWN_A, 49),
        En1545FixedInteger.datePacked(En1545TransitData.ENV_APPLICATION_VALIDITY_END),
        En1545FixedInteger("HolderProfileNumber", 8),
        En1545FixedHex(En1545TransitData.ENV_UNKNOWN_B, 2),
        En1545FixedInteger.datePacked(En1545TransitData.HOLDER_PROFILE)
    )

    companion object {
        private val CARD_INFO = CardInfo(
            nameRes = Res.string.card_name_venezia_unica,
            cardType = CardType.ISO7816,
            region = TransitRegion.ITALY,
            locationRes = Res.string.card_location_venice_italy,
            imageRes = Res.drawable.veneziaunica,
            latitude = 45.4408f,
            longitude = 12.3155f,
            brandColor = 0xC4F5F7,
            credits = listOf("Metrodroid Project", "Vladimir Serbinenko"),
        )
    }
}
