/*
 * SmartRiderTransitFactory.kt
 *
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.smartrider

import com.codebutler.farebot.base.util.HashUtils
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.base.util.byteArrayToIntReversed
import com.codebutler.farebot.base.util.getHexString
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.TransactionTrip
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity

/**
 * Transit factory for SmartRider (Perth, Western Australia) and MyWay (Canberra, ACT).
 *
 * These cards are MIFARE Classic cards identified by checking salted MD5 hashes of
 * the keys on sector 7.
 *
 * https://github.com/micolous/metrodroid/wiki/SmartRider
 * https://github.com/micolous/metrodroid/wiki/MyWay
 */
class SmartRiderTransitFactory(
    private val stringResource: StringResource,
) : TransitFactory<ClassicCard, SmartRiderTransitInfo> {

    companion object {
        // Unfortunately, there's no way to reliably identify these cards except for the
        // "standard" keys which are used for some empty sectors. It is not enough to read
        // the whole card (most data is protected by a unique key).
        //
        // We don't want to actually include these keys in the program, so include a hashed
        // version of this key.
        private const val MYWAY_KEY_SALT = "myway"

        // md5sum of Salt + Common Key 2 + Salt, used on sector 7 key A and B.
        private const val MYWAY_KEY_DIGEST = "29a61b3a4d5c818415350804c82cd834"

        private const val SMARTRIDER_KEY_SALT = "smartrider"

        // md5sum of Salt + Common Key 2 + Salt, used on Sector 7 key A.
        private const val SMARTRIDER_KEY2_DIGEST = "e0913518a5008c03e1b3f2bb3a43ff78"

        // md5sum of Salt + Common Key 3 + Salt, used on Sector 7 key B.
        private const val SMARTRIDER_KEY3_DIGEST = "bc510c0183d2c0316533436038679620"

        fun detectKeyType(card: ClassicCard): SmartRiderType {
            try {
                val sector = card.sectors.getOrNull(7) ?: return SmartRiderType.UNKNOWN
                if (sector !is DataClassicSector) return SmartRiderType.UNKNOWN

                // Check for MyWay key
                if (HashUtils.checkKeyHash(
                        sector.keyA, sector.keyB,
                        MYWAY_KEY_SALT, MYWAY_KEY_DIGEST
                    ) >= 0
                ) {
                    return SmartRiderType.MYWAY
                }

                // Check for SmartRider key
                if (HashUtils.checkKeyHash(
                        sector.keyA, sector.keyB,
                        SMARTRIDER_KEY_SALT,
                        SMARTRIDER_KEY2_DIGEST, SMARTRIDER_KEY3_DIGEST
                    ) >= 0
                ) {
                    return SmartRiderType.SMARTRIDER
                }
            } catch (_: IndexOutOfBoundsException) {
                // If that sector number is too high, then it's not for us.
            }

            return SmartRiderType.UNKNOWN
        }

        private fun getSerialData(card: ClassicCard): String {
            val sector0 = card.getSector(0)
            if (sector0 !is DataClassicSector) return ""
            val serialData = sector0.getBlock(1).data
            var serial = serialData.getHexString(6, 5)
            if (serial.startsWith("0")) {
                serial = serial.substring(1)
            }
            return serial
        }
    }

    override fun check(card: ClassicCard): Boolean {
        return detectKeyType(card) != SmartRiderType.UNKNOWN
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity {
        val type = detectKeyType(card)
        return TransitIdentity.create(stringResource.getString(type.friendlyName), getSerialData(card))
    }

    override fun parseInfo(card: ClassicCard): SmartRiderTransitInfo {
        val cardType = detectKeyType(card)
        val serialNumber = getSerialData(card)

        // Read configuration from sector 1
        val sector1 = card.getSector(1) as DataClassicSector
        val config = sector1.readBlocks(0, 3)
        val issueDate = config.byteArrayToIntReversed(16, 2)
        val tokenExpiryDate = config.byteArrayToIntReversed(18, 2)
        // SmartRider only
        val autoloadThreshold = config.byteArrayToIntReversed(20, 2)
        // SmartRider only
        val autoloadValue = config.byteArrayToIntReversed(22, 2)
        val tokenType = config[24].toInt()

        // Balance records from sectors 2 and 3
        val balanceA = SmartRiderBalanceRecord(cardType, card.getSector(2) as DataClassicSector, stringResource)
        val balanceB = SmartRiderBalanceRecord(cardType, card.getSector(3) as DataClassicSector, stringResource)
        val sortedBalances = listOf(balanceA, balanceB).sortedByDescending { it.transactionNumber }
        val balance = sortedBalances[0].balance

        // Read trips from sectors 10-13 (3 data blocks each, excluding trailer)
        val tagRecords = (10..13).flatMap { s ->
            val sector = card.getSector(s)
            if (sector !is DataClassicSector) return@flatMap emptyList()
            (0..2).map { b -> sector.getBlock(b).data }
        }.map { blockData ->
            SmartRiderTagRecord.parse(cardType, blockData, stringResource)
        }.filter { it.isValid }.map { record ->
            // Check the Balances for a recent transaction with more data.
            for (b in sortedBalances) {
                if (b.recentTagOn.isValid && b.recentTagOn.mTimestamp == record.mTimestamp) {
                    return@map record.enrichWithRecentData(b.recentTagOn)
                }
                if (b.firstTagOn.isValid && b.firstTagOn.mTimestamp == record.mTimestamp) {
                    return@map record.enrichWithRecentData(b.firstTagOn)
                }
            }
            // There was no extra data available.
            record
        }

        // Build the Tag events into trips.
        val trips = TransactionTrip.merge(tagRecords)

        return SmartRiderTransitInfo(
            serialNumberValue = serialNumber,
            mBalance = balance,
            trips = trips,
            mSmartRiderType = cardType,
            mIssueDate = issueDate,
            mTokenType = tokenType,
            mTokenExpiryDate = tokenExpiryDate,
            mAutoloadThreshold = autoloadThreshold,
            mAutoloadValue = autoloadValue,
            stringResource = stringResource,
        )
    }
}
