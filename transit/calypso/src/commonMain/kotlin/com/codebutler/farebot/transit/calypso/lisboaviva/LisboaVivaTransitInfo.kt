/*
 * LisboaVivaTransitInfo.kt
 *
 * Copyright 2018 Google
 * Copyright 2025 Eric Butler
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

package com.codebutler.farebot.transit.calypso.lisboaviva

import com.codebutler.farebot.base.ui.FareBotUiTree
import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.base.util.byteArrayToLong
import com.codebutler.farebot.base.util.readLatin1
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
import farebot.transit.calypso.generated.resources.*

// Reference: https://github.com/L1L1/cardpeek/blob/master/dot_cardpeek_dir/scripts/calypso/c131.lua
class LisboaVivaTransitInfo internal constructor(
    result: CalypsoParseResult,
    private val holderName: String?,
    private val tagId: Long?,
) : CalypsoTransitInfo(result) {
    override val cardName: String = NAME

    override val info: List<ListItemInterface>
        get() {
            if (holderName.isNullOrEmpty()) return emptyList()
            return listOf(ListItem(Res.string.calypso_holder_name, holderName))
        }

    override fun getAdvancedUi(stringResource: StringResource): FareBotUiTree? {
        if (tagId == null) return null
        val b = FareBotUiTree.builder(stringResource)
        b.item().title(Res.string.calypso_engraved_serial).value(tagId.toString())
        return b.build()
    }

    companion object {
        const val NAME = "Viva"
    }

    class Factory(
        stringResource: StringResource,
    ) : CalypsoTransitFactory(stringResource) {
        override val allCards: List<CardInfo>
            get() = listOf(CARD_INFO)

        override val name: String = NAME

        override fun checkTenv(tenv: ByteArray): Boolean {
            val countryCode = tenv.getBitsFromBuffer(13, 12)
            return countryCode == COUNTRY_CODE_PORTUGAL
        }

        override fun parseTransitInfo(
            app: ISO7816Application,
            serial: String?,
        ): TransitInfo {
            val result =
                Calypso1545TransitData.parse(
                    app = app,
                    ticketEnvFields = TICKET_ENV_FIELDS,
                    contractListFields = null,
                    serial = serial,
                    createSubscription = {
                        data,
                        counter,
                        _,
                        _,
                        ->
                        LisboaVivaSubscription.parse(data, stringResource, counter)
                    },
                    createTrip = { data -> LisboaVivaTransaction.parse(data) },
                )

            // Parse tag ID from ICC file (SFI 0x02)
            val tagId =
                Calypso1545TransitData
                    .getSfiFile(app, 0x02)
                    ?.records
                    ?.get(1)
                    ?.let { record ->
                        if (record.size >= 20) record.byteArrayToLong(16, 4) else null
                    }

            // Parse holder name from ID file (SFI 0x03)
            val holderName =
                Calypso1545TransitData
                    .getSfiFile(app, 0x03)
                    ?.records
                    ?.get(1)
                    ?.readLatin1()
                    ?.trim()

            return LisboaVivaTransitInfo(result, holderName, tagId)
        }

        override fun getSerial(app: ISO7816Application): String? {
            val tenvRecord =
                app.sfiFiles[CalypsoConstants.SFI_TICKETING_ENVIRONMENT]
                    ?.records
                    ?.get(1) ?: return null
            return NumberUtils.zeroPad(tenvRecord.getBitsFromBuffer(30, 8), 3) + " " +
                NumberUtils.zeroPad(tenvRecord.getBitsFromBuffer(38, 24), 9)
        }

        companion object {
            private val CARD_INFO =
                CardInfo(
                    nameRes = Res.string.card_name_lisboa_viva,
                    cardType = CardType.ISO7816,
                    region = TransitRegion.PORTUGAL,
                    locationRes = Res.string.card_location_lisbon_portugal,
                    imageRes = Res.drawable.lisboaviva,
                    latitude = 38.7223f,
                    longitude = -9.1393f,
                    brandColor = 0x46552C,
                    credits = listOf("Metrodroid Project", "Vladimir Serbinenko"),
                )

            private const val COUNTRY_CODE_PORTUGAL = 0x131
            private const val ENV_UNKNOWN_A = "EnvUnknownA"
            private const val ENV_UNKNOWN_B = "EnvUnknownB"
            private const val ENV_UNKNOWN_C = "EnvUnknownC"
            private const val ENV_UNKNOWN_D = "EnvUnknownD"
            private const val ENV_NETWORK_COUNTRY = "EnvNetworkCountry"
            private const val CARD_SERIAL_PREFIX = "CardSerialPrefix"

            private val TICKET_ENV_FIELDS =
                En1545Container(
                    En1545FixedInteger(ENV_UNKNOWN_A, 13),
                    En1545FixedInteger(ENV_NETWORK_COUNTRY, 12),
                    En1545FixedInteger(ENV_UNKNOWN_B, 5),
                    En1545FixedInteger(CARD_SERIAL_PREFIX, 8),
                    En1545FixedInteger(En1545TransitData.ENV_CARD_SERIAL, 24),
                    En1545FixedInteger.date(En1545TransitData.ENV_APPLICATION_ISSUE),
                    En1545FixedInteger.date(En1545TransitData.ENV_APPLICATION_VALIDITY_END),
                    En1545FixedInteger(ENV_UNKNOWN_C, 15),
                    En1545FixedInteger.dateBCD(En1545TransitData.HOLDER_BIRTH_DATE),
                    En1545FixedHex(ENV_UNKNOWN_D, 95),
                )
        }
    }
}
