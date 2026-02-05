/*
 * TroikaUltralightTransitFactory.kt
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

package com.codebutler.farebot.transit.ultralight

import com.codebutler.farebot.base.mdst.MdstStationLookup
import com.codebutler.farebot.base.ui.HeaderListItem
import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.base.util.getHexString
import com.codebutler.farebot.base.util.hexString
import com.codebutler.farebot.card.ultralight.UltralightCard
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.farebot_transit_ultralight.generated.resources.Res
import farebot.farebot_transit_ultralight.generated.resources.moscow_ground_transport
import farebot.farebot_transit_ultralight.generated.resources.moscow_mcc
import farebot.farebot_transit_ultralight.generated.resources.moscow_metro
import farebot.farebot_transit_ultralight.generated.resources.moscow_monorail
import farebot.farebot_transit_ultralight.generated.resources.troika_druzhinnik_card
import farebot.farebot_transit_ultralight.generated.resources.troika_empty_ticket_holder
import farebot.farebot_transit_ultralight.generated.resources.troika_fare_90mins
import farebot.farebot_transit_ultralight.generated.resources.troika_fare_single
import farebot.farebot_transit_ultralight.generated.resources.troika_layout
import farebot.farebot_transit_ultralight.generated.resources.troika_purse
import farebot.farebot_transit_ultralight.generated.resources.troika_refill_counter
import farebot.farebot_transit_ultralight.generated.resources.troika_rides_one
import farebot.farebot_transit_ultralight.generated.resources.troika_rides_other
import farebot.farebot_transit_ultralight.generated.resources.troika_trips_on_purse
import farebot.farebot_transit_ultralight.generated.resources.troika_unknown_ticket
import farebot.farebot_transit_ultralight.generated.resources.unknown
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.jetbrains.compose.resources.getString
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private const val NAME = "Troika"
private val MOSCOW = TimeZone.of("Europe/Moscow")

private val TROIKA_EPOCH_1992 = LocalDate(1992, 1, 1).atStartOfDayIn(MOSCOW)
private val TROIKA_EPOCH_2016 = LocalDate(2016, 1, 1).atStartOfDayIn(MOSCOW)
private val TROIKA_EPOCH_2019 = LocalDate(2019, 1, 1).atStartOfDayIn(MOSCOW)

/**
 * Troika Ultralight card (Moscow Metro).
 * Ported from Metrodroid.
 */
class TroikaUltralightTransitFactory : TransitFactory<UltralightCard, TroikaUltralightTransitInfo> {

    override fun check(card: UltralightCard): Boolean =
        TroikaBlock.check(card.getPage(4).data)

    override fun parseIdentity(card: UltralightCard): TransitIdentity {
        val rawData = card.readPages(4, 2)
        return TransitIdentity.create(NAME, TroikaBlock.formatSerial(TroikaBlock.getSerial(rawData)))
    }

    override fun parseInfo(card: UltralightCard): TroikaUltralightTransitInfo {
        val rawData = card.readPages(4, 12)
        val block = TroikaBlock.parseBlock(rawData)
        return TroikaUltralightTransitInfo(block)
    }
}

class TroikaUltralightTransitInfo internal constructor(
    private val block: TroikaBlock
) : TransitInfo() {
    override val cardName: String get() = NAME
    override val serialNumber: String get() = block.serialNumber
    override val trips: List<Trip> get() = block.trips
    override val subscriptions: List<Subscription>? get() = listOfNotNull(block.subscription)
    override val balances: List<TransitBalance>? get() = listOfNotNull(block.balance)
    override val info: List<ListItemInterface>? get() = block.info
}

// --- Epoch date/time converters ---

private fun convertDateTime1992(days: Int, mins: Int): Instant? =
    if (days == 0 && mins == 0) null
    else TROIKA_EPOCH_1992 + (days - 1).days + mins.minutes

private fun convertDate1992(days: Int): Instant? =
    if (days == 0) null
    else TROIKA_EPOCH_1992 + (days - 1).days

private fun convertDate2019(days: Int): Instant? =
    if (days == 0) null
    else TROIKA_EPOCH_2019 + (days - 1).days

private fun convertDateTime2019(days: Int, mins: Int): Instant? =
    if (days == 0 && mins == 0) null
    else TROIKA_EPOCH_2019 + (days - 1).days + mins.minutes

private fun convertDateTime2016(days: Int, mins: Int): Instant? =
    if (days == 0 && mins == 0) null
    else TROIKA_EPOCH_2016 + (days - 1).days + mins.minutes

// --- Transport type enum ---

internal enum class TroikaTransportType {
    NONE,
    UNKNOWN,
    SUBWAY,
    MONORAIL,
    GROUND,
    MCC
}

// --- TroikaBlock abstract base ---

abstract class TroikaBlock(
    private val serial: Long,
    val layout: Int,
    val ticketType: Int,
    private val lastTransportLeadingCode: Int?,
    private val lastTransportLongCode: Int?,
    private val lastTransportRaw: String?,
    protected val lastValidator: Int?,
    protected val validityLengthMinutes: Int?,
    protected val expiryDate: Instant?,
    protected val lastValidationTime: Instant?,
    private val validityStart: Instant?,
    protected val validityEnd: Instant?,
    private val remainingTrips: Int?,
    protected val transfers: List<Int>,
    private val fareDesc: String?,
    private val checkSum: String?
) {

    val serialNumber: String get() = formatSerial(serial)

    open val subscription: Subscription?
        get() = TroikaSubscription(
            expiryDate = expiryDate,
            validFrom = validityStart,
            validityEnd = validityEnd,
            remainingTripCount = remainingTrips,
            validityLengthMinutes = validityLengthMinutes,
            ticketType = ticketType
        )

    open val info: List<ListItemInterface>?
        get() = null

    open val balance: TransitBalance?
        get() = null

    open val lastRefillTime: Instant?
        get() = null

    val trips: List<Trip>
        get() {
            val t = mutableListOf<Trip>()
            val rawTransport = lastTransportRaw
                ?: (lastTransportLeadingCode?.shl(8)?.or(lastTransportLongCode ?: 0))?.toString(16)
            if (lastValidationTime != null) {
                var isLast = true
                for (transfer in transfers.filter { it != 0 }.sortedByDescending { it } + listOf(0)) {
                    val transferTime = lastValidationTime + transfer.minutes
                    if (isLast)
                        t += TroikaTrip(transferTime, getTransportType(true), lastValidator, rawTransport, fareDesc)
                    else
                        t += TroikaTrip(transferTime, getTransportType(false), null, rawTransport, fareDesc)
                    isLast = false
                }
            }
            lastRefillTime?.let {
                t.add(TroikaRefill(it))
            }
            return t
        }

    internal open fun getTransportType(getLast: Boolean): TroikaTransportType? {
        when (lastTransportLeadingCode) {
            0 -> return TroikaTransportType.NONE
            1 -> { /* fall through to long code parsing */ }
            2 -> return if (getLast) TroikaTransportType.GROUND else TroikaTransportType.UNKNOWN
            else -> return TroikaTransportType.UNKNOWN
        }

        if (lastTransportLongCode == 0 || lastTransportLongCode == null)
            return TroikaTransportType.UNKNOWN

        // This is actually 4 fields used in sequence.
        var first: TroikaTransportType? = null
        var last: TroikaTransportType? = null

        var i = 6
        var found = 0
        while (i >= 0) {
            val shortCode = lastTransportLongCode shr i and 3
            if (shortCode == 0) {
                i -= 2
                continue
            }
            val type = when (shortCode) {
                1 -> TroikaTransportType.SUBWAY
                2 -> TroikaTransportType.MONORAIL
                3 -> TroikaTransportType.MCC
                else -> null
            }
            if (first == null) first = type
            last = type
            found++
            i -= 2
        }
        if (found == 1 && !getLast)
            return TroikaTransportType.UNKNOWN
        return if (getLast) last else first
    }

    constructor(
        rawData: ByteArray,
        lastTransportLeadingCode: Int? = null,
        lastTransportLongCode: Int? = null,
        lastTransportRaw: String? = null,
        lastValidator: Int? = null,
        validityLengthMinutes: Int? = null,
        expiryDate: Instant? = null,
        lastValidationTime: Instant? = null,
        validityStart: Instant? = null,
        validityEnd: Instant? = null,
        remainingTrips: Int? = null,
        transfers: List<Int> = listOf(),
        fareDesc: String? = null,
        checkSum: String? = null
    ) : this(
        serial = getSerial(rawData),
        layout = getLayout(rawData),
        ticketType = getTicketType(rawData),
        lastTransportLeadingCode = lastTransportLeadingCode,
        lastTransportLongCode = lastTransportLongCode,
        lastTransportRaw = lastTransportRaw,
        lastValidator = lastValidator,
        validityLengthMinutes = validityLengthMinutes,
        expiryDate = expiryDate,
        lastValidationTime = lastValidationTime,
        validityStart = validityStart,
        validityEnd = validityEnd,
        remainingTrips = remainingTrips,
        transfers = transfers,
        fareDesc = fareDesc,
        checkSum = checkSum
    )

    companion object {
        fun formatSerial(sn: Long) = NumberUtils.formatNumber(sn, " ", 4, 3, 3)

        fun getSerial(rawData: ByteArray) =
            rawData.getBitsFromBuffer(20, 32).toLong() and 0xffffffffL

        private fun getTicketType(rawData: ByteArray) =
            rawData.getBitsFromBuffer(4, 16)

        private fun getLayout(rawData: ByteArray) = rawData.getBitsFromBuffer(52, 4)

        fun getHeader(ticketType: Int) = runBlocking {
            when (ticketType) {
                0x5d3d, 0x5d3e, 0x5d48, 0x2135 -> getString(Res.string.troika_empty_ticket_holder)
                0x183d, 0x2129 -> getString(Res.string.troika_druzhinnik_card)
                0x5d9a -> troikaRides(1)
                0x5d9b -> troikaRides(1)
                0x5d9c -> troikaRides(2)
                0x5da0 -> troikaRides(20)
                0x5db1 -> getString(Res.string.troika_purse)
                0x5dd3 -> troikaRides(60)
                else -> getString(Res.string.troika_unknown_ticket, ticketType.toString(16))
            }
        }

        private fun troikaRides(rides: Int) = runBlocking {
            if (rides == 1) getString(Res.string.troika_rides_one, rides)
            else getString(Res.string.troika_rides_other, rides)
        }

        fun check(rawData: ByteArray): Boolean =
            rawData.getBitsFromBuffer(0, 10) in listOf(0x117, 0x108, 0x106)

        fun parseBlock(rawData: ByteArray): TroikaBlock = when (getLayout(rawData)) {
            0x2 -> TroikaLayout2(rawData)
            0xa -> TroikaLayoutA(rawData)
            0xd -> TroikaLayoutD(rawData)
            0xe -> when (rawData.getBitsFromBuffer(56, 5)) {
                2 -> TroikaLayoutE2(rawData)
                3 -> TroikaPurseE3(rawData)
                5 -> TroikaPurseE5(rawData)
                else -> TroikaUnknownBlock(rawData)
            }
            else -> TroikaUnknownBlock(rawData)
        }
    }
}

// --- Layout subclasses ---

// This was seen only as placeholder for Troika card sector 7
private class TroikaLayout2(rawData: ByteArray) : TroikaBlock(
    rawData,
    expiryDate = convertDateTime1992(rawData.getBitsFromBuffer(56, 16), 0),
    // 69 bits unknown
    lastValidationTime = convertDateTime1992(
        rawData.getBitsFromBuffer(141, 16),
        rawData.getBitsFromBuffer(130, 11)
    ),
    validityStart = convertDateTime1992(rawData.getBitsFromBuffer(157, 16), 0),
    validityEnd = convertDateTime1992(rawData.getBitsFromBuffer(173, 16), 0),
    // 16 bits unknown
    lastValidator = rawData.getBitsFromBuffer(205, 16)
    // 35 bits unknown
) {
    // Empty holder
    override val subscription: Subscription?
        get() = if (ticketType == 0x5d3d || ticketType == 0x5d3e || ticketType == 0x5d48
            || ticketType == 0x2135 || ticketType == 0x2141
        ) null else super.subscription
}

// This layout is found on newer single and double-rides
private class TroikaLayoutA(
    rawData: ByteArray,
    validityStartDays: Int = rawData.getBitsFromBuffer(67, 9)
) : TroikaBlock(
    rawData,
    // 3 bits unknown
    validityLengthMinutes = rawData.getBitsFromBuffer(76, 19),
    // 1 bit unknown
    lastValidationTime = convertDateTime2016(validityStartDays, rawData.getBitsFromBuffer(96, 19)),
    // 4 bits unknown
    transfers = listOf(rawData.getBitsFromBuffer(119, 7)),
    remainingTrips = rawData.getBitsFromBuffer(128, 8),
    lastValidator = rawData.getBitsFromBuffer(136, 16),
    lastTransportLeadingCode = rawData.getBitsFromBuffer(126, 2),
    lastTransportLongCode = rawData.getBitsFromBuffer(152, 8),
    // 32 bits zero
    checkSum = rawData.getHexString(8, 5).substring(1, 4),
    validityEnd = convertDateTime2016(validityStartDays, rawData.getBitsFromBuffer(76, 19) - 1),
    validityStart = convertDateTime2016(validityStartDays, 0)
    // missing: expiry date
)

// This layout is found on older multi-ride passes
private class TroikaLayoutD(rawData: ByteArray) : TroikaBlock(
    rawData,
    validityEnd = convertDate1992(rawData.getBitsFromBuffer(64, 16)),
    // 16 bits unknown
    // 32 bits repetition
    validityStart = convertDate1992(rawData.getBitsFromBuffer(128, 16)),
    validityLengthMinutes = rawData.getBitsFromBuffer(144, 8) * 60 * 24,
    // 3 bits unknown
    transfers = listOf(rawData.getBitsFromBuffer(155, 5) * 5),
    lastTransportLeadingCode = rawData.getBitsFromBuffer(160, 2),
    lastTransportLongCode = rawData.getBitsFromBuffer(251, 2),
    // 4 bits unknown
    remainingTrips = rawData.getBitsFromBuffer(166, 10),
    lastValidator = rawData.getBitsFromBuffer(176, 16),
    // 30 bits unknown
    lastValidationTime = convertDateTime1992(
        rawData.getBitsFromBuffer(224, 16),
        rawData.getBitsFromBuffer(240, 11)
    )
    // 2 bits transport type
    // 3 bits unknown
    // missing: expiry
)

// This layout is found on some newer multi-ride passes
private class TroikaLayoutE2(
    rawData: ByteArray,
    private val transportCode: Int = rawData.getBitsFromBuffer(163, 2),
    validityLengthMins: Int = rawData.getBitsFromBuffer(131, 20),
    validityStartDays: Int = rawData.getBitsFromBuffer(97, 16)
) : TroikaBlock(
    rawData,
    expiryDate = convertDateTime1992(rawData.getBitsFromBuffer(71, 16), 0),
    validityLengthMinutes = validityLengthMins,
    transfers = listOf(rawData.getBitsFromBuffer(154, 8)),
    lastTransportRaw = transportCode.toString(16),
    remainingTrips = rawData.getBitsFromBuffer(167, 10),
    lastValidator = rawData.getBitsFromBuffer(177, 16),
    validityStart = convertDate1992(validityStartDays),
    validityEnd = convertDateTime1992(validityStartDays, validityLengthMins - 1),
    lastValidationTime = convertDateTime1992(
        validityStartDays,
        validityLengthMins - rawData.getBitsFromBuffer(196, 20)
    )
) {
    override fun getTransportType(getLast: Boolean): TroikaTransportType =
        when (transportCode) {
            0 -> TroikaTransportType.NONE
            1 -> TroikaTransportType.SUBWAY
            2 -> TroikaTransportType.MONORAIL
            3 -> TroikaTransportType.GROUND
            else -> TroikaTransportType.UNKNOWN
        }
}

// This is e-purse layout
private class TroikaPurseE3(private val rawData: ByteArray) : TroikaBlock(
    rawData,
    expiryDate = convertDateTime1992(rawData.getBitsFromBuffer(61, 16), 0),
    // 41 bits zero
    lastValidator = rawData.getBitsFromBuffer(128, 16),
    lastValidationTime = convertDateTime2016(0, rawData.getBitsFromBuffer(144, 23)),
    // 4 bits zero
    transfers = listOf(rawData.getBitsFromBuffer(171, 7)),
    lastTransportLeadingCode = rawData.getBitsFromBuffer(178, 2),
    lastTransportLongCode = rawData.getBitsFromBuffer(180, 8),
    fareDesc = runBlocking {
        when (rawData.getBitsFromBuffer(210, 2)) {
            1 -> getString(Res.string.troika_fare_single)
            2 -> getString(Res.string.troika_fare_90mins)
            else -> null
        }
    },
    // 12 bits zero
    checkSum = rawData.getHexString(28, 4)
) {
    /**
     * Balance of the card, in kopeyka (0.01 RUB).
     */
    private val purseBalance get() = rawData.getBitsFromBuffer(188, 22)

    override val balance: TransitBalance
        get() = TransitBalance(
            balance = TransitCurrency.RUB(purseBalance),
            name = NAME,
            validTo = expiryDate
        )

    override val subscription: Subscription?
        get() = null
}

// This is e-purse layout (newer, 2019 epoch)
private class TroikaPurseE5(private val rawData: ByteArray) : TroikaBlock(
    rawData,
    expiryDate = convertDate2019(rawData.getBitsFromBuffer(61, 13)),
    // 10 bits Ticket Type 2
    // 84-107: lastRefillTime
    // 107-117: refillCounter
    // 117-128: unknown (B)
    lastValidationTime = convertDateTime2019(0, rawData.getBitsFromBuffer(128, 23)),
    transfers = listOf(
        rawData.getBitsFromBuffer(151, 7),
        rawData.getBitsFromBuffer(158, 7)
    ),
    // 2 bits unknown
    // 19 bits balance
    lastValidator = rawData.getBitsFromBuffer(186, 16),
    // 202-216: unknown (D)
    // 216-223: tripsOnPurse
    // 224: unknown (E)
    lastTransportLeadingCode = null,
    lastTransportLongCode = null,
    fareDesc = null,
    checkSum = rawData.getHexString(28, 4)
) {
    /**
     * Balance of the card, in kopeyka (0.01 RUB).
     */
    private val purseBalance get() = rawData.getBitsFromBuffer(167, 19)

    override val balance: TransitBalance
        get() = TransitBalance(
            balance = TransitCurrency.RUB(purseBalance),
            name = NAME,
            validTo = expiryDate
        )

    override val subscription: Subscription?
        get() = null

    override val lastRefillTime: Instant?
        get() = convertDateTime2019(0, rawData.getBitsFromBuffer(84, 23))

    private val refillCounter get() = rawData.getBitsFromBuffer(107, 10)
    private val tripsOnPurse get() = rawData.getBitsFromBuffer(216, 7)

    override val info: List<ListItemInterface>
        get() = listOf(
            ListItem(Res.string.troika_refill_counter, refillCounter.toString()),
            ListItem(Res.string.troika_trips_on_purse, tripsOnPurse.toString())
        )
}

// Fallback for unknown layout types
private class TroikaUnknownBlock(rawData: ByteArray) : TroikaBlock(rawData) {
    override val info: List<ListItemInterface>
        get() = listOf(
            HeaderListItem(TroikaBlock.getHeader(ticketType)),
            ListItem(Res.string.troika_layout, layout.toString(16))
        )
}

// --- TroikaSubscription ---

private class TroikaSubscription(
    private val expiryDate: Instant?,
    override val validFrom: Instant?,
    private val validityEnd: Instant?,
    override val remainingTripCount: Int?,
    private val validityLengthMinutes: Int?,
    private val ticketType: Int
) : Subscription() {

    override val validTo: Instant?
        get() = validityEnd ?: expiryDate

    override val subscriptionName: String
        get() = TroikaBlock.getHeader(ticketType)

    override val agencyName: String get() = NAME
}

// --- TroikaTrip ---

private class TroikaTrip(
    override val startTimestamp: Instant?,
    private val transportType: TroikaTransportType?,
    private val validator: Int?,
    private val rawTransport: String?,
    private val fareDescription: String? = null
) : Trip() {

    companion object {
        private const val TROIKA_STR = "troika"
    }

    override val startStation: Station?
        get() {
            if (validator == null || validator == 0) return null
            val result = MdstStationLookup.getStation(TROIKA_STR, validator)
            if (result != null) {
                return Station.Builder()
                    .stationName(result.stationName)
                    .shortStationName(result.shortStationName)
                    .companyName(result.companyName)
                    .lineName(result.lineNames.firstOrNull())
                    .latitude(if (result.hasLocation) result.latitude.toString() else null)
                    .longitude(if (result.hasLocation) result.longitude.toString() else null)
                    .build()
            }
            return Station.nameOnly(validator.toString())
        }

    // Troika doesn't store monetary price of trip. Only a fare code.
    // Show the fare code description to the user.
    override val fare: TransitCurrency?
        get() = null

    override val fareString: String? get() = fareDescription

    override val mode: Mode
        get() = when (transportType) {
            null -> Mode.OTHER
            TroikaTransportType.NONE, TroikaTransportType.UNKNOWN -> Mode.OTHER
            TroikaTransportType.SUBWAY -> Mode.METRO
            TroikaTransportType.MONORAIL -> Mode.MONORAIL
            TroikaTransportType.GROUND -> Mode.BUS
            TroikaTransportType.MCC -> Mode.TRAIN
        }

    override val agencyName: String?
        get() = runBlocking {
            when (transportType) {
                TroikaTransportType.UNKNOWN -> getString(Res.string.unknown)
                null, TroikaTransportType.NONE -> rawTransport
                TroikaTransportType.SUBWAY -> getString(Res.string.moscow_metro)
                TroikaTransportType.MONORAIL -> getString(Res.string.moscow_monorail)
                TroikaTransportType.GROUND -> getString(Res.string.moscow_ground_transport)
                TroikaTransportType.MCC -> getString(Res.string.moscow_mcc)
            }
        }
}

// --- TroikaRefill (ticket machine trip) ---

private class TroikaRefill(
    override val startTimestamp: Instant?
) : Trip() {
    override val fare: TransitCurrency? get() = null
    override val mode: Mode get() = Mode.TICKET_MACHINE
}
