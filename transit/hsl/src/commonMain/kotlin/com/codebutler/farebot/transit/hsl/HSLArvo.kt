/*
 * HSLArvo.kt
 *
 * Copyright 2019 Google
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

package com.codebutler.farebot.transit.hsl

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.DefaultStringResource
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.base.util.getPluralStringBlocking
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.base.util.isAllZero
import com.codebutler.farebot.transit.en1545.En1545Container
import com.codebutler.farebot.transit.en1545.En1545FixedHex
import com.codebutler.farebot.transit.en1545.En1545FixedInteger
import com.codebutler.farebot.transit.en1545.En1545Lookup
import com.codebutler.farebot.transit.en1545.En1545Parsed
import com.codebutler.farebot.transit.en1545.En1545Parser
import com.codebutler.farebot.transit.en1545.En1545Subscription
import farebot.transit.hsl.generated.resources.*
import kotlin.time.Instant

class HSLArvo(
    override val parsed: En1545Parsed,
    val lastTransaction: HSLTransaction?,
    private val ultralightCity: Int? = null,
) : En1545Subscription() {
    override val lookup: En1545Lookup
        get() = HSLLookup
    override val stringResource: StringResource
        get() = DefaultStringResource()

    internal fun formatPeriod(): String {
        val period = parsed.getIntOrZero(CONTRACT_PERIOD)
        return when (parsed.getIntOrZero(CONTRACT_PERIOD_UNITS)) {
            0 -> getPluralStringBlocking(Res.plurals.hsl_valid_mins, period, period)
            1 -> getPluralStringBlocking(Res.plurals.hsl_valid_hours, period, period)
            2 -> getPluralStringBlocking(Res.plurals.hsl_valid_days_24h, period, period)
            else -> getPluralStringBlocking(Res.plurals.hsl_valid_days_calendar, period, period)
        }
    }

    internal val profile: String?
        get() {
            val prof = parsed.getInt(CUSTOMER_PROFILE)
            when (prof) {
                null -> {}
                1 -> return getStringBlocking(Res.string.hsl_adult)
                else -> return getStringBlocking(Res.string.hsl_unknown_format, prof.toString())
            }
            return when (parsed.getInt(CHILD)) {
                0 -> getStringBlocking(Res.string.hsl_adult)
                1 -> getStringBlocking(Res.string.hsl_child)
                else -> null
            }
        }

    internal val language get() = HSLLookup.languageCode(parsed.getInt(LANGUAGE_CODE))

    // Override to return null so base class doesn't add its own purchase date item.
    // We add our own with hour suffix in `info` below.
    override val purchaseTimestamp: Instant?
        get() = null

    private val purchaseDate: Instant?
        get() = parsed.getTimeStamp(CONTRACT_SALE, HSLLookup.timeZone)

    override val info: List<ListItemInterface>
        get() =
            super.info.orEmpty() +
                listOfNotNull(
                    ListItem(Res.string.hsl_period, formatPeriod()),
                    ListItem(Res.string.hsl_language, language),
                    profile?.let { ListItem(Res.string.hsl_customer_profile, it) },
                    purchaseDate?.let {
                        val hourStr = formatHour(parsed.getInt(CONTRACT_SALE_HOUR)) ?: ""
                        ListItem(Res.string.hsl_purchase_date, it.toString() + hourStr)
                    },
                )

    override val subscriptionName: String?
        get() {
            val area =
                HSLLookup.getArea(
                    parsed,
                    prefix = CONTRACT_PREFIX,
                    isValidity = true,
                    ultralightCity = ultralightCity,
                )
            return getStringBlocking(Res.string.hsl_arvo_format, area ?: "")
        }

    companion object {
        private const val CONTRACT_PERIOD_UNITS = "ContractPeriodUnits"
        private const val CONTRACT_PERIOD = "ContractPeriod"
        private const val CONTRACT_PREFIX = "Contract"
        private const val LANGUAGE_CODE = "LanguageCode"
        private const val CHILD = "Child"
        private const val CUSTOMER_PROFILE = "CustomerProfile"
        private const val CONTRACT_SALE_HOUR = "ContractSaleHour"

        private val FIELDS_WALTTI =
            En1545Container(
                En1545FixedInteger(HSLLookup.contractWalttiRegionName(CONTRACT_PREFIX), 8),
                En1545FixedInteger("ProductCode", 14),
                En1545FixedInteger(CUSTOMER_PROFILE, 5),
                En1545FixedInteger("CustomerProfileGroup", 5),
                En1545FixedInteger(LANGUAGE_CODE, 2),
                En1545FixedInteger(CONTRACT_PERIOD_UNITS, 2),
                En1545FixedInteger(CONTRACT_PERIOD, 8),
                En1545FixedInteger(HSLLookup.contractWalttiZoneName(CONTRACT_PREFIX), 6),
                En1545FixedInteger.date(CONTRACT_SALE),
                En1545FixedInteger(CONTRACT_SALE_HOUR, 5),
                En1545FixedInteger("SaleDeviceType", 3),
                En1545FixedInteger(CONTRACT_SALE_DEVICE, 14),
                En1545FixedInteger(CONTRACT_PRICE_AMOUNT, 14),
                En1545FixedInteger(CONTRACT_PASSENGER_TOTAL, 6),
                En1545FixedInteger("SaleStatus", 1),
                En1545FixedInteger(CONTRACT_UNKNOWN_A, 5),
                En1545FixedInteger.date(CONTRACT_START),
                En1545FixedInteger.timeLocal(CONTRACT_START),
                En1545FixedInteger.date(CONTRACT_END),
                En1545FixedInteger.timeLocal(CONTRACT_END),
                En1545FixedInteger("reservedA", 5),
                En1545FixedInteger("ValidityStatus", 1),
            )

        private val FIELDS_V1 =
            En1545Container(
                En1545FixedInteger("ProductCode", 14),
                En1545FixedInteger(CHILD, 1),
                En1545FixedInteger(LANGUAGE_CODE, 2),
                En1545FixedInteger(CONTRACT_PERIOD_UNITS, 2),
                En1545FixedInteger(CONTRACT_PERIOD, 8),
                En1545FixedInteger(HSLLookup.contractAreaTypeName(CONTRACT_PREFIX), 1),
                En1545FixedInteger(HSLLookup.contractAreaName(CONTRACT_PREFIX), 4),
                En1545FixedInteger.date(CONTRACT_SALE),
                En1545FixedInteger(CONTRACT_SALE_HOUR, 5),
                En1545FixedInteger("SaleDeviceType", 3),
                En1545FixedInteger(CONTRACT_SALE_DEVICE, 14),
                En1545FixedInteger(CONTRACT_PRICE_AMOUNT, 14),
                En1545FixedInteger(CONTRACT_PASSENGER_TOTAL, 5),
                En1545FixedInteger("SaleStatus", 1),
                En1545FixedInteger.date(CONTRACT_START),
                En1545FixedInteger.timeLocal(CONTRACT_START),
                En1545FixedInteger.date(CONTRACT_END),
                En1545FixedInteger.timeLocal(CONTRACT_END),
                En1545FixedInteger("reservedA", 5),
                En1545FixedInteger("ValidityStatus", 1),
            )

        private val FIELDS_V1_UL =
            En1545Container(
                En1545FixedInteger("ProductCode", 14),
                En1545FixedInteger(CHILD, 1),
                En1545FixedInteger(LANGUAGE_CODE, 2),
                En1545FixedInteger(CONTRACT_PERIOD_UNITS, 2),
                En1545FixedInteger(CONTRACT_PERIOD, 8),
                En1545FixedInteger(HSLLookup.contractAreaTypeName(CONTRACT_PREFIX), 1),
                En1545FixedInteger(HSLLookup.contractAreaName(CONTRACT_PREFIX), 4),
                En1545FixedInteger.date(CONTRACT_SALE),
                En1545FixedInteger(CONTRACT_SALE_HOUR, 5),
                En1545FixedInteger("SaleDeviceType", 3),
                En1545FixedInteger(CONTRACT_SALE_DEVICE, 14),
                En1545FixedInteger(CONTRACT_PRICE_AMOUNT, 14),
                En1545FixedInteger(CONTRACT_PASSENGER_TOTAL, 5),
                En1545FixedInteger("SaleStatus", 1),
                En1545FixedHex("Seal1", 48),
                En1545FixedInteger.date(CONTRACT_START),
                En1545FixedInteger.timeLocal(CONTRACT_START),
                En1545FixedInteger.date(CONTRACT_END),
                En1545FixedInteger.timeLocal(CONTRACT_END),
                En1545FixedInteger("reservedA", 5),
                En1545FixedInteger("ValidityStatus", 1),
            )

        private val FIELDS_V2 =
            En1545Container(
                En1545FixedInteger("ProductCodeType", 1),
                En1545FixedInteger("ProductCode", 14),
                En1545FixedInteger("ProductCodeGroup", 14),
                En1545FixedInteger(CUSTOMER_PROFILE, 5),
                En1545FixedInteger("CustomerProfileGroup", 5),
                En1545FixedInteger(LANGUAGE_CODE, 2),
                En1545FixedInteger(CONTRACT_PERIOD_UNITS, 2),
                En1545FixedInteger(CONTRACT_PERIOD, 8),
                En1545FixedInteger("ValidityLengthTypeGroup", 2),
                En1545FixedInteger("ValidityLengthGroup", 8),
                En1545FixedInteger(HSLLookup.contractAreaTypeName(CONTRACT_PREFIX), 2),
                En1545FixedInteger(HSLLookup.contractAreaName(CONTRACT_PREFIX), 6),
                En1545FixedInteger.date(CONTRACT_SALE),
                En1545FixedInteger(CONTRACT_SALE_HOUR, 5),
                En1545FixedInteger("SaleDeviceType", 3),
                En1545FixedInteger("SaleDeviceNumber", 14),
                En1545FixedInteger(CONTRACT_PRICE_AMOUNT, 14),
                En1545FixedInteger("TicketFareGroup", 14),
                En1545FixedInteger(CONTRACT_PASSENGER_TOTAL, 6),
                En1545FixedInteger("ExtraZone", 1),
                En1545FixedInteger("PeriodPassValidityArea", 6),
                En1545FixedInteger("ExtensionProductCode", 14),
                En1545FixedInteger("Extension1ValidityArea", 6),
                En1545FixedInteger("Extension1Fare", 14),
                En1545FixedInteger("Extension2ValidityArea", 6),
                En1545FixedInteger("Extension2Fare", 14),
                En1545FixedInteger("SaleStatus", 1),
                En1545FixedInteger("reservedA", 4),
                En1545FixedInteger.date(CONTRACT_START),
                En1545FixedInteger.timeLocal(CONTRACT_START),
                En1545FixedInteger.date(CONTRACT_END),
                En1545FixedInteger.timeLocal(CONTRACT_END),
                En1545FixedInteger("ValidityEndDateGroup", 14),
                En1545FixedInteger("ValidityEndTimeGroup", 11),
                En1545FixedInteger("reservedB", 5),
                En1545FixedInteger("ValidityStatus", 1),
            )

        private val FIELDS_V2_UL =
            En1545Container(
                En1545FixedInteger("ProductCode", 10),
                En1545FixedInteger(CHILD, 1),
                En1545FixedInteger(LANGUAGE_CODE, 2),
                En1545FixedInteger(CONTRACT_PERIOD_UNITS, 2),
                En1545FixedInteger(CONTRACT_PERIOD, 8),
                En1545FixedInteger(HSLLookup.contractAreaTypeName(CONTRACT_PREFIX), 2),
                En1545FixedInteger(HSLLookup.contractAreaName(CONTRACT_PREFIX), 6),
                En1545FixedInteger.date(CONTRACT_SALE),
                En1545FixedInteger(CONTRACT_SALE_HOUR, 5),
                En1545FixedInteger("SaleDeviceType", 3),
                En1545FixedInteger("SaleDeviceNumber", 14),
                En1545FixedInteger(CONTRACT_PRICE_AMOUNT, 15),
                En1545FixedInteger(CONTRACT_PASSENGER_TOTAL, 6),
                En1545FixedHex("Seal1", 48),
                En1545FixedInteger.date(CONTRACT_START),
                En1545FixedInteger.timeLocal(CONTRACT_START),
                En1545FixedInteger.date(CONTRACT_END),
                En1545FixedInteger.timeLocal(CONTRACT_END),
                // RFU 14 bits and seal2 64 bits
            )

        fun parse(
            raw: ByteArray,
            version: HSLVariant,
        ): HSLArvo? {
            if (raw.isAllZero()) {
                return null
            }
            val (fields, offset) =
                when (version) {
                    HSLVariant.HSL_V1 -> Pair(FIELDS_V1, 144)
                    HSLVariant.HSL_V2 -> Pair(FIELDS_V2, 286)
                    HSLVariant.WALTTI -> Pair(FIELDS_WALTTI, 168)
                }
            val parsed = En1545Parser.parse(raw, fields)
            return HSLArvo(
                parsed,
                HSLTransaction.parseEmbed(
                    raw = raw,
                    version = version,
                    offset = offset,
                    walttiArvoRegion = parsed.getInt(HSLLookup.contractWalttiRegionName(CONTRACT_PREFIX)),
                ),
            )
        }

        fun parseUL(
            raw: ByteArray,
            version: Int,
            city: Int,
        ): HSLArvo? {
            if (raw.isAllZero()) {
                return null
            }
            if (version == 2) {
                return HSLArvo(
                    En1545Parser.parse(raw, FIELDS_V2_UL),
                    HSLTransaction.parseEmbed(
                        raw = raw,
                        version = HSLVariant.HSL_V2,
                        offset = 264,
                        ultralightCity = city,
                    ),
                    ultralightCity = city,
                )
            }
            return HSLArvo(
                En1545Parser.parse(raw, FIELDS_V1_UL),
                HSLTransaction.parseEmbed(
                    raw = raw,
                    version = HSLVariant.HSL_V1,
                    offset = 264,
                    ultralightCity = city,
                ),
                ultralightCity = city,
            )
        }

        fun formatHour(hour: Int?): String? {
            hour ?: return null
            return " ${NumberUtils.zeroPad(hour, 2)}:XX"
        }
    }
}
