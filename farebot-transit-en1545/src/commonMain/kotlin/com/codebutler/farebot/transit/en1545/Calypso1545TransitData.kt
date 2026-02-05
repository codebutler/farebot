/*
 * Calypso1545TransitData.kt
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

import com.codebutler.farebot.card.iso7816.ISO7816Application
import com.codebutler.farebot.card.iso7816.ISO7816File
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransactionTrip
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip

typealias SubCreator = (data: ByteArray, counter: Int?, contractList: En1545Parsed?, listNum: Int?) -> En1545Subscription?

typealias TripCreator = (data: ByteArray) -> En1545Transaction?

/**
 * Parsed result from a Calypso card.
 */
data class CalypsoParseResult(
    val ticketEnv: En1545Parsed,
    val trips: List<Trip>,
    val subscriptions: List<Subscription>,
    val balances: List<TransitCurrency>,
    val serial: String?,
    val contractList: En1545Parsed?
)

/**
 * Helpers for parsing Calypso / EN1545 transit cards backed by ISO 7816 applications.
 * Individual transit system implementations extend En1545TransitData and use these helpers
 * to extract trips, subscriptions, and balances from the card data.
 */
object Calypso1545TransitData {

    fun getSfiFile(app: ISO7816Application, sfi: Int): ISO7816File? {
        return app.sfiFiles[sfi]
    }

    fun getSfiRecords(app: ISO7816Application, sfi: Int): List<ByteArray> {
        val file = getSfiFile(app, sfi) ?: return emptyList()
        return file.records.entries.sortedBy { it.key }.map { it.value }
    }

    fun parseTicketEnv(
        app: ISO7816Application,
        ticketEnvFields: En1545Container
    ): En1545Parsed {
        val records = getSfiRecords(app, CalypsoConstants.SFI_TICKETING_ENVIRONMENT)
        val combined = records.fold(byteArrayOf()) { acc, bytes -> acc + bytes }
        return if (combined.isEmpty()) En1545Parsed() else En1545Parser.parse(combined, ticketEnvFields)
    }

    fun parseTrips(
        app: ISO7816Application,
        createTrip: TripCreator,
        createSpecialEvent: TripCreator? = null
    ): List<Trip> {
        val transactions = getSfiRecords(app, CalypsoConstants.SFI_TICKETING_LOG)
            .filter { !it.isAllZero() }
            .mapNotNull { createTrip(it) }

        val specialEvents = if (createSpecialEvent != null) {
            getSfiRecords(app, CalypsoConstants.SFI_TICKETING_SPECIAL_EVENTS)
                .filter { !it.isAllZero() }
                .mapNotNull { createSpecialEvent(it) }
        } else {
            emptyList()
        }

        return TransactionTrip.merge(transactions + specialEvents)
    }

    fun getContracts(app: ISO7816Application): List<ByteArray> {
        return listOf(
            CalypsoConstants.SFI_TICKETING_CONTRACTS_1,
            CalypsoConstants.SFI_TICKETING_CONTRACTS_2
        ).flatMap { sfi -> getSfiRecords(app, sfi) }
    }

    fun getCounter(app: ISO7816Application, recordNum: Int): Int? {
        if (recordNum < 1 || recordNum > 4) return null

        // Try shared counter first (SFI 0x19)
        val sharedFile = getSfiFile(app, CalypsoConstants.SFI_TICKETING_COUNTERS_9)
        if (sharedFile != null) {
            val record = sharedFile.records[1]
            if (record != null && record.size >= 3 * recordNum) {
                val offset = 3 * (recordNum - 1)
                return byteArrayToInt(record, offset, 3)
            }
        }

        // Try individual counter
        val counterSfi = CalypsoConstants.getCounterSfi(recordNum) ?: return null
        val counterFile = getSfiFile(app, counterSfi)
        val record = counterFile?.records?.get(1) ?: return null
        if (record.size >= 3) {
            return byteArrayToInt(record, 0, 3)
        }
        return null
    }

    fun parseContracts(
        app: ISO7816Application,
        contractListFields: En1545Field?,
        createSubscription: SubCreator,
        contracts: List<ByteArray> = getContracts(app)
    ): Triple<List<Subscription>, List<TransitCurrency>, En1545Parsed?> {
        val subscriptions = mutableListOf<En1545Subscription>()
        val balances = mutableListOf<TransitCurrency>()
        val parsed = mutableSetOf<Int>()
        val contractList: En1545Parsed?

        if (contractListFields != null) {
            val contractListRecord = getSfiFile(app, CalypsoConstants.SFI_TICKETING_CONTRACT_LIST)
                ?.records?.get(1) ?: ByteArray(0)
            contractList = if (contractListRecord.isNotEmpty()) {
                En1545Parser.parse(contractListRecord, contractListFields)
            } else {
                En1545Parsed()
            }

            for (i in 0..15) {
                val ptr = contractList.getInt(En1545TransitData.CONTRACTS_POINTER, i) ?: continue
                if (ptr == 0) continue
                parsed.add(ptr)
                if (ptr > contracts.size) continue
                val recordData = contracts[ptr - 1]
                val sub = createSubscription(recordData, getCounter(app, ptr), contractList, i)
                if (sub != null) {
                    val cost = sub.cost
                    if (cost != null) balances.add(cost)
                    else subscriptions.add(sub)
                }
            }
        } else {
            contractList = null
        }

        for ((idx, record) in contracts.withIndex()) {
            if (record.isAllZero()) continue
            if (parsed.contains(idx)) continue
            val sub = createSubscription(record, null, null, null)
            if (sub != null) {
                val cost = sub.cost
                if (cost != null) balances.add(cost)
                else subscriptions.add(sub)
            }
        }

        return Triple(subscriptions, balances, contractList)
    }

    fun parse(
        app: ISO7816Application,
        ticketEnvFields: En1545Container,
        contractListFields: En1545Field?,
        serial: String?,
        createSubscription: SubCreator,
        createTrip: TripCreator,
        createSpecialEvent: TripCreator? = null,
        contracts: List<ByteArray> = getContracts(app)
    ): CalypsoParseResult {
        val ticketEnv = parseTicketEnv(app, ticketEnvFields)
        val (subscriptions, balances, contractList) = parseContracts(
            app, contractListFields, createSubscription, contracts
        )
        val trips = parseTrips(app, createTrip, createSpecialEvent)
        return CalypsoParseResult(
            ticketEnv = ticketEnv,
            trips = trips,
            subscriptions = subscriptions,
            balances = balances,
            serial = serial,
            contractList = contractList
        )
    }

    private fun byteArrayToInt(data: ByteArray, offset: Int, length: Int): Int {
        var result = 0
        for (i in 0 until length) {
            result = (result shl 8) or (data[offset + i].toInt() and 0xFF)
        }
        return result
    }

    private fun ByteArray.isAllZero(): Boolean = all { it == 0.toByte() }
}

private fun ByteArray.isAllZero(): Boolean = all { it == 0.toByte() }
