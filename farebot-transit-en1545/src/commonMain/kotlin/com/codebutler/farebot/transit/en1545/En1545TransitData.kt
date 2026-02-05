/*
 * En1545TransitData.kt
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

package com.codebutler.farebot.transit.en1545

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.DateFormatStyle
import com.codebutler.farebot.base.util.formatDate
import farebot.farebot_transit_en1545.generated.resources.Res
import farebot.farebot_transit_en1545.generated.resources.en1545_card_issuer
import farebot.farebot_transit_en1545.generated.resources.en1545_card_type
import farebot.farebot_transit_en1545.generated.resources.en1545_card_type_anonymous
import farebot.farebot_transit_en1545.generated.resources.en1545_card_type_declarative
import farebot.farebot_transit_en1545.generated.resources.en1545_card_type_personal
import farebot.farebot_transit_en1545.generated.resources.en1545_card_type_provider_specific
import farebot.farebot_transit_en1545.generated.resources.en1545_date_of_birth
import farebot.farebot_transit_en1545.generated.resources.en1545_expiry_date
import farebot.farebot_transit_en1545.generated.resources.en1545_issue_date
import farebot.farebot_transit_en1545.generated.resources.en1545_network_id
import farebot.farebot_transit_en1545.generated.resources.en1545_card_expiry_date_profile
import farebot.farebot_transit_en1545.generated.resources.en1545_postal_code

/**
 * Base class providing EN1545 environment field name constants and parsed ticket environment.
 */
abstract class En1545TransitData(
    protected val mTicketEnvParsed: En1545Parsed
) {
    protected abstract val lookup: En1545Lookup

    val networkId: Int
        get() = mTicketEnvParsed.getIntOrZero(ENV_NETWORK_ID)

    /**
     * Returns info list items for display on the Info tab.
     * Includes network ID, expiry date, birth date, issuer, issue date, profile date, postal code, card type.
     */
    open val en1545Info: List<ListItemInterface>
        get() {
            val li = mutableListOf<ListItem>()
            val tz = lookup.timeZone

            if (mTicketEnvParsed.contains(ENV_NETWORK_ID)) {
                li.add(ListItem(
                    Res.string.en1545_network_id,
                    mTicketEnvParsed.getIntOrZero(ENV_NETWORK_ID).toString(16)
                ))
            }

            mTicketEnvParsed.getTimeStamp(ENV_APPLICATION_VALIDITY_END, tz)?.let {
                li.add(ListItem(
                    Res.string.en1545_expiry_date,
                    formatDate(it, DateFormatStyle.LONG)
                ))
            }

            // Birth date - skipped if privacy settings would hide it (not implemented in FareBot)
            mTicketEnvParsed.getTimeStamp(HOLDER_BIRTH_DATE, tz)?.let {
                li.add(ListItem(
                    Res.string.en1545_date_of_birth,
                    formatDate(it, DateFormatStyle.LONG)
                ))
            }

            if (mTicketEnvParsed.getIntOrZero(ENV_APPLICATION_ISSUER_ID) != 0) {
                li.add(ListItem(
                    Res.string.en1545_card_issuer,
                    lookup.getAgencyName(mTicketEnvParsed.getIntOrZero(ENV_APPLICATION_ISSUER_ID), false)
                ))
            }

            mTicketEnvParsed.getTimeStamp(ENV_APPLICATION_ISSUE, tz)?.let {
                li.add(ListItem(
                    Res.string.en1545_issue_date,
                    formatDate(it, DateFormatStyle.LONG)
                ))
            }

            mTicketEnvParsed.getTimeStamp(HOLDER_PROFILE, tz)?.let {
                li.add(ListItem(
                    Res.string.en1545_card_expiry_date_profile,
                    formatDate(it, DateFormatStyle.LONG)
                ))
            }

            // Postal code - skipped if privacy settings would hide it (not implemented in FareBot)
            // Only Mobib sets this, and Belgium has numeric postal codes.
            mTicketEnvParsed.getInt(HOLDER_INT_POSTAL_CODE)?.let {
                if (it != 0) {
                    li.add(ListItem(
                        Res.string.en1545_postal_code,
                        it.toString()
                    ))
                }
            }

            mTicketEnvParsed.getInt(HOLDER_CARD_TYPE)?.let { cardType ->
                val cardTypeRes = when (cardType) {
                    0 -> Res.string.en1545_card_type_anonymous
                    1 -> Res.string.en1545_card_type_declarative
                    2 -> Res.string.en1545_card_type_personal
                    else -> Res.string.en1545_card_type_provider_specific
                }
                li.add(ListItem(
                    Res.string.en1545_card_type,
                    cardTypeRes
                ))
            }

            return li
        }

    companion object {
        const val ENV_NETWORK_ID = "EnvNetworkId"
        const val ENV_VERSION_NUMBER = "EnvVersionNumber"
        const val HOLDER_BIRTH_DATE = "HolderBirth"
        const val ENV_APPLICATION_VALIDITY_END = "EnvApplicationValidityEnd"
        const val ENV_APPLICATION_ISSUER_ID = "EnvApplicationIssuerId"
        const val ENV_APPLICATION_ISSUE = "EnvApplicationIssue"
        const val HOLDER_PROFILE = "HolderProfile"
        const val HOLDER_INT_POSTAL_CODE = "HolderIntPostalCode"
        const val HOLDER_CARD_TYPE = "HolderDataCardStatus"
        const val ENV_AUTHENTICATOR = "EnvAuthenticator"
        const val ENV_UNKNOWN_A = "EnvUnknownA"
        const val ENV_UNKNOWN_B = "EnvUnknownB"
        const val ENV_UNKNOWN_C = "EnvUnknownC"
        const val ENV_UNKNOWN_D = "EnvUnknownD"
        const val ENV_UNKNOWN_E = "EnvUnknownE"
        const val ENV_CARD_SERIAL = "EnvCardSerial"
        const val HOLDER_ID_NUMBER = "HolderIdNumber"
        const val HOLDER_UNKNOWN_A = "HolderUnknownA"
        const val HOLDER_UNKNOWN_B = "HolderUnknownB"
        const val HOLDER_UNKNOWN_C = "HolderUnknownC"
        const val HOLDER_UNKNOWN_D = "HolderUnknownD"
        const val CONTRACTS_PROVIDER = "ContractsProvider"
        const val CONTRACTS_POINTER = "ContractsPointer"
        const val CONTRACTS_TARIFF = "ContractsTariff"
        const val CONTRACTS_UNKNOWN_A = "ContractsUnknownA"
        const val CONTRACTS_UNKNOWN_B = "ContractsUnknownB"
        const val CONTRACTS_NETWORK_ID = "ContractsNetworkId"
    }
}
