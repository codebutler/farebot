/*
 * En1545Subscription.kt
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
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import farebot.farebot_transit_en1545.generated.resources.Res
import farebot.farebot_transit_en1545.generated.resources.en1545_passenger_class
import farebot.farebot_transit_en1545.generated.resources.en1545_valid_origin_destination
import farebot.farebot_transit_en1545.generated.resources.en1545_valid_origin_destination_via
import farebot.farebot_transit_en1545.generated.resources.en1545_with_receipt
import farebot.farebot_transit_en1545.generated.resources.en1545_without_receipt
import kotlin.time.Instant

abstract class En1545Subscription : Subscription() {
    protected abstract val parsed: En1545Parsed
    protected abstract val lookup: En1545Lookup
    protected abstract val stringResource: StringResource

    protected val contractTariff: Int?
        get() = parsed.getInt(CONTRACT_TARIFF)

    protected val contractProvider: Int?
        get() = parsed.getInt(CONTRACT_PROVIDER)

    override val zones: IntArray?
        get() {
            val zonecode = parsed.getInt(CONTRACT_ZONES) ?: return null

            val zones = mutableListOf<Int>()
            var zone = 0
            while (zonecode shr zone > 0) {
                if (zonecode and (1 shl zone) != 0) {
                    zones.add(zone + 1)
                }
                zone++
            }

            return zones.toIntArray()
        }

    override val paymentMethod: PaymentMethod
        get() {
            if (cost == null) {
                return super.paymentMethod
            }

            return when (parsed.getIntOrZero(CONTRACT_PAY_METHOD)) {
                0x90 -> PaymentMethod.CASH
                0xb3 -> PaymentMethod.CREDIT_CARD
                0 -> PaymentMethod.UNKNOWN
                else -> PaymentMethod.UNKNOWN
            }
        }

    override val subscriptionState: SubscriptionState
        get() {
            val status = parsed.getInt(CONTRACT_STATUS) ?: return super.subscriptionState

            return when (status) {
                0 -> SubscriptionState.UNUSED
                1 -> SubscriptionState.STARTED
                0xFF -> SubscriptionState.EXPIRED
                else -> SubscriptionState.UNKNOWN
            }
        }

    override val saleAgencyName: String?
        get() {
            val agency = parsed.getInt(CONTRACT_SALE_AGENT) ?: return null
            return lookup.getAgencyName(agency, false)
        }

    override val passengerCount: Int
        get() = parsed.getInt(CONTRACT_PASSENGER_TOTAL) ?: super.passengerCount

    open val balance: TransitBalance?
        get() = null

    override val cost: TransitCurrency?
        get() {
            val amount = parsed.getIntOrZero(CONTRACT_PRICE_AMOUNT)
            return if (amount == 0) null else lookup.parseCurrency(amount)
        }

    override val purchaseTimestamp: Instant?
        get() = parsed.getTimeStamp(CONTRACT_SALE, lookup.timeZone)

    override val lastUseTimestamp: Instant?
        get() = parsed.getTimeStamp(CONTRACT_LAST_USE, lookup.timeZone)

    override val id: Int? get() = parsed.getInt(CONTRACT_SERIAL_NUMBER)

    override val validFrom: Instant?
        get() = parsed.getTimeStamp(CONTRACT_START, lookup.timeZone)

    override val validTo: Instant?
        get() = parsed.getTimeStamp(CONTRACT_END, lookup.timeZone)

    override val agencyName: String?
        get() = lookup.getAgencyName(contractProvider, false)

    override val shortAgencyName: String?
        get() = lookup.getAgencyName(contractProvider, true)

    override val machineId: Int?
        get() = parsed.getInt(CONTRACT_SALE_DEVICE)?.let { if (it == 0) null else it }

    override val subscriptionName: String?
        get() = lookup.getSubscriptionName(stringResource, contractProvider, contractTariff)

    override val info: List<ListItemInterface>?
        get() {
            val li = mutableListOf<ListItem>()
            val clas = parsed.getInt(CONTRACT_PASSENGER_CLASS)
            if (clas != null) {
                li.add(ListItem(Res.string.en1545_passenger_class, clas.toString()))
            }
            val receipt = parsed.getInt(CONTRACT_RECEIPT_DELIVERED)
            if (receipt != null && receipt != 0) {
                li.add(ListItem(getStringBlocking(Res.string.en1545_with_receipt)))
            }
            if (receipt != null && receipt == 0) {
                li.add(ListItem(getStringBlocking(Res.string.en1545_without_receipt)))
            }
            if (parsed.contains(CONTRACT_ORIGIN_1) || parsed.contains(CONTRACT_DESTINATION_1)) {
                if (parsed.contains(CONTRACT_VIA_1)) {
                    li.add(
                        ListItem(
                            getStringBlocking(Res.string.en1545_valid_origin_destination_via, getStationName(CONTRACT_ORIGIN_1) ?: "?", getStationName(CONTRACT_DESTINATION_1) ?: "?", getStationName(CONTRACT_VIA_1) ?: "?")
                        )
                    )
                } else {
                    li.add(
                        ListItem(
                            getStringBlocking(Res.string.en1545_valid_origin_destination, getStationName(CONTRACT_ORIGIN_1) ?: "?", getStationName(CONTRACT_DESTINATION_1) ?: "?")
                        )
                    )
                }
            }
            if (parsed.contains(CONTRACT_ORIGIN_2) || parsed.contains(CONTRACT_DESTINATION_2)) {
                li.add(
                    ListItem(
                        getStringBlocking(Res.string.en1545_valid_origin_destination, getStationName(CONTRACT_ORIGIN_2) ?: "?", getStationName(CONTRACT_DESTINATION_2) ?: "?")
                    )
                )
            }
            return super.info.orEmpty() + li
        }

    private fun getStationName(prop: String): String? {
        return lookup.getStation(parsed.getInt(prop) ?: return null, contractProvider, null)?.stationName
    }

    companion object {
        const val CONTRACT_ZONES = "ContractZones"
        const val CONTRACT_SALE = "ContractSale"
        const val CONTRACT_PRICE_AMOUNT = "ContractPriceAmount"
        const val CONTRACT_PAY_METHOD = "ContractPayMethod"
        const val CONTRACT_LAST_USE = "ContractLastUse"
        const val CONTRACT_STATUS = "ContractStatus"
        const val CONTRACT_SALE_AGENT = "ContractSaleAgent"
        const val CONTRACT_PASSENGER_TOTAL = "ContractPassengerTotal"
        const val CONTRACT_START = "ContractStart"
        const val CONTRACT_END = "ContractEnd"
        const val CONTRACT_PROVIDER = "ContractProvider"
        const val CONTRACT_TARIFF = "ContractTariff"
        const val CONTRACT_SALE_DEVICE = "ContractSaleDevice"
        const val CONTRACT_SERIAL_NUMBER = "ContractSerialNumber"
        const val CONTRACT_UNKNOWN_A = "ContractUnknownA"
        const val CONTRACT_UNKNOWN_B = "ContractUnknownB"
        const val CONTRACT_UNKNOWN_C = "ContractUnknownC"
        const val CONTRACT_UNKNOWN_D = "ContractUnknownD"
        const val CONTRACT_UNKNOWN_E = "ContractUnknownE"
        const val CONTRACT_UNKNOWN_F = "ContractUnknownF"
        const val CONTRACT_NETWORK_ID = "ContractNetworkId"
        const val CONTRACT_PASSENGER_CLASS = "ContractPassengerClass"
        const val CONTRACT_AUTHENTICATOR = "ContractAuthenticator"
        const val CONTRACT_SOLD = "ContractSold"
        const val CONTRACT_DEBIT_SOLD = "ContractDebitSold"
        const val CONTRACT_JOURNEYS = "ContractJourneys"
        const val CONTRACT_RECEIPT_DELIVERED = "ContractReceiptDelivered"
        const val CONTRACT_ORIGIN_1 = "ContractOrigin1"
        const val CONTRACT_VIA_1 = "ContractVia1"
        const val CONTRACT_DESTINATION_1 = "ContractDestination1"
        const val CONTRACT_ORIGIN_2 = "ContractOrigin2"
        const val CONTRACT_DESTINATION_2 = "ContractDestination2"
        const val CONTRACT_VEHICULE_CLASS_ALLOWED = "ContractVehiculeClassAllowed"
        const val CONTRACT_DURATION = "ContractDuration"
        const val CONTRACT_INTERCHANGE = "ContractInterchange"
        const val LINKED_CONTRACT = "LinkedContract"
        const val CONTRACT_RESTRICT_CODE = "ContractRestrictCode"
    }
}
