/*
 * HSLUltralightTransitFactory.kt
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

package com.codebutler.farebot.transit.ultralight

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.base.util.getHexString
import com.codebutler.farebot.base.util.isAllZero
import com.codebutler.farebot.base.util.sliceOffLen
import com.codebutler.farebot.card.ultralight.UltralightCard
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransactionTrip
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.en1545.En1545Container
import com.codebutler.farebot.transit.en1545.En1545FixedHex
import com.codebutler.farebot.transit.en1545.En1545FixedInteger
import com.codebutler.farebot.transit.en1545.En1545Lookup
import com.codebutler.farebot.base.util.DefaultStringResource
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.transit.en1545.En1545Parsed
import com.codebutler.farebot.transit.en1545.En1545Parser
import com.codebutler.farebot.transit.en1545.En1545Subscription
import com.codebutler.farebot.transit.en1545.En1545Transaction
import farebot.farebot_transit_ultralight.generated.resources.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.getString
import kotlin.time.Instant

private const val CITY_UL_TAMPERE = 1

private fun getNameUL(city: Int) = runBlocking {
    if (city == CITY_UL_TAMPERE) getString(Res.string.tampere_ultralight_card_name)
    else getString(Res.string.hsl_ultralight_card_name)
}

/**
 * HSL (Helsinki) and Tampere Ultralight transit cards.
 * Ported from Metrodroid.
 */
class HSLUltralightTransitFactory : TransitFactory<UltralightCard, HSLUltralightTransitInfo> {

    override fun check(card: UltralightCard): Boolean {
        val page4 = card.getPage(4).data
        return page4.getBitsFromBuffer(0, 4) in 1..2 &&
            page4.getBitsFromBuffer(8, 24) == 0x924621
    }

    override fun parseIdentity(card: UltralightCard): TransitIdentity {
        val city = card.pages[5].data.getBitsFromBuffer(0, 8)
        return TransitIdentity.create(
            getNameUL(city),
            formatSerial(getSerial(card))
        )
    }

    override fun parseInfo(card: UltralightCard): HSLUltralightTransitInfo {
        val raw = card.readPages(4, 12)
        val version = raw.getBitsFromBuffer(0, 4)
        val city = card.pages[5].data.getBitsFromBuffer(0, 8)

        val arvo = HSLUltralightArvo.parseUL(raw.sliceOffLen(7, 41), version, city)

        return HSLUltralightTransitInfo(
            serialNumber = formatSerial(getSerial(card)),
            subscriptions = listOfNotNull(arvo),
            applicationVersion = version,
            applicationKeyVersion = card.pages[4].data.getBitsFromBuffer(4, 4),
            platformType = card.pages[5].data.getBitsFromBuffer(20, 3),
            securityLevel = card.pages[5].data.getBitsFromBuffer(23, 1),
            trips = TransactionTrip.merge(listOfNotNull(arvo?.lastTransaction)),
            city = city
        )
    }

    companion object {
        private fun getSerial(card: UltralightCard): String {
            val num = (card.tagId.byteArrayToInt(1, 3) xor card.tagId.byteArrayToInt(4, 3)) and 0x7fffff
            return card.readPages(4, 2).getHexString(1, 5) +
                NumberUtils.zeroPad(num, 7) + card.pages[5].data.getBitsFromBuffer(16, 4)
        }

        internal fun formatSerial(serial: String): String {
            if (serial.length < 18) return serial
            return serial.substring(0, 6) + " " +
                serial.substring(6, 13) + " " +
                serial.substring(13, 17) + " " +
                serial.substring(17)
        }
    }
}

class HSLUltralightTransitInfo internal constructor(
    override val serialNumber: String,
    override val trips: List<Trip>,
    override val subscriptions: List<Subscription>?,
    val applicationVersion: Int,
    val applicationKeyVersion: Int,
    val platformType: Int,
    val securityLevel: Int,
    val city: Int
) : TransitInfo() {
    override val cardName: String get() = getNameUL(city)

    override val info: List<ListItemInterface>
        get() = listOf(
            ListItem(Res.string.hsl_application_version, applicationVersion.toString()),
            ListItem(Res.string.hsl_application_key_version, applicationKeyVersion.toString()),
            ListItem(Res.string.hsl_platform_type, platformType.toString()),
            ListItem(Res.string.hsl_security_level, securityLevel.toString())
        )
}

// --- HSL Ultralight Lookup ---

private object HSLUltralightLookup : En1545Lookup {
    override val timeZone: TimeZone = TimeZone.of("Europe/Helsinki")
    override fun parseCurrency(price: Int) = TransitCurrency.EUR(price)

    override fun getRouteName(routeNumber: Int?, routeVariant: Int?, agency: Int?, transport: Int?): String? = null
    override fun getAgencyName(agency: Int?, isShort: Boolean): String? = null
    override fun getStation(station: Int, agency: Int?, transport: Int?): Station? = null
    override fun getSubscriptionName(stringResource: StringResource, agency: Int?, contractTariff: Int?): String? = null
    override fun getMode(agency: Int?, route: Int?): Trip.Mode = Trip.Mode.BUS

    fun contractAreaTypeName(prefix: String) = "${prefix}AreaType"
    fun contractAreaName(prefix: String) = "${prefix}Area"
    fun contractWalttiZoneName(prefix: String) = "${prefix}WalttiZone"
    fun contractWalttiRegionName(prefix: String) = "${prefix}WalttiRegion"

    fun languageCode(input: Int?) = when (input) {
        0 -> "Finnish"
        1 -> "Swedish"
        2 -> "English"
        else -> "Unknown ($input)"
    }

    private val areaMap = mapOf(
        Pair(0, 1) to "Helsinki",
        Pair(0, 2) to "Espoo",
        Pair(0, 4) to "Vantaa",
        Pair(0, 5) to "Seutu",
        Pair(0, 6) to "Kirkkonummi-Siuntio",
        Pair(0, 7) to "Vihti",
        Pair(0, 8) to "Nurmijärvi",
        Pair(0, 9) to "Kerava-Sipoo-Tuusula",
        Pair(0, 10) to "Sipoo",
        Pair(0, 14) to "Lähiseutu 2",
        Pair(0, 15) to "Lähiseutu 3",
        Pair(1, 1) to "Bus",
        Pair(1, 2) to "Bus 2 zones",
        Pair(1, 3) to "Bus 3 zones",
        Pair(1, 4) to "Bus 4+ zones",
        Pair(1, 5) to "Tram",
        Pair(1, 6) to "Metro",
        Pair(1, 7) to "Train",
        Pair(1, 8) to "Ferry",
        Pair(1, 9) to "U-linja"
    )

    private val walttiValiditySplit = listOf(Pair(0, 0)) +
        (1..10).map { Pair(it, it) } +
        (1..10).flatMap { start -> ((start + 1)..10).map { Pair(start, it) } }

    private fun mapWalttiZone(region: Int, id: Int): String {
        return charArrayOf(('A'.code + id - 1).toChar()).concatToString()
    }

    fun getArea(
        parsed: En1545Parsed,
        prefix: String,
        isValidity: Boolean,
        walttiRegion: Int? = null,
        ultralightCity: Int? = null
    ): String? {
        if (parsed.getInt(contractAreaName(prefix)) == null &&
            parsed.getInt(contractWalttiZoneName(prefix)) != null
        ) {
            val region = walttiRegion ?: parsed.getIntOrZero(contractWalttiRegionName(prefix))
            val regionName = region.toString()
            val zone = parsed.getIntOrZero(contractWalttiZoneName(prefix))
            if (zone == 0) return null
            if (!isValidity && zone in 1..10) {
                return "$regionName zone ${mapWalttiZone(region, zone)}"
            }
            val (start, end) = walttiValiditySplit[zone]
            return "$regionName zones ${mapWalttiZone(region, start)} - ${mapWalttiZone(region, end)}"
        }
        val type = parsed.getIntOrZero(contractAreaTypeName(prefix))
        val value = parsed.getIntOrZero(contractAreaName(prefix))
        if (type in 0..1 && value == 0) return null
        if (ultralightCity == CITY_UL_TAMPERE && type == 0) {
            val from = value % 6
            if (isValidity) {
                val to = value / 6
                val num = to - from + 1
                val zones = (from..to).map { ('A'.code + it).toChar() }.toCharArray().concatToString()
                return if (num == 1) "Zone $zones" else "$num zones $zones"
            } else {
                return "Zone ${charArrayOf(('A'.code + from).toChar()).concatToString()}"
            }
        }
        if (type == 2) {
            val to = value and 7
            if (isValidity) {
                val from = value shr 3
                val num = to - from + 1
                val zones = (from..to).map { ('A'.code + it).toChar() }.toCharArray().concatToString()
                return if (num == 1) "Zone $zones" else "$num zones $zones"
            } else {
                return "Zone ${charArrayOf(('A'.code + to).toChar()).concatToString()}"
            }
        }
        return areaMap[Pair(type, value)] ?: "Unknown ($type/$value)"
    }
}

// --- HSL Ultralight Arvo (subscription) ---

private class HSLUltralightArvo(
    override val parsed: En1545Parsed,
    val lastTransaction: HSLUltralightTransaction?,
    private val ultralightCity: Int? = null
) : En1545Subscription() {
    override val lookup: En1545Lookup get() = HSLUltralightLookup
    override val stringResource: StringResource get() = DefaultStringResource()

    private fun formatPeriod(): String {
        val period = parsed.getIntOrZero(CONTRACT_PERIOD)
        return when (parsed.getIntOrZero(CONTRACT_PERIOD_UNITS)) {
            0 -> if (period == 1) "$period minute" else "$period minutes"
            1 -> if (period == 1) "$period hour" else "$period hours"
            2 -> if (period == 1) "$period day (24h)" else "$period days (24h)"
            else -> if (period == 1) "$period day (calendar)" else "$period days (calendar)"
        }
    }

    private val profile: String?
        get() {
            val prof = parsed.getInt(CUSTOMER_PROFILE)
            when (prof) {
                null -> {}
                1 -> return "Adult"
                else -> return "Unknown ($prof)"
            }
            return when (parsed.getInt(CHILD)) {
                0 -> "Adult"
                1 -> "Child"
                else -> null
            }
        }

    private val language get() = HSLUltralightLookup.languageCode(parsed.getInt(LANGUAGE_CODE))

    override val info: List<ListItemInterface>
        get() {
            val items = mutableListOf<ListItemInterface>()
            items.add(ListItem(Res.string.hsl_period, formatPeriod()))
            items.add(ListItem(Res.string.hsl_language, language))
            profile?.let { items.add(ListItem(Res.string.hsl_customer_profile, it)) }
            val saleHour = parsed.getInt(CONTRACT_SALE_HOUR)
            val purchaseStr = purchaseTimestamp?.toString().orEmpty()
            if (saleHour != null) {
                items.add(ListItem(Res.string.hsl_purchase_date, "$purchaseStr ${NumberUtils.zeroPad(saleHour, 2)}:XX"))
            } else if (purchaseStr.isNotEmpty()) {
                items.add(ListItem(Res.string.hsl_purchase_date, purchaseStr))
            }
            return items
        }

    override val subscriptionName: String?
        get() {
            val area = HSLUltralightLookup.getArea(
                parsed, prefix = CONTRACT_PREFIX,
                isValidity = true, ultralightCity = ultralightCity
            )
            return if (area != null) "Arvo ($area)" else "Arvo"
        }

    companion object {
        private const val CONTRACT_PERIOD_UNITS = "ContractPeriodUnits"
        private const val CONTRACT_PERIOD = "ContractPeriod"
        private const val CONTRACT_PREFIX = "Contract"
        private const val LANGUAGE_CODE = "LanguageCode"
        private const val CHILD = "Child"
        private const val CUSTOMER_PROFILE = "CustomerProfile"
        private const val CONTRACT_SALE_HOUR = "ContractSaleHour"

        private val FIELDS_V1_UL = En1545Container(
            En1545FixedInteger("ProductCode", 14),
            En1545FixedInteger(CHILD, 1),
            En1545FixedInteger(LANGUAGE_CODE, 2),
            En1545FixedInteger(CONTRACT_PERIOD_UNITS, 2),
            En1545FixedInteger(CONTRACT_PERIOD, 8),
            En1545FixedInteger(HSLUltralightLookup.contractAreaTypeName(CONTRACT_PREFIX), 1),
            En1545FixedInteger(HSLUltralightLookup.contractAreaName(CONTRACT_PREFIX), 4),
            En1545FixedInteger.date(En1545Subscription.CONTRACT_SALE),
            En1545FixedInteger(CONTRACT_SALE_HOUR, 5),
            En1545FixedInteger("SaleDeviceType", 3),
            En1545FixedInteger(En1545Subscription.CONTRACT_SALE_DEVICE, 14),
            En1545FixedInteger(En1545Subscription.CONTRACT_PRICE_AMOUNT, 14),
            En1545FixedInteger(En1545Subscription.CONTRACT_PASSENGER_TOTAL, 5),
            En1545FixedInteger("SaleStatus", 1),
            En1545FixedHex("Seal1", 48),
            En1545FixedInteger.date(En1545Subscription.CONTRACT_START),
            En1545FixedInteger.timeLocal(En1545Subscription.CONTRACT_START),
            En1545FixedInteger.date(En1545Subscription.CONTRACT_END),
            En1545FixedInteger.timeLocal(En1545Subscription.CONTRACT_END),
            En1545FixedInteger("reservedA", 5),
            En1545FixedInteger("ValidityStatus", 1)
        )

        private val FIELDS_V2_UL = En1545Container(
            En1545FixedInteger("ProductCode", 10),
            En1545FixedInteger(CHILD, 1),
            En1545FixedInteger(LANGUAGE_CODE, 2),
            En1545FixedInteger(CONTRACT_PERIOD_UNITS, 2),
            En1545FixedInteger(CONTRACT_PERIOD, 8),
            En1545FixedInteger(HSLUltralightLookup.contractAreaTypeName(CONTRACT_PREFIX), 2),
            En1545FixedInteger(HSLUltralightLookup.contractAreaName(CONTRACT_PREFIX), 6),
            En1545FixedInteger.date(En1545Subscription.CONTRACT_SALE),
            En1545FixedInteger(CONTRACT_SALE_HOUR, 5),
            En1545FixedInteger("SaleDeviceType", 3),
            En1545FixedInteger("SaleDeviceNumber", 14),
            En1545FixedInteger(En1545Subscription.CONTRACT_PRICE_AMOUNT, 15),
            En1545FixedInteger(En1545Subscription.CONTRACT_PASSENGER_TOTAL, 6),
            En1545FixedHex("Seal1", 48),
            En1545FixedInteger.date(En1545Subscription.CONTRACT_START),
            En1545FixedInteger.timeLocal(En1545Subscription.CONTRACT_START),
            En1545FixedInteger.date(En1545Subscription.CONTRACT_END),
            En1545FixedInteger.timeLocal(En1545Subscription.CONTRACT_END)
            // RFU 14 bits and seal2 64 bits
        )

        fun parseUL(raw: ByteArray, version: Int, city: Int): HSLUltralightArvo? {
            if (raw.isAllZero()) return null
            val (fields, embedFields) = if (version == 2) {
                Pair(FIELDS_V2_UL, HSLUltralightTransaction.EMBED_FIELDS_V2)
            } else {
                Pair(FIELDS_V1_UL, HSLUltralightTransaction.EMBED_FIELDS_V1)
            }
            val parsed = En1545Parser.parse(raw, fields)
            val transaction = HSLUltralightTransaction.parseEmbed(raw, 264, embedFields, city)
            return HSLUltralightArvo(parsed, transaction, ultralightCity = city)
        }
    }
}

// --- HSL Ultralight Transaction ---

internal class HSLUltralightTransaction(
    override val parsed: En1545Parsed,
    private val ultralightCity: Int? = null
) : En1545Transaction() {

    override val lookup: En1545Lookup get() = HSLUltralightLookup

    override val station: Station?
        get() = HSLUltralightLookup.getArea(
            parsed, AREA_PREFIX, isValidity = false,
            ultralightCity = ultralightCity
        )?.let { Station.nameOnly(it) }

    override val mode: Trip.Mode
        get() = when (parsed.getInt(LOCATION_NUMBER)) {
            null -> Trip.Mode.BUS
            1300 -> Trip.Mode.METRO
            1019 -> Trip.Mode.FERRY
            in 1000..1010 -> Trip.Mode.TRAM
            in 3000..3999 -> Trip.Mode.TRAIN
            else -> Trip.Mode.BUS
        }

    override val routeNumber: Int?
        get() = parsed.getInt(LOCATION_NUMBER)

    override val routeNames: List<String>
        get() = listOfNotNull(parsed.getInt(LOCATION_NUMBER)?.let { (it % 1000).toString() })

    companion object {
        private const val AREA_PREFIX = "EventBoarding"
        private const val LOCATION_TYPE = "BoardingLocationNumberType"
        private const val LOCATION_NUMBER = "BoardingLocationNumber"

        val EMBED_FIELDS_V1 = En1545Container(
            En1545FixedInteger.date(En1545Transaction.EVENT),
            En1545FixedInteger.timeLocal(En1545Transaction.EVENT),
            En1545FixedInteger(En1545Transaction.EVENT_VEHICLE_ID, 14),
            En1545FixedInteger(LOCATION_TYPE, 2),
            En1545FixedInteger(LOCATION_NUMBER, 14),
            En1545FixedInteger("BoardingDirection", 1),
            En1545FixedInteger(HSLUltralightLookup.contractAreaName(AREA_PREFIX), 4),
            En1545FixedInteger("reserved", 4)
        )

        val EMBED_FIELDS_V2 = En1545Container(
            En1545FixedInteger.date(En1545Transaction.EVENT),
            En1545FixedInteger.timeLocal(En1545Transaction.EVENT),
            En1545FixedInteger(En1545Transaction.EVENT_VEHICLE_ID, 14),
            En1545FixedInteger(LOCATION_TYPE, 2),
            En1545FixedInteger(LOCATION_NUMBER, 14),
            En1545FixedInteger("BoardingDirection", 1),
            En1545FixedInteger(HSLUltralightLookup.contractAreaTypeName(AREA_PREFIX), 2),
            En1545FixedInteger(HSLUltralightLookup.contractAreaName(AREA_PREFIX), 6)
        )

        fun parseEmbed(
            raw: ByteArray,
            offset: Int,
            fields: En1545Container,
            city: Int
        ): HSLUltralightTransaction? {
            val parsed = En1545Parser.parse(raw, offset, fields)
            if (parsed.getTimeStamp(En1545Transaction.EVENT, HSLUltralightLookup.timeZone) == null)
                return null
            return HSLUltralightTransaction(parsed, ultralightCity = city)
        }
    }
}
