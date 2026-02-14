/*
 * IntercodeFields.kt
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

package com.codebutler.farebot.transit.calypso

import com.codebutler.farebot.transit.en1545.En1545Bitmap
import com.codebutler.farebot.transit.en1545.En1545Container
import com.codebutler.farebot.transit.en1545.En1545Field
import com.codebutler.farebot.transit.en1545.En1545FixedInteger
import com.codebutler.farebot.transit.en1545.En1545FixedString
import com.codebutler.farebot.transit.en1545.En1545Repeat
import com.codebutler.farebot.transit.en1545.En1545Subscription
import com.codebutler.farebot.transit.en1545.En1545Transaction
import com.codebutler.farebot.transit.en1545.En1545TransitData

/**
 * Shared Intercode ticket environment, holder, transaction, and subscription
 * field definitions.
 *
 * These are used by Opus (Montreal), Adelaide metroCARD, and potentially
 * other Intercode-based systems.
 * Ported from Metrodroid's IntercodeTransitData, IntercodeTransaction,
 * IntercodeSubscription.
 */
object IntercodeFields {
    val TICKET_ENV_FIELDS =
        En1545Container(
            En1545FixedInteger(En1545TransitData.ENV_VERSION_NUMBER, 6),
            En1545Bitmap(
                En1545FixedInteger(En1545TransitData.ENV_NETWORK_ID, 24),
                En1545FixedInteger(En1545TransitData.ENV_APPLICATION_ISSUER_ID, 8),
                En1545FixedInteger.date(En1545TransitData.ENV_APPLICATION_VALIDITY_END),
                En1545FixedInteger("EnvPayMethod", 11),
                En1545FixedInteger(En1545TransitData.ENV_AUTHENTICATOR, 16),
                En1545FixedInteger("EnvSelectList", 32),
                En1545Container(
                    En1545FixedInteger("EnvCardStatus", 1),
                    En1545FixedInteger("EnvExtra", 0),
                ),
            ),
        )

    val HOLDER_FIELDS =
        En1545Container(
            En1545Bitmap(
                En1545Bitmap(
                    En1545FixedString("HolderSurname", 85),
                    En1545FixedString("HolderForename", 85),
                ),
                En1545Bitmap(
                    En1545FixedInteger.dateBCD(En1545TransitData.HOLDER_BIRTH_DATE),
                    En1545FixedString("HolderBirthPlace", 115),
                ),
                En1545FixedString("HolderBirthName", 85),
                En1545FixedInteger(En1545TransitData.HOLDER_ID_NUMBER, 32),
                En1545FixedInteger("HolderCountryAlpha", 24),
                En1545FixedInteger("HolderCompany", 32),
                En1545Repeat(
                    2,
                    En1545Bitmap(
                        En1545FixedInteger("HolderProfileNetworkId", 24),
                        En1545FixedInteger("HolderProfileNumber", 8),
                        En1545FixedInteger.date(En1545TransitData.HOLDER_PROFILE),
                    ),
                ),
                En1545Bitmap(
                    En1545FixedInteger(En1545TransitData.HOLDER_CARD_TYPE, 4),
                    En1545FixedInteger("HolderDataTeleReglement", 4),
                    En1545FixedInteger("HolderDataResidence", 17),
                    En1545FixedInteger("HolderDataCommercialID", 6),
                    En1545FixedInteger("HolderDataWorkPlace", 17),
                    En1545FixedInteger("HolderDataStudyPlace", 17),
                    En1545FixedInteger("HolderDataSaleDevice", 16),
                    En1545FixedInteger("HolderDataAuthenticator", 16),
                    En1545FixedInteger.date("HolderDataProfileStart1"),
                    En1545FixedInteger.date("HolderDataProfileStart2"),
                    En1545FixedInteger.date("HolderDataProfileStart3"),
                    En1545FixedInteger.date("HolderDataProfileStart4"),
                ),
            ),
        )

    val TICKET_ENV_HOLDER_FIELDS =
        En1545Container(
            TICKET_ENV_FIELDS,
            HOLDER_FIELDS,
        )

    // --- Intercode Transaction (Trip) Fields ---

    private fun tripFields(time: (String) -> En1545FixedInteger) =
        En1545Container(
            En1545FixedInteger.date(En1545Transaction.EVENT),
            time(En1545Transaction.EVENT),
            En1545Bitmap(
                En1545FixedInteger(En1545Transaction.EVENT_DISPLAY_DATA, 8),
                En1545FixedInteger(En1545Transaction.EVENT_NETWORK_ID, 24),
                En1545FixedInteger(En1545Transaction.EVENT_CODE, 8),
                En1545FixedInteger(En1545Transaction.EVENT_RESULT, 8),
                En1545FixedInteger(En1545Transaction.EVENT_SERVICE_PROVIDER, 8),
                En1545FixedInteger(En1545Transaction.EVENT_NOT_OK_COUNTER, 8),
                En1545FixedInteger(En1545Transaction.EVENT_SERIAL_NUMBER, 24),
                En1545FixedInteger(En1545Transaction.EVENT_DESTINATION, 16),
                En1545FixedInteger(En1545Transaction.EVENT_LOCATION_ID, 16),
                En1545FixedInteger(En1545Transaction.EVENT_LOCATION_GATE, 8),
                En1545FixedInteger(En1545Transaction.EVENT_DEVICE, 16),
                En1545FixedInteger(En1545Transaction.EVENT_ROUTE_NUMBER, 16),
                En1545FixedInteger(En1545Transaction.EVENT_ROUTE_VARIANT, 8),
                En1545FixedInteger(En1545Transaction.EVENT_JOURNEY_RUN, 16),
                En1545FixedInteger(En1545Transaction.EVENT_VEHICLE_ID, 16),
                En1545FixedInteger(En1545Transaction.EVENT_VEHICULE_CLASS, 8),
                En1545FixedInteger(En1545Transaction.EVENT_LOCATION_TYPE, 5),
                En1545FixedString(En1545Transaction.EVENT_EMPLOYEE, 240),
                En1545FixedInteger(En1545Transaction.EVENT_LOCATION_REFERENCE, 16),
                En1545FixedInteger(En1545Transaction.EVENT_JOURNEY_INTERCHANGES, 8),
                En1545FixedInteger(En1545Transaction.EVENT_PERIOD_JOURNEYS, 16),
                En1545FixedInteger(En1545Transaction.EVENT_TOTAL_JOURNEYS, 16),
                En1545FixedInteger(En1545Transaction.EVENT_JOURNEY_DISTANCE, 16),
                En1545FixedInteger(En1545Transaction.EVENT_PRICE_AMOUNT, 16),
                En1545FixedInteger(En1545Transaction.EVENT_PRICE_UNIT, 16),
                En1545FixedInteger(En1545Transaction.EVENT_CONTRACT_POINTER, 5),
                En1545FixedInteger(En1545Transaction.EVENT_AUTHENTICATOR, 16),
                En1545Bitmap(
                    En1545FixedInteger.date(En1545Transaction.EVENT_FIRST_STAMP),
                    time(En1545Transaction.EVENT_FIRST_STAMP),
                    En1545FixedInteger(En1545Transaction.EVENT_DATA_SIMULATION, 1),
                    En1545FixedInteger(En1545Transaction.EVENT_DATA_TRIP, 2),
                    En1545FixedInteger(En1545Transaction.EVENT_DATA_ROUTE_DIRECTION, 2),
                ),
            ),
        )

    val TRIP_FIELDS_LOCAL = tripFields(En1545FixedInteger.Companion::timeLocal)

    // --- Intercode Subscription (Contract) Fields ---

    private val SALE_CONTAINER =
        En1545Container(
            En1545FixedInteger.date(En1545Subscription.CONTRACT_SALE),
            En1545FixedInteger(En1545Subscription.CONTRACT_SALE_DEVICE, 16),
            En1545FixedInteger(En1545Subscription.CONTRACT_SALE_AGENT, 8),
        )

    private val PAY_CONTAINER =
        En1545Container(
            En1545FixedInteger(En1545Subscription.CONTRACT_PAY_METHOD, 11),
            En1545FixedInteger(En1545Subscription.CONTRACT_PRICE_AMOUNT, 16),
            En1545FixedInteger(En1545Subscription.CONTRACT_RECEIPT_DELIVERED, 1),
        )

    private val SOLD_CONTAINER =
        En1545Container(
            En1545FixedInteger(En1545Subscription.CONTRACT_SOLD, 8),
            En1545FixedInteger(En1545Subscription.CONTRACT_DEBIT_SOLD, 5),
        )

    private val PERIOD_CONTAINER =
        En1545Container(
            En1545FixedInteger("ContractEndPeriod", 14),
            En1545FixedInteger("ContractSoldPeriod", 6),
        )

    private val PASSENGER_COUNTER = En1545FixedInteger(En1545Subscription.CONTRACT_PASSENGER_TOTAL, 6)

    private val ZONE_MASK = En1545FixedInteger(En1545Subscription.CONTRACT_ZONES, 16)

    private val OVD1_CONTAINER =
        En1545Container(
            En1545FixedInteger(En1545Subscription.CONTRACT_ORIGIN_1, 16),
            En1545FixedInteger(En1545Subscription.CONTRACT_VIA_1, 16),
            En1545FixedInteger(En1545Subscription.CONTRACT_DESTINATION_1, 16),
        )

    private val OD2_CONTAINER =
        En1545Container(
            En1545FixedInteger(En1545Subscription.CONTRACT_ORIGIN_2, 16),
            En1545FixedInteger(En1545Subscription.CONTRACT_DESTINATION_2, 16),
        )

    private fun commonFormat(extra: En1545Field): En1545Field =
        En1545Bitmap(
            En1545FixedInteger(En1545Subscription.CONTRACT_PROVIDER, 8),
            En1545FixedInteger(En1545Subscription.CONTRACT_TARIFF, 16),
            En1545FixedInteger(En1545Subscription.CONTRACT_SERIAL_NUMBER, 32),
            En1545FixedInteger(En1545Subscription.CONTRACT_PASSENGER_CLASS, 8),
            En1545Bitmap(
                En1545FixedInteger.date(En1545Subscription.CONTRACT_START),
                En1545FixedInteger.date(En1545Subscription.CONTRACT_END),
            ),
            En1545FixedInteger(En1545Subscription.CONTRACT_STATUS, 8),
            extra,
        )

    val SUB_FIELDS_TYPE_FF: En1545Field =
        En1545Bitmap(
            En1545FixedInteger(En1545Subscription.CONTRACT_NETWORK_ID, 24),
            En1545FixedInteger(En1545Subscription.CONTRACT_PROVIDER, 8),
            En1545FixedInteger(En1545Subscription.CONTRACT_TARIFF, 16),
            En1545FixedInteger(En1545Subscription.CONTRACT_SERIAL_NUMBER, 32),
            En1545Bitmap(
                En1545FixedInteger("ContractCustomerProfile", 6),
                En1545FixedInteger("ContractCustomerNumber", 32),
            ),
            En1545Bitmap(
                En1545FixedInteger(En1545Subscription.CONTRACT_PASSENGER_CLASS, 8),
                En1545FixedInteger(En1545Subscription.CONTRACT_PASSENGER_TOTAL, 8),
            ),
            En1545FixedInteger(En1545Subscription.CONTRACT_VEHICULE_CLASS_ALLOWED, 6),
            En1545FixedInteger("ContractPaymentPointer", 32),
            En1545FixedInteger(En1545Subscription.CONTRACT_PAY_METHOD, 11),
            En1545FixedInteger("ContractServices", 16),
            En1545FixedInteger(En1545Subscription.CONTRACT_PRICE_AMOUNT, 16),
            En1545FixedInteger("ContractPriceUnit", 16),
            En1545Bitmap(
                En1545FixedInteger.timeLocal("ContractRestrictStart"),
                En1545FixedInteger.timeLocal("ContractRestrictEnd"),
                En1545FixedInteger("ContractRestrictDay", 8),
                En1545FixedInteger("ContractRestrictTimeCode", 8),
                En1545FixedInteger(En1545Subscription.CONTRACT_RESTRICT_CODE, 8),
                En1545FixedInteger("ContractRestrictProduct", 16),
                En1545FixedInteger("ContractRestrictLocation", 16),
            ),
            En1545Bitmap(
                En1545FixedInteger.date(En1545Subscription.CONTRACT_START),
                En1545FixedInteger.timeLocal(En1545Subscription.CONTRACT_START),
                En1545FixedInteger.date(En1545Subscription.CONTRACT_END),
                En1545FixedInteger.timeLocal(En1545Subscription.CONTRACT_END),
                En1545FixedInteger(En1545Subscription.CONTRACT_DURATION, 8),
                En1545FixedInteger.date("ContractLimit"),
                En1545FixedInteger(En1545Subscription.CONTRACT_ZONES, 8),
                En1545FixedInteger(En1545Subscription.CONTRACT_JOURNEYS, 16),
                En1545FixedInteger("ContractPeriodJourneys", 16),
            ),
            En1545Bitmap(
                En1545FixedInteger(En1545Subscription.CONTRACT_ORIGIN_1, 16),
                En1545FixedInteger(En1545Subscription.CONTRACT_DESTINATION_1, 16),
                En1545FixedInteger("ContractRouteNumbers", 16),
                En1545FixedInteger("ContractRouteVariants", 8),
                En1545FixedInteger("ContractRun", 16),
                En1545FixedInteger(En1545Subscription.CONTRACT_VIA_1, 16),
                En1545FixedInteger("ContractDistance", 16),
                En1545FixedInteger(En1545Subscription.CONTRACT_INTERCHANGE, 8),
            ),
            En1545Bitmap(
                En1545FixedInteger.date(En1545Subscription.CONTRACT_SALE),
                En1545FixedInteger.timeLocal(En1545Subscription.CONTRACT_SALE),
                En1545FixedInteger(En1545Subscription.CONTRACT_SALE_AGENT, 8),
                En1545FixedInteger(En1545Subscription.CONTRACT_SALE_DEVICE, 16),
            ),
            En1545FixedInteger(En1545Subscription.CONTRACT_STATUS, 8),
            En1545FixedInteger("ContractLoyaltyPoints", 16),
            En1545FixedInteger(En1545Subscription.CONTRACT_AUTHENTICATOR, 16),
            En1545FixedInteger("ContractExtra", 0),
        )

    val SUB_FIELDS_TYPE_20: En1545Field =
        commonFormat(
            En1545Bitmap(
                OVD1_CONTAINER,
                OD2_CONTAINER,
                ZONE_MASK,
                SALE_CONTAINER,
                PAY_CONTAINER,
                PASSENGER_COUNTER,
                PERIOD_CONTAINER,
                SOLD_CONTAINER,
                En1545FixedInteger(En1545Subscription.CONTRACT_VEHICULE_CLASS_ALLOWED, 4),
                En1545FixedInteger(En1545Subscription.LINKED_CONTRACT, 5),
            ),
        )

    val SUB_FIELDS_TYPE_46: En1545Field =
        commonFormat(
            En1545Bitmap(
                OVD1_CONTAINER,
                OD2_CONTAINER,
                ZONE_MASK,
                SALE_CONTAINER,
                PAY_CONTAINER,
                PASSENGER_COUNTER,
                PERIOD_CONTAINER,
                SOLD_CONTAINER,
                En1545FixedInteger(En1545Subscription.CONTRACT_VEHICULE_CLASS_ALLOWED, 4),
                En1545FixedInteger(En1545Subscription.LINKED_CONTRACT, 5),
                En1545FixedInteger.timeLocal(En1545Subscription.CONTRACT_START),
                En1545FixedInteger.timeLocal(En1545Subscription.CONTRACT_END),
                En1545FixedInteger.date("ContractDataEndInhibition"),
                En1545FixedInteger.date("ContractDataValidityLimit"),
                En1545FixedInteger("ContractDataGeoLine", 28),
                En1545FixedInteger(En1545Subscription.CONTRACT_JOURNEYS, 16),
                En1545FixedInteger("ContractDataSaleSecureDevice", 32),
            ),
        )

    val SUB_FIELDS_TYPE_OTHER: En1545Field = commonFormat(En1545FixedInteger("ContractData", 0))

    fun getSubscriptionFields(type: Int): En1545Field {
        if (type == 0xff) {
            return SUB_FIELDS_TYPE_FF
        }
        if (type == 0x20) {
            return SUB_FIELDS_TYPE_20
        }
        return if (type == 0x46) SUB_FIELDS_TYPE_46 else SUB_FIELDS_TYPE_OTHER
    }

    // --- Intercode Contract List Fields ---

    val CONTRACT_LIST_FIELDS =
        En1545Repeat(
            4,
            En1545Bitmap(
                En1545FixedInteger(En1545TransitData.CONTRACTS_NETWORK_ID, 24),
                En1545FixedInteger(En1545TransitData.CONTRACTS_TARIFF, 16),
                En1545FixedInteger(En1545TransitData.CONTRACTS_POINTER, 5),
            ),
        )
}
