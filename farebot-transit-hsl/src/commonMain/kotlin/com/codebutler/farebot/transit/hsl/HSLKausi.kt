/*
 * HSLKausi.kt
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
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.base.util.isAllZero
import com.codebutler.farebot.base.util.sliceOffLen
import com.codebutler.farebot.transit.en1545.En1545Container
import com.codebutler.farebot.transit.en1545.En1545FixedInteger
import com.codebutler.farebot.transit.en1545.En1545Lookup
import com.codebutler.farebot.transit.en1545.En1545Parsed
import com.codebutler.farebot.transit.en1545.En1545Parser
import com.codebutler.farebot.transit.en1545.En1545Subscription
import farebot.farebot_transit_hsl.generated.resources.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

class HSLKausi(
    override val parsed: En1545Parsed
) : En1545Subscription() {
    override val lookup: En1545Lookup
        get() = HSLLookup
    override val stringResource: StringResource
        get() = DefaultStringResource()

    internal fun formatPeriod(): String {
        val period = parsed.getIntOrZero(CONTRACT_PERIOD_DAYS)
        return runBlocking {
            getPluralString(Res.plurals.hsl_valid_days_calendar, period, period)
        }
    }

    override val subscriptionName: String?
        get() {
            val area = HSLLookup.getArea(
                parsed, prefix = CONTRACT_PREFIX,
                isValidity = true
            )
            return runBlocking { getString(Res.string.hsl_kausi_format, area ?: "") }
        }

    override val info: List<ListItemInterface>?
        get() = super.info.orEmpty() + listOf(
            ListItem(Res.string.hsl_period, formatPeriod())
        )

    companion object {
        private const val CONTRACT_PREFIX = "Contract"
        private const val CONTRACT_PERIOD_DAYS = "ContractPeriodDays"

        private val FIELDS_V1_PRODUCT = En1545Container(
            En1545FixedInteger("ProductCode1", 14),
            En1545FixedInteger(HSLLookup.contractAreaTypeName(CONTRACT_PREFIX), 1),
            En1545FixedInteger(HSLLookup.contractAreaName(CONTRACT_PREFIX), 4),
            En1545FixedInteger.date(CONTRACT_START),
            En1545FixedInteger.date(CONTRACT_END),
            En1545FixedInteger("reservedA", 1)
        )
        private val FIELDS_V1_LOAD = En1545Container(
            En1545FixedInteger("ProductCode", 14),
            En1545FixedInteger.date(CONTRACT_SALE),
            En1545FixedInteger.timeLocal(CONTRACT_SALE),
            En1545FixedInteger(CONTRACT_PERIOD_DAYS, 9),
            En1545FixedInteger(CONTRACT_PRICE_AMOUNT, 20),
            En1545FixedInteger("LoadingOrganisationID", 14),
            En1545FixedInteger(CONTRACT_SALE_DEVICE, 14)
        )

        private val FIELDS_V2_PRODUCT = En1545Container(
            En1545FixedInteger("ProductCodeType1", 1),
            En1545FixedInteger("ProductCode1", 14),
            En1545FixedInteger(HSLLookup.contractAreaTypeName(CONTRACT_PREFIX), 2),
            En1545FixedInteger(HSLLookup.contractAreaName(CONTRACT_PREFIX), 6),
            En1545FixedInteger.date(CONTRACT_START),
            En1545FixedInteger.date(CONTRACT_END),
            En1545FixedInteger("reserved", 5)
        )

        private val FIELDS_V2_LOAD = En1545Container(
            En1545FixedInteger("ProductCodeType", 1),
            En1545FixedInteger("ProductCode", 14),
            En1545FixedInteger.date(CONTRACT_SALE),
            En1545FixedInteger.timeLocal(CONTRACT_SALE),
            En1545FixedInteger(CONTRACT_PERIOD_DAYS, 9),
            En1545FixedInteger(CONTRACT_PRICE_AMOUNT, 20),
            En1545FixedInteger("LoadingOrganisationID", 14),
            En1545FixedInteger(CONTRACT_SALE_DEVICE, 13)
        )

        private val FIELDS_WALTTI_PRODUCT = En1545Container(
            En1545FixedInteger(HSLLookup.contractWalttiRegionName(CONTRACT_PREFIX), 8),
            En1545FixedInteger("ProductCodeType1", 4),
            En1545FixedInteger("ProductCode1", 14),
            En1545FixedInteger("Invoicable", 1),
            En1545FixedInteger(HSLLookup.contractWalttiZoneName(CONTRACT_PREFIX), 6),
            En1545FixedInteger.date(CONTRACT_START),
            En1545FixedInteger.date(CONTRACT_END),
            En1545FixedInteger("reservedA", 1)
        )
        private val FIELDS_WALTTI_LOAD = En1545Container(
            En1545FixedInteger("ProductCode", 14),
            En1545FixedInteger.date(CONTRACT_SALE),
            En1545FixedInteger.timeLocal(CONTRACT_SALE),
            En1545FixedInteger(CONTRACT_PERIOD_DAYS, 9),
            En1545FixedInteger(CONTRACT_PRICE_AMOUNT, 20),
            En1545FixedInteger("LoadingOrganisationID", 14),
            En1545FixedInteger(CONTRACT_SALE_DEVICE, 13),
            En1545FixedInteger("LoadedPass", 1)
        )

        data class ParseResult(val subs: List<HSLKausi>, val transaction: HSLTransaction?)

        private fun parseNonZero(raw: ByteArray, offByte: Int, lenByte: Int, field: En1545Container): En1545Parsed? {
            val cut = raw.sliceOffLen(offByte, lenByte)
            if (cut.isAllZero())
                return null
            return En1545Parser.parse(cut, field)
        }

        fun parse(raw: ByteArray, version: HSLVariant): ParseResult? {
            if (raw.isAllZero())
                return null
            val trip: HSLTransaction?
            val load: En1545Parsed
            val products: List<En1545Parsed>
            when (version) {
                HSLVariant.HSL_V2 -> {
                    trip = HSLTransaction.parseEmbed(raw = raw, version = version, offset = 208)
                    load = En1545Parser.parse(raw, off = 112, field = FIELDS_V2_LOAD)
                    products = listOfNotNull(
                        parseNonZero(raw, offByte = 0, lenByte = 7, field = FIELDS_V2_PRODUCT),
                        parseNonZero(raw, offByte = 7, lenByte = 7, field = FIELDS_V2_PRODUCT)
                    )
                }
                HSLVariant.HSL_V1 -> {
                    trip = HSLTransaction.parseEmbed(raw = raw, version = version, offset = 192)
                    load = En1545Parser.parse(raw, off = 96, field = FIELDS_V1_LOAD)
                    products = listOfNotNull(
                        parseNonZero(raw, offByte = 0, lenByte = 6, field = FIELDS_V1_PRODUCT),
                        parseNonZero(raw, offByte = 6, lenByte = 6, field = FIELDS_V1_PRODUCT)
                    )
                }
                HSLVariant.WALTTI -> {
                    trip = HSLTransaction.parseEmbed(raw = raw, version = version, offset = 280)
                    load = En1545Parser.parse(raw, off = 160, field = FIELDS_WALTTI_LOAD)
                    products = listOf(0, 10).mapNotNull {
                        parseNonZero(raw, offByte = it, lenByte = 10, field = FIELDS_WALTTI_PRODUCT)
                    }.filter {
                        it.getInt(CONTRACT_START + "Date") != 999 || it.getInt(CONTRACT_END + "Date") != 0
                    }
                }
            }
            if (products.isEmpty() && version == HSLVariant.WALTTI && load.getInt(CONTRACT_PERIOD_DAYS) == 511)
                return ParseResult(emptyList(), trip)
            if (products.isEmpty())
                return ParseResult(listOf(HSLKausi(load)), trip)
            return ParseResult(products.map { HSLKausi(load + it) }, trip)
        }
    }
}
