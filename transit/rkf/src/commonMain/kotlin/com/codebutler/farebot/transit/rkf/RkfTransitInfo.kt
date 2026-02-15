/*
 * RkfTransitInfo.kt
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

package com.codebutler.farebot.transit.rkf

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.DateFormatStyle
import com.codebutler.farebot.base.util.HashUtils
import com.codebutler.farebot.base.util.Luhn
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.byteArrayToLongReversed
import com.codebutler.farebot.base.util.formatDate
import com.codebutler.farebot.base.util.getBitsFromBufferLeBits
import com.codebutler.farebot.base.util.sliceOffLen
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransactionTripLastPrice
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.TransitRegion
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.en1545.En1545Container
import com.codebutler.farebot.transit.en1545.En1545FixedInteger
import com.codebutler.farebot.transit.en1545.En1545Parsed
import com.codebutler.farebot.transit.en1545.En1545Parser
import com.codebutler.farebot.transit.en1545.En1545TransitData
import farebot.transit.rkf.generated.resources.Res
import farebot.transit.rkf.generated.resources.rkf_card_issuer
import farebot.transit.rkf.generated.resources.rkf_card_name_default
import farebot.transit.rkf.generated.resources.rkf_card_name_rejsekort
import farebot.transit.rkf.generated.resources.rkf_card_name_slaccess
import farebot.transit.rkf.generated.resources.rkf_card_name_vasttrafik
import farebot.transit.rkf.generated.resources.rkf_card_status
import farebot.transit.rkf.generated.resources.rkf_expiry_date
import farebot.transit.rkf.generated.resources.rkf_location_denmark
import farebot.transit.rkf.generated.resources.rkf_location_gothenburg
import farebot.transit.rkf.generated.resources.rkf_location_stockholm
import farebot.transit.rkf.generated.resources.rkf_status_action_pending
import farebot.transit.rkf.generated.resources.rkf_status_not_ok
import farebot.transit.rkf.generated.resources.rkf_status_ok
import farebot.transit.rkf.generated.resources.rkf_status_temp_disabled
import farebot.transit.rkf.generated.resources.rkf_unknown_format
import com.codebutler.farebot.base.util.FormattedString

// Record types
sealed class RkfRecord

data class RkfSimpleRecord(
    val raw: ByteArray,
) : RkfRecord()

data class RkfTctoRecord(
    val chunks: List<List<ByteArray>>,
) : RkfRecord()

// Serial number
data class RkfSerial(
    val mCompany: Int,
    val mCustomerNumber: Long,
    val mHwSerial: Long,
) {
    val formatted: String
        get() =
            when (mCompany) {
                RkfLookup.REJSEKORT -> {
                    val main = "30843" + NumberUtils.formatNumber(mCustomerNumber, " ", 1, 3, 3, 3)
                    main + " " + Luhn.calculateLuhn(main.replace(" ", ""))
                }
                RkfLookup.SLACCESS -> {
                    NumberUtils.formatNumber(mHwSerial, " ", 5, 5)
                }
                RkfLookup.VASTTRAFIK -> {
                    val main = NumberUtils.zeroPad(mHwSerial, 10)
                    val allDigits = "2401" + main + Luhn.calculateLuhn(main)
                    NumberUtils.groupString(allDigits, " ", 4, 4, 6)
                }
                else -> mHwSerial.toString()
            }
}

// Specification: https://github.com/mchro/RejsekortReader/tree/master/resekortsforeningen
class RkfTransitInfo internal constructor(
    private val mTcci: En1545Parsed,
    private val mTrips: List<Trip>,
    private val mBalances: List<RkfPurse>,
    private val mLookup: RkfLookup,
    private val mTccps: List<En1545Parsed>,
    private val mSerial: RkfSerial,
    private val mSubscriptions: List<RkfTicket>,
) : TransitInfo() {
    override val cardName: FormattedString get() =
        issuerMap[aid]?.let {
            FormattedString(it.nameRes)
        } ?: FormattedString(Res.string.rkf_card_name_default)

    private val aid
        get() = mTcci.getIntOrZero(En1545TransitData.ENV_APPLICATION_ISSUER_ID)

    override val serialNumber: String get() = mSerial.formatted

    override val trips: List<Trip> get() = mTrips

    // Filter out ghost purse on Rejsekort unless it was ever used (is it ever?)
    override val balances: List<TransitBalance> get() =
        mBalances
            .withIndex()
            .filter { (idx, bal) ->
                aid != RkfLookup.REJSEKORT ||
                    idx != 1 ||
                    bal.transactionNumber != 0
            }.map { (_, bal) -> bal.balance }

    override val subscriptions: List<Subscription> get() = mSubscriptions

    val issuer
        get() = mLookup.getAgencyName(mTcci.getIntOrZero(En1545TransitData.ENV_APPLICATION_ISSUER_ID), false)

    private val expiryDate
        get() = mTcci.getTimeStamp(En1545TransitData.ENV_APPLICATION_VALIDITY_END, mLookup.timeZone)

    val cardStatus: FormattedString
        get() =
            when (mTcci.getIntOrZero(STATUS)) {
                0x01 -> FormattedString(Res.string.rkf_status_ok)
                0x21 -> FormattedString(Res.string.rkf_status_action_pending)
                0x3f -> FormattedString(Res.string.rkf_status_temp_disabled)
                0x58 -> FormattedString(Res.string.rkf_status_not_ok)
                else ->
                    FormattedString(
                        Res.string.rkf_unknown_format,
                        NumberUtils.intToHex(mTcci.getIntOrZero(STATUS)),
                    )
            }

    private val expiryDateInfo: ListItem?
        get() {
            val date = expiryDate ?: return null
            return ListItem(Res.string.rkf_expiry_date, formatDate(date, DateFormatStyle.LONG))
        }

    override val info: List<ListItemInterface> get() =
        listOfNotNull(expiryDateInfo) +
            listOf(
                ListItem(Res.string.rkf_card_issuer, issuer),
                ListItem(Res.string.rkf_card_status, cardStatus),
            )

    companion object {
        val issuerMap =
            mapOf(
                RkfLookup.SLACCESS to
                    CardInfo(
                        nameRes = Res.string.rkf_card_name_slaccess,
                        locationRes = Res.string.rkf_location_stockholm,
                        cardType = CardType.MifareClassic,
                        keysRequired = true,
                        keyBundle = "slaccess",
                        region = TransitRegion.SWEDEN,
                        preview = true,
                        brandColor = 0x00ACEE,
                        credits = listOf("Metrodroid Project"),
                    ),
                RkfLookup.REJSEKORT to
                    CardInfo(
                        nameRes = Res.string.rkf_card_name_rejsekort,
                        locationRes = Res.string.rkf_location_denmark,
                        cardType = CardType.MifareClassic,
                        keysRequired = true,
                        keyBundle = "rejsekort",
                        region = TransitRegion.DENMARK,
                        preview = true,
                        brandColor = 0x4591C9,
                        credits = listOf("Metrodroid Project"),
                    ),
                RkfLookup.VASTTRAFIK to
                    CardInfo(
                        nameRes = Res.string.rkf_card_name_vasttrafik,
                        locationRes = Res.string.rkf_location_gothenburg,
                        cardType = CardType.MifareClassic,
                        keysRequired = true,
                        keyBundle = "gothenburg",
                        region = TransitRegion.SWEDEN,
                        preview = true,
                        brandColor = 0x88C797,
                        credits = listOf("Metrodroid Project"),
                    ),
            )

        internal fun clearSeconds(timeInMillis: Long) = timeInMillis / 60000 * 60000

        internal fun getRecords(card: ClassicCard): List<RkfRecord> {
            val records = mutableListOf<RkfRecord>()
            var sector = 3
            var block = 0

            while (sector < card.sectors.size) {
                val curSector = card.sectors[sector]
                if (curSector !is DataClassicSector) {
                    sector++
                    block = 0
                    continue
                }
                // FIXME: we should also check TCDI entry but TCDI doesn't match the spec apparently,
                // so for now just use id byte
                val type = curSector.getBlock(block).data.getBitsFromBufferLeBits(0, 8)
                if (type == 0) {
                    sector++
                    block = 0
                    continue
                }
                var first = true
                val oldSector = sector
                var oldBlockCount = -1

                while (sector < card.sectors.size && (first || block != 0)) {
                    first = false
                    val sectorData = card.sectors[sector] as? DataClassicSector ?: break
                    val blockData = sectorData.getBlock(block).data
                    val newType = blockData.getBitsFromBufferLeBits(0, 8)
                    // Some Rejsekort skip slot in the middle of the sector
                    if (newType == 0 && block + oldBlockCount < sectorData.blocks.size - 1) {
                        block += oldBlockCount
                        continue
                    }
                    if (newType != type) {
                        break
                    }
                    val version = blockData.getBitsFromBufferLeBits(8, 6)
                    if (type in 0x86..0x87) {
                        val chunks = mutableListOf<List<ByteArray>>()
                        while (true) {
                            val sd = card.sectors[sector] as? DataClassicSector ?: break
                            if (sd.getBlock(block).data[0] == 0.toByte() && block < sd.blocks.size - 2) {
                                block++
                                continue
                            }
                            if (sd.getBlock(block).data[0].toInt() and 0xff !in 0x86..0x88) {
                                break
                            }
                            var ptr = 0
                            val tags = mutableListOf<ByteArray>()
                            while (true) {
                                val sd2 = card.sectors[sector] as? DataClassicSector ?: break
                                val subType = sd2.getBlock(block).data[ptr].toInt() and 0xff
                                var l = getTccoTagSize(subType, version)
                                if (l == -1) {
                                    break
                                }
                                var tag = ByteArray(0)
                                while (l > 0) {
                                    if (ptr == 16) {
                                        ptr = 0
                                        block++
                                        val sd3 = card.sectors[sector] as? DataClassicSector ?: break
                                        if (block >= sd3.blocks.size - 1) {
                                            sector++
                                            block = 0
                                        }
                                    }
                                    val sd3 = card.sectors[sector] as? DataClassicSector ?: break
                                    val c = minOf(16 - ptr, l)
                                    tag += sd3.getBlock(block).data.sliceOffLen(ptr, c)
                                    l -= c
                                    ptr += c
                                }
                                tags += tag
                            }
                            chunks += listOf(tags)
                            if (ptr != 0) {
                                block++
                                val sd2 = card.sectors[sector] as? DataClassicSector ?: break
                                if (block >= sd2.blocks.size - 1) {
                                    sector++
                                    block = 0
                                }
                            }
                        }
                        records.add(RkfTctoRecord(chunks))
                    } else {
                        val blockCount = getBlockCount(type, version)
                        if (blockCount == -1) {
                            break
                        }
                        oldBlockCount = blockCount
                        var dat = ByteArray(0)

                        repeat(blockCount) {
                            val sd = card.sectors[sector] as? DataClassicSector ?: return@repeat
                            dat += sd.getBlock(block).data
                            block++
                            if (block >= sd.blocks.size - 1) {
                                sector++
                                block = 0
                            }
                        }

                        records.add(RkfSimpleRecord(dat))
                    }
                }
                if (block != 0 || sector == oldSector) {
                    sector++
                    block = 0
                }
            }
            return records
        }

        private fun getTccoTagSize(
            type: Int,
            version: Int,
        ) = when (type) {
            0x86 -> 2
            0x87 ->
                when (version) {
                    1, 2 -> 2
                    else -> 17 // No idea how it's actually supposed to be parsed but this works
                }
            0x88 -> 3 // tested: version 3
            0x89 -> 11 // tested: version 3
            0x8a -> 1
            0x93 -> 4
            0x94 -> 4
            0x95 -> 2
            0x96 ->
                when (version) {
                    1, 2 -> 15
                    else -> 21 // tested: 3
                }
            0x97 -> 18
            0x98 -> 4
            0x99 -> 5
            0x9a -> 7
            0x9c -> 7 // tested: version 3
            0x9d -> 9
            0x9e -> 5
            0x9f -> 2
            else -> -1
        }

        private fun getBlockCount(
            type: Int,
            version: Int,
        ) = when (type) {
            0x84 -> 1
            0x85 ->
                when (version) {
                    // Only 3 is tested
                    1, 2, 3, 4, 5 -> 3
                    else -> 6
                }
            0xa2 -> 2
            0xa3 ->
                when (version) {
                    // Only 2 is tested
                    1, 2 -> 3
                    // Only 5 is tested
                    // 3 seems already have size 6
                    else -> 6
                }
            else -> -1
        }

        internal fun getSerial(card: ClassicCard): RkfSerial {
            val issuer = getIssuer(card)

            val hwSerial =
                card.sectors[0].let { sector ->
                    (sector as? DataClassicSector)?.getBlock(0)?.data?.byteArrayToLongReversed(0, 4) ?: 0L
                }

            for (record in getRecords(card).filterIsInstance<RkfSimpleRecord>()) {
                if ((record.raw[0].toInt() and 0xff) == 0xa2) {
                    val low = record.raw.getBitsFromBufferLeBits(34, 20).toLong()
                    val high = record.raw.getBitsFromBufferLeBits(54, 14).toLong()
                    return RkfSerial(mCompany = issuer, mHwSerial = hwSerial, mCustomerNumber = (high shl 20) or low)
                }
            }
            return RkfSerial(mCompany = issuer, mHwSerial = hwSerial, mCustomerNumber = 0)
        }

        private fun getIssuer(card: ClassicCard): Int {
            val sector0 = card.sectors[0] as? DataClassicSector ?: return 0
            return sector0.getBlock(1).data.getBitsFromBufferLeBits(22, 12)
        }

        internal const val COMPANY = "Company"
        internal const val STATUS = "Status"
        internal val ID_FIELD = En1545FixedInteger("Identifier", 8)
        internal val VERSION_FIELD = En1545FixedInteger("Version", 6)
        internal val HEADER =
            En1545Container(
                ID_FIELD,
                VERSION_FIELD,
                En1545FixedInteger(COMPANY, 12),
            )

        internal val STATUS_FIELD = En1545FixedInteger(STATUS, 8)
        internal val MAC =
            En1545Container(
                En1545FixedInteger("MACAlgorithmIdentifier", 2),
                En1545FixedInteger("MACKeyIdentifier", 6),
                En1545FixedInteger("MACAuthenticator", 16),
            )

        internal const val CURRENCY = "CardCurrencyUnit"
        internal const val EVENT_LOG_VERSION = "EventLogVersionNumber"
        internal val TCCI_FIELDS =
            En1545Container(
                En1545FixedInteger("MADindicator", 16),
                En1545FixedInteger("CardVersion", 6),
                En1545FixedInteger(En1545TransitData.ENV_APPLICATION_ISSUER_ID, 12),
                En1545FixedInteger.date(En1545TransitData.ENV_APPLICATION_VALIDITY_END),
                STATUS_FIELD,
                En1545FixedInteger(CURRENCY, 16),
                En1545FixedInteger(EVENT_LOG_VERSION, 6),
                En1545FixedInteger("A", 26),
                MAC,
            )
        internal val TCCP_FIELDS =
            En1545Container(
                HEADER,
                STATUS_FIELD,
                En1545Container(
                    // This is actually a single field. Split is only
                    // because of limitations of parser
                    En1545FixedInteger("CustomerNumberLow", 20),
                    En1545FixedInteger("CustomerNumberHigh", 14),
                ),
                // Rest unknown
            )
    }
}

/**
 * Transit factory for RKF-based MIFARE Classic cards.
 */
class RkfTransitFactory : TransitFactory<ClassicCard, RkfTransitInfo> {
    override val allCards: List<CardInfo>
        get() = RkfTransitInfo.issuerMap.values.toList()

    override fun check(card: ClassicCard): Boolean {
        if (card.sectors.isEmpty()) return false
        val sector0 = card.sectors[0] as? DataClassicSector ?: return false
        return HashUtils.checkKeyHash(
            sector0.keyA,
            sector0.keyB,
            "rkf",
            // Most cards
            "b9ae9b2f6855aa199b4af7bdc130ba1c",
            "2107bb612627fb1dfe57348fea8a8b58",
            // Jo-jo
            "f40bb9394d94c7040c1dd19997b4f5e8",
        ) >= 0
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity {
        val serial = RkfTransitInfo.getSerial(card)
        val issuerName =
            RkfTransitInfo.issuerMap[serial.mCompany]?.let {
                FormattedString(it.nameRes)
            } ?: FormattedString(Res.string.rkf_card_name_default)
        return TransitIdentity(issuerName, serial.formatted)
    }

    override fun parseInfo(card: ClassicCard): RkfTransitInfo {
        val sector0 = card.sectors[0] as DataClassicSector
        val tcciRaw = sector0.getBlock(1).data
        val tcci = En1545Parser.parseLeBits(tcciRaw, 0, RkfTransitInfo.TCCI_FIELDS)
        val tripVersion = tcci.getIntOrZero(RkfTransitInfo.EVENT_LOG_VERSION)
        val currency = tcci.getIntOrZero(RkfTransitInfo.CURRENCY)
        val company = tcci.getIntOrZero(En1545TransitData.ENV_APPLICATION_ISSUER_ID)
        val lookup = RkfLookup(currency, company)
        val transactions = mutableListOf<RkfTransaction>()
        val balances = mutableListOf<RkfPurse>()
        val tccps = mutableListOf<En1545Parsed>()
        val unfilteredTrips = mutableListOf<RkfTCSTTrip>()
        val records = RkfTransitInfo.getRecords(card)
        recordloop@ for (record in records.filterIsInstance<RkfSimpleRecord>()) {
            when (record.raw[0].toInt() and 0xff) {
                0x84 ->
                    transactions +=
                        RkfTransaction.parseTransaction(record.raw, lookup, tripVersion) ?: continue@recordloop
                0x85 -> balances += RkfPurse.parse(record.raw, lookup)
                0xa2 -> tccps += En1545Parser.parseLeBits(record.raw, RkfTransitInfo.TCCP_FIELDS)
                0xa3 -> unfilteredTrips += RkfTCSTTrip.parse(record.raw, lookup) ?: continue@recordloop
            }
        }
        val tickets = records.filterIsInstance<RkfTctoRecord>().map { RkfTicket.parse(it, lookup) }
        transactions.sortBy { it.timestamp?.toEpochMilliseconds() }
        unfilteredTrips.sortBy { it.startTimestamp?.toEpochMilliseconds() }
        val trips = mutableListOf<RkfTCSTTrip>()
        // Check if unfinished trip is superseeded by finished one
        for ((idx, trip) in unfilteredTrips.withIndex()) {
            if (idx > 0 &&
                unfilteredTrips[idx - 1].startTimestamp?.toEpochMilliseconds() ==
                trip.startTimestamp?.toEpochMilliseconds() &&
                unfilteredTrips[idx - 1].checkoutCompleted &&
                !trip.checkoutCompleted
            ) {
                continue
            }
            if (idx < unfilteredTrips.size - 1 &&
                unfilteredTrips[idx + 1].startTimestamp?.toEpochMilliseconds() ==
                trip.startTimestamp?.toEpochMilliseconds() &&
                unfilteredTrips[idx + 1].checkoutCompleted &&
                !trip.checkoutCompleted
            ) {
                continue
            }
            trips.add(trip)
        }
        val nonTripTransactions = transactions.filter { it.isOther() }
        val tripTransactions = transactions.filter { !it.isOther() }
        val remainingTransactions = mutableListOf<RkfTransaction>()
        var i = 0
        for (trip in trips) {
            while (i < tripTransactions.size) {
                val transaction = tripTransactions[i]
                val transactionTimestamp =
                    RkfTransitInfo.clearSeconds(
                        transaction.timestamp?.toEpochMilliseconds() ?: 0,
                    )
                if (transactionTimestamp > RkfTransitInfo.clearSeconds(trip.endTimestamp?.toEpochMilliseconds() ?: 0)) {
                    break
                }
                i++
                if (transactionTimestamp <
                    RkfTransitInfo.clearSeconds(trip.startTimestamp?.toEpochMilliseconds() ?: 0)
                ) {
                    remainingTransactions.add(transaction)
                    continue
                }
                trip.addTransaction(transaction)
            }
        }
        if (i < tripTransactions.size) {
            remainingTransactions.addAll(tripTransactions.subList(i, tripTransactions.size))
        }
        return RkfTransitInfo(
            mTcci = tcci,
            mTrips =
                TransactionTripLastPrice.merge(nonTripTransactions + remainingTransactions) +
                    trips.map { it.tripLegs }.flatten(),
            mBalances = balances,
            mLookup = lookup,
            mTccps = tccps,
            mSerial = RkfTransitInfo.getSerial(card),
            mSubscriptions = tickets,
        )
    }
}
