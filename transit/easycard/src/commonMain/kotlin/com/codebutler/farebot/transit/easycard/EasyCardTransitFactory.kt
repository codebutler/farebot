/*
 * EasyCardTransitFactory.kt
 *
 * Copyright 2017 Eric Butler <eric@codebutler.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
 *
 * Based on code from:
 * - http://www.fuzzysecurity.com/tutorials/rfid/4.html
 * - Farebot <https://codebutler.github.io/farebot/>
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

package com.codebutler.farebot.transit.easycard

import com.codebutler.farebot.base.mdst.MdstStationLookup
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.base.util.byteArrayToIntReversed
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import com.codebutler.farebot.transit.Trip
import farebot.transit.easycard.generated.resources.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import kotlin.time.Instant

class EasyCardTransitFactory(
    private val stringResource: StringResource,
) : TransitFactory<ClassicCard, EasyCardTransitInfo> {
    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    companion object {
        internal const val EASYCARD_STR = "easycard"

        // Taipei timezone
        private val TAIPEI_TZ = TimeZone.of("Asia/Taipei")

        // Magic bytes at sector 0, block 1 that identify EasyCard
        private val MAGIC =
            byteArrayOf(
                0x0e,
                0x14,
                0x00,
                0x01,
                0x07,
                0x02,
                0x08,
                0x03,
                0x09,
                0x04,
                0x08,
                0x10,
                0x00,
                0x00,
                0x00,
                0x00,
            )

        private val CARD_INFO =
            CardInfo(
                nameRes = Res.string.easycard_card_name,
                cardType = CardType.MifareClassic,
                region = TransitRegion.TAIWAN,
                locationRes = Res.string.easycard_card_location,
                keysRequired = true,
                extraNoteRes = Res.string.easycard_card_note,
                imageRes = Res.drawable.easycard,
                latitude = 25.0330f,
                longitude = 121.5654f,
                brandColor = 0xE63279,
                credits = listOf("b33f"),
                sampleDumpFile = "EasyCard.mfc",
            )

        /**
         * Parse an EasyCard timestamp to an Instant.
         * EasyCard stores timestamps as seconds since 1970-01-01 00:00:00 in Taipei local time,
         * not UTC. We interpret the raw value as a local datetime and convert to UTC.
         */
        internal fun parseTimestamp(ts: Long?): Instant? {
            ts ?: return null
            if (ts == 0L) return null
            val fakeUtc = Instant.fromEpochSeconds(ts)
            val localDateTime = fakeUtc.toLocalDateTime(TimeZone.UTC)
            return localDateTime.toInstant(TAIPEI_TZ)
        }

        /**
         * Look up a station by its ID.
         */
        fun lookupStation(stationId: Int): Station? {
            // Try MDST database first
            val result = MdstStationLookup.getStation(EASYCARD_STR, stationId)
            if (result != null) {
                return Station
                    .Builder()
                    .stationName(result.stationName)
                    .shortStationName(result.shortStationName)
                    .companyName(result.companyName)
                    .lineNames(result.lineNames)
                    .latitude(if (result.hasLocation) result.latitude.toString() else null)
                    .longitude(if (result.hasLocation) result.longitude.toString() else null)
                    .build()
            }
            // Fallback to hardcoded map
            val name = EasyCardStations[stationId]
            return if (name != null) Station.nameOnly(name) else null
        }

        /**
         * Look up a station name by its ID.
         */
        fun lookupStationName(stationId: Int): String? {
            val station = lookupStation(stationId)
            if (station != null) return station.stationName
            return EasyCardStations[stationId]
        }
    }

    override fun check(card: ClassicCard): Boolean {
        // Check magic bytes at sector 0, block 1
        val data =
            (card.getSector(0) as? DataClassicSector)?.getBlock(1)?.data
                ?: return false
        return data.contentEquals(MAGIC)
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity =
        TransitIdentity.create(stringResource.getString(Res.string.easycard_card_name), null)

    override fun parseInfo(card: ClassicCard): EasyCardTransitInfo {
        val balance = parseBalance(card)
        val trips = EasyCardTransaction.parseTrips(card)
        val topUp = EasyCardTopUp.parse(card)

        // Combine trips and top-up into a single list
        val allTrips: List<Trip> =
            if (topUp != null) {
                trips + listOf(topUp)
            } else {
                trips
            }

        return EasyCardTransitInfo(
            balanceValue = balance,
            tripList = allTrips,
        )
    }

    /**
     * Parse balance from sector 2, block 0.
     * Balance is a 4-byte little-endian integer.
     */
    private fun parseBalance(card: ClassicCard): Int {
        val data =
            (card.getSector(2) as? DataClassicSector)?.getBlock(0)?.data
                ?: return 0
        return data.byteArrayToIntReversed(0, 4)
    }
}
