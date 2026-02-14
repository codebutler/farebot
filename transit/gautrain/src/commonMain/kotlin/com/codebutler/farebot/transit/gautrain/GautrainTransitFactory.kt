/*
 * GautrainTransitFactory.kt
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

package com.codebutler.farebot.transit.gautrain

import com.codebutler.farebot.base.util.DefaultStringResource
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.base.util.byteArrayToLong
import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransactionTripLastPrice
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import farebot.transit.gautrain.generated.resources.*

/**
 * Transit factory for Gautrain (Gauteng, South Africa).
 *
 * The Gautrain card is a MIFARE Classic card using an OVChip-derived data format
 * with EN1545 field encoding for transactions and subscriptions.
 */
class GautrainTransitFactory(
    private val stringResource: StringResource = DefaultStringResource(),
) : TransitFactory<ClassicCard, GautrainTransitInfo> {
    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override fun check(card: ClassicCard): Boolean {
        val sector0 = card.getSector(0) as? DataClassicSector ?: return false
        if (sector0.blocks.size < 2) return false
        val block1 = sector0.readBlocks(1, 1)
        if (block1.size < MAGIC_HEADER.size) return false
        return block1.copyOfRange(0, MAGIC_HEADER.size).contentEquals(MAGIC_HEADER)
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity =
        TransitIdentity.create(
            getStringBlocking(Res.string.gautrain_card_name),
            GautrainTransitInfo.formatSerial(getSerial(card)),
        )

    override fun parseInfo(card: ClassicCard): GautrainTransitInfo {
        val sector39 = card.getSector(39) as DataClassicSector

        // Parse OVChip index from sector 39 blocks 11-14
        val indexData = sector39.readBlocks(11, 4)
        val index = GautrainIndex.parse(indexData)

        // Parse transactions from sectors 35-38 (7 transactions per sector, 2 blocks each)
        val transactions =
            (35..38).flatMap { sectorIdx ->
                val sector = card.getSector(sectorIdx) as? DataClassicSector ?: return@flatMap emptyList()
                (0..12 step 2).mapNotNull { block ->
                    val data = sector.readBlocks(block, 2)
                    GautrainTransaction.parse(data)
                }
            }
        val trips = TransactionTripLastPrice.merge(transactions)

        // Parse subscriptions using OVChip index
        val subIndexData = sector39.readBlocks(if (index.recentSubscriptionSlot) 3 else 1, 2)
        val subCount = subIndexData.getBitsFromBuffer(0, 4)
        val subscriptions =
            (0 until subCount).map { i ->
                val bits = subIndexData.getBitsFromBuffer(4 + i * 21, 21)
                val type1 = NumberUtils.getBitsFromInteger(bits, 13, 8)
                val used = NumberUtils.getBitsFromInteger(bits, 6, 1)
                val subscriptionIndexId = NumberUtils.getBitsFromInteger(bits, 0, 4)
                val subscriptionAddress = index.subscriptionIndex[subscriptionIndexId - 1]
                val subSector = card.getSector(32 + subscriptionAddress / 5) as DataClassicSector
                val subData = subSector.readBlocks(subscriptionAddress % 5 * 3, 3)
                GautrainSubscription.parse(subData, stringResource, type1, used)
            }

        // Parse balance blocks from sector 39 blocks 9-10
        val balances =
            listOf(
                sector39.getBlock(9).data,
                sector39.getBlock(10).data,
            ).map { GautrainBalanceBlock.parse(it) }

        // Parse expiry date from sector 0 block 1
        val expdate = (card.getSector(0) as DataClassicSector).getBlock(1).data.getBitsFromBuffer(88, 20)

        return GautrainTransitInfo(
            serial = getSerial(card),
            trips = trips,
            subscriptions = subscriptions,
            expdate = expdate,
            mBalanceBlocks = balances,
        )
    }

    companion object {
        private val CARD_INFO =
            CardInfo(
                nameRes = Res.string.gautrain_card_name,
                cardType = CardType.MifareClassic,
                region = TransitRegion.SOUTH_AFRICA,
                locationRes = Res.string.transit_gautrain_location,
                imageRes = Res.drawable.gautrain,
                latitude = -26.2041f,
                longitude = 28.0473f,
                brandColor = 0xA2813C,
                credits = listOf("Metrodroid Project"),
            )

        private val MAGIC_HEADER =
            byteArrayOf(
                0xb1.toByte(),
                0x80.toByte(),
                0x00,
                0x00,
                0x06,
                0xb5.toByte(),
                0x5c,
                0x00,
                0x13,
                0xae.toByte(),
                0xe4.toByte(),
            )

        private fun getSerial(card: ClassicCard): Long =
            (card.getSector(0) as DataClassicSector).getBlock(0).data.byteArrayToLong(0, 4)
    }
}
