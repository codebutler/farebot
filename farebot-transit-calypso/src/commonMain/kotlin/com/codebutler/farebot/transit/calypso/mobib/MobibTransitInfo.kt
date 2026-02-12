/*
 * MobibTransitInfo.kt
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

package com.codebutler.farebot.transit.calypso.mobib

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.DateFormatStyle
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.formatDate
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.iso7816.ISO7816Application
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransactionTrip
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.TransitRegion
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.calypso.CalypsoTransitFactory
import com.codebutler.farebot.transit.en1545.Calypso1545TransitData
import com.codebutler.farebot.transit.en1545.CalypsoConstants
import com.codebutler.farebot.transit.en1545.En1545Container
import com.codebutler.farebot.transit.en1545.En1545FixedHex
import com.codebutler.farebot.transit.en1545.En1545FixedInteger
import com.codebutler.farebot.transit.en1545.En1545FixedString
import com.codebutler.farebot.transit.en1545.En1545Parsed
import com.codebutler.farebot.transit.en1545.En1545Parser
import com.codebutler.farebot.transit.en1545.En1545Subscription
import com.codebutler.farebot.transit.en1545.En1545TransitData
import com.codebutler.farebot.transit.en1545.getBitsFromBuffer
import farebot.farebot_transit_calypso.generated.resources.*
import kotlinx.datetime.TimeZone

/*
 * Reference:
 * - https://github.com/zoobab/mobib-extractor
 */
class MobibTransitInfo internal constructor(
    override val serialNumber: String?,
    override val trips: List<Trip>,
    override val subscriptions: List<Subscription>?,
    override val balances: List<TransitBalance>?,
    private val extHolderParsed: En1545Parsed?,
    private val purchase: Int,
    private val totalTrips: Int,
) : TransitInfo() {

    override val cardName: String = NAME

    override val info: List<ListItemInterface>
        get() {
            val li = mutableListOf<ListItemInterface>()
            val purchaseDate = En1545FixedInteger.parseDate(purchase, TZ)
            if (purchaseDate != null) {
                li.add(ListItem(Res.string.calypso_purchase_date, formatDate(purchaseDate, DateFormatStyle.LONG)))
            }
            li.add(ListItem(Res.string.calypso_transaction_counter, totalTrips.toString()))
            if (extHolderParsed != null) {
                val gender = extHolderParsed.getIntOrZero(EXT_HOLDER_GENDER)
                if (gender == 0) {
                    li.add(ListItem(Res.string.calypso_card_type, Res.string.calypso_card_type_anonymous))
                } else {
                    li.add(ListItem(Res.string.calypso_card_type, Res.string.calypso_card_type_personal))
                    val name = extHolderParsed.getString(EXT_HOLDER_NAME)
                    if (name != null) {
                        li.add(ListItem(Res.string.calypso_holder_name, name))
                    }
                    when (gender) {
                        1 -> li.add(ListItem(Res.string.calypso_gender, Res.string.calypso_gender_male))
                        2 -> li.add(ListItem(Res.string.calypso_gender, Res.string.calypso_gender_female))
                        else -> li.add(ListItem(Res.string.calypso_gender, gender.toString(16)))
                    }
                }
            }
            return li
        }

    companion object {
        const val NAME = "Mobib"
        private const val NETWORK_ID = 0x56001
        private const val EXT_HOLDER_NAME = "ExtHolderName"
        private const val EXT_HOLDER_GENDER = "ExtHolderGender"
        private const val EXT_HOLDER_DATE_OF_BIRTH = "ExtHolderDateOfBirth"
        private const val EXT_HOLDER_CARD_SERIAL = "ExtHolderCardSerial"
        private const val EXT_HOLDER_UNKNOWN_A = "ExtHolderUnknownA"
        private const val EXT_HOLDER_UNKNOWN_B = "ExtHolderUnknownB"
        private const val EXT_HOLDER_UNKNOWN_C = "ExtHolderUnknownC"
        private const val EXT_HOLDER_UNKNOWN_D = "ExtHolderUnknownD"
        val TZ = TimeZone.of("Europe/Brussels")
    }

    class Factory(stringResource: StringResource) : CalypsoTransitFactory(stringResource) {

        override val allCards: List<CardInfo>
            get() = listOf(CARD_INFO)

        override val name: String = NAME

        override fun checkTenv(tenv: ByteArray): Boolean {
            val networkId = tenv.getBitsFromBuffer(13, 24)
            return networkId == NETWORK_ID
        }

        override fun getSerial(app: ISO7816Application): String? {
            val holder = app.sfiFiles[CalypsoConstants.SFI_TICKETING_ENVIRONMENT]
                ?.records?.get(1) ?: return null
            return try {
                NumberUtils.zeroPad(NumberUtils.convertBCDtoInteger(holder.getBitsFromBuffer(18 + 80, 24)), 6) + " / " +
                    NumberUtils.zeroPad(NumberUtils.convertBCDtoInteger(holder.getBitsFromBuffer(42 + 80, 24)), 6) +
                    NumberUtils.zeroPad(NumberUtils.convertBCDtoInteger(holder.getBitsFromBuffer(66 + 80, 16)), 4) +
                    NumberUtils.zeroPad(NumberUtils.convertBCDtoInteger(holder.getBitsFromBuffer(82 + 80, 8)), 2) + " / " +
                    NumberUtils.convertBCDtoInteger(holder.getBitsFromBuffer(90 + 80, 4)).toString()
            } catch (_: Exception) {
                null
            }
        }

        override fun parseTransitInfo(
            app: ISO7816Application,
            serial: String?
        ): TransitInfo {
            // Parse ticket env with version-dependent fields
            val rawTicketEnvRecords = Calypso1545TransitData.getSfiRecords(
                app, CalypsoConstants.SFI_TICKETING_ENVIRONMENT
            )
            val rawTicketEnv = rawTicketEnvRecords.fold(byteArrayOf()) { acc, bytes -> acc + bytes }
            val version = if (rawTicketEnv.isNotEmpty()) rawTicketEnv.getBitsFromBuffer(0, 6) else 0
            val ticketEnv = if (rawTicketEnv.isEmpty()) En1545Parsed()
            else En1545Parser.parse(rawTicketEnv, ticketEnvFields(version))

            // Parse contracts (first 7 only)
            val allContracts = Calypso1545TransitData.getSfiRecords(
                app, CalypsoConstants.SFI_TICKETING_CONTRACTS_1
            )
            val contracts = if (allContracts.size > 7) allContracts.subList(0, 7) else allContracts
            val subscriptions = mutableListOf<En1545Subscription>()
            val balances = mutableListOf<TransitCurrency>()

            for ((idx, record) in contracts.withIndex()) {
                val sub = MobibSubscription.parse(record, stringResource, Calypso1545TransitData.getCounter(app, idx + 1)) ?: continue
                val bal = sub.cost
                if (bal != null) {
                    balances.add(bal)
                } else {
                    subscriptions.add(sub)
                }
            }

            // Parse trips - try main log first, then fallback SFI 0x17
            val ticketLogRecords = Calypso1545TransitData.getSfiRecords(
                app, CalypsoConstants.SFI_TICKETING_LOG
            ).ifEmpty {
                Calypso1545TransitData.getSfiRecords(app, 0x17)
            }
            val transactions = ticketLogRecords.mapNotNull { MobibTransaction.parse(it) }
            val trips = TransactionTrip.merge(transactions)
            val totalTrips = transactions.maxOfOrNull { it.transactionNumber } ?: 0

            // Parse extended holder (SFI 0x1E = HOLDER_EXTENDED)
            val holderFile = Calypso1545TransitData.getSfiFile(app, 0x1E)
            val extHolderParsed = if (holderFile != null) {
                val holder = (holderFile.records[1] ?: ByteArray(0)) +
                    (holderFile.records[2] ?: ByteArray(0))
                if (holder.isNotEmpty()) En1545Parser.parse(holder, extHolderFields)
                else null
            } else {
                null
            }

            // Parse purchase date from EP_LOAD_LOG (SFI 0x14)
            val epLoadLog = Calypso1545TransitData.getSfiFile(app, 0x14)
            val purchase = epLoadLog?.records?.get(1)?.let {
                try { it.getBitsFromBuffer(2, 14) } catch (_: Exception) { 0 }
            } ?: 0

            return MobibTransitInfo(
                serialNumber = serial,
                trips = trips,
                subscriptions = subscriptions.ifEmpty { null },
                balances = if (balances.isNotEmpty()) balances.map { TransitBalance(balance = it) } else null,
                extHolderParsed = extHolderParsed,
                purchase = purchase,
                totalTrips = totalTrips,
            )
        }

        companion object {
            private val CARD_INFO = CardInfo(
                nameRes = Res.string.card_name_mobib,
                cardType = CardType.ISO7816,
                region = TransitRegion.BELGIUM,
                locationRes = Res.string.card_location_brussels_belgium,
                imageRes = Res.drawable.mobib_card,
                latitude = 50.8503f,
                longitude = 4.3517f,
                sampleDumpFile = "Mobib.json",
                brandColor = 0x9CBC17,
            )

            private fun ticketEnvFields(version: Int) = when {
                version <= 2 -> En1545Container(
                    En1545FixedInteger(En1545TransitData.ENV_VERSION_NUMBER, 6),
                    En1545FixedInteger(En1545TransitData.ENV_UNKNOWN_A, 7),
                    En1545FixedInteger(En1545TransitData.ENV_NETWORK_ID, 24),
                    En1545FixedInteger(En1545TransitData.ENV_UNKNOWN_B, 9),
                    En1545FixedInteger.date(En1545TransitData.ENV_APPLICATION_VALIDITY_END),
                    En1545FixedInteger(En1545TransitData.ENV_UNKNOWN_C, 6),
                    En1545FixedInteger.dateBCD(En1545TransitData.HOLDER_BIRTH_DATE),
                    En1545FixedHex(En1545TransitData.ENV_CARD_SERIAL, 76),
                    En1545FixedInteger(En1545TransitData.ENV_UNKNOWN_D, 5),
                    En1545FixedInteger(En1545TransitData.HOLDER_INT_POSTAL_CODE, 14),
                    En1545FixedHex(En1545TransitData.ENV_UNKNOWN_E, 34)
                )
                else -> En1545Container(
                    En1545FixedInteger(En1545TransitData.ENV_VERSION_NUMBER, 6),
                    En1545FixedInteger(En1545TransitData.ENV_UNKNOWN_A, 7),
                    En1545FixedInteger(En1545TransitData.ENV_NETWORK_ID, 24),
                    En1545FixedInteger(En1545TransitData.ENV_UNKNOWN_B, 5),
                    En1545FixedInteger.date(En1545TransitData.ENV_APPLICATION_VALIDITY_END),
                    En1545FixedInteger(En1545TransitData.ENV_UNKNOWN_C, 10),
                    En1545FixedInteger.dateBCD(En1545TransitData.HOLDER_BIRTH_DATE),
                    En1545FixedHex(En1545TransitData.ENV_CARD_SERIAL, 76),
                    En1545FixedInteger(En1545TransitData.ENV_UNKNOWN_D, 5),
                    En1545FixedInteger(En1545TransitData.HOLDER_INT_POSTAL_CODE, 14),
                    En1545FixedHex(En1545TransitData.ENV_UNKNOWN_E, 34)
                )
            }

            private val extHolderFields = En1545Container(
                En1545FixedInteger(EXT_HOLDER_UNKNOWN_A, 18),
                En1545FixedHex(EXT_HOLDER_CARD_SERIAL, 76),
                En1545FixedInteger(EXT_HOLDER_UNKNOWN_B, 16),
                En1545FixedHex(EXT_HOLDER_UNKNOWN_C, 58),
                En1545FixedInteger(EXT_HOLDER_DATE_OF_BIRTH, 32),
                En1545FixedInteger(EXT_HOLDER_GENDER, 2),
                En1545FixedInteger(EXT_HOLDER_UNKNOWN_D, 3),
                En1545FixedString(EXT_HOLDER_NAME, 259)
            )
        }
    }
}
