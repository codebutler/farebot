/*
 * OVChipParser.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2012, 2014-2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.ovc

import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.ClassicUtils
import com.codebutler.farebot.base.util.ByteUtils
import com.codebutler.farebot.card.classic.DataClassicSector

internal class OVChipParser(
    private val mCard: ClassicCard,
    private val mIndex: OVChipIndex
) {

    fun getPreamble(): OVChipPreamble {
        val data = (mCard.getSector(0) as DataClassicSector).readBlocks(0, 3)
        return OVChipPreamble.create(data)
    }

    private fun readBlocksAtBytePointer(bytePointer: Int, blockCount: Int): ByteArray {
        var blockIndex = ClassicUtils.convertBytePointerToBlock(bytePointer)
        val sector = ClassicUtils.blockToSector(blockIndex)
        val firstBlock = ClassicUtils.sectorToBlock(sector)
        blockIndex -= firstBlock
        return (mCard.getSector(sector) as DataClassicSector).readBlocks(blockIndex, blockCount)
    }

    fun getInfo(): OVChipInfo {
        val data = readBlocksAtBytePointer(mIndex.recentInfoSlot, 3)
        return OVChipInfo.create(data)
    }

    fun getCredit(): OVChipCredit {
        return OVChipCredit.create(readBlocksAtBytePointer(mIndex.recentCreditSlot, 1))
    }

    fun getTransactions(): Array<OVChipTransaction> {
        return Array(28) { transactionId ->
            OVChipTransaction.create(transactionId, readTransaction(transactionId))
        }
    }

    private fun readTransaction(transactionId: Int): ByteArray {
        val blockIndex = (transactionId % 7) * 2
        return when {
            transactionId <= 6 ->
                (mCard.getSector(35) as DataClassicSector).readBlocks(blockIndex, 2)
            transactionId in 7..13 ->
                (mCard.getSector(36) as DataClassicSector).readBlocks(blockIndex, 2)
            transactionId in 14..20 ->
                (mCard.getSector(37) as DataClassicSector).readBlocks(blockIndex, 2)
            transactionId in 21..27 ->
                (mCard.getSector(38) as DataClassicSector).readBlocks(blockIndex, 2)
            else ->
                throw IllegalArgumentException("Invalid transactionId: $transactionId")
        }
    }

    fun getSubscriptions(): Array<OVChipSubscription> {
        val data = readSubscriptionIndexSlot(mIndex.recentSubscriptionSlot)

        /*
        * TODO / FIXME
        * The card can store 15 subscriptions and stores pointers to some extra information
        * regarding these subscriptions. The problem is, it only stores 12 of these pointers.
        * In the code used here we get the subscriptions according to these pointers,
        * but this means that we could miss a few subscriptions.
        *
        * We could get the last few by looking at what has already been collected and get the
        * rest ourself, but they will lack the extra information because it simply isn't
        * there.
        *
        * Or rewrite this and just get all the subscriptions and discard the ones that are
        * invalid. Afterwards we can get the extra information if it's available.
        *
        * For more info see:
        * Dutch:   http://ov-chipkaart.pc-active.nl/Indexen
        * English: http://ov-chipkaart.pc-active.nl/Indexes
        */
        val count = ByteUtils.getBitsFromBuffer(data, 0, 4)
        val subscriptions = Array(count) { i ->
            val bits = ByteUtils.getBitsFromBuffer(data, 4 + (i * 21), 21)

            /* Based on info from ovc-tools by ocsr ( https://github.com/ocsrunl/ ) */
            val type1 = ByteUtils.getBitsFromInteger(bits, 13, 8)
            val type2 = ByteUtils.getBitsFromInteger(bits, 7, 6)
            val used = ByteUtils.getBitsFromInteger(bits, 6, 1)
            val rest = ByteUtils.getBitsFromInteger(bits, 4, 2)
            val subscriptionIndexId = ByteUtils.getBitsFromInteger(bits, 0, 4)
            val subscriptionAddress = mIndex.subscriptionIndex[subscriptionIndexId - 1]

            getSubscription(subscriptionAddress, type1, type2, used, rest)
        }

        return subscriptions
    }

    private fun readSubscriptionIndexSlot(subscriptionSlot: Int): ByteArray {
        return readBlocksAtBytePointer(subscriptionSlot, 2)
    }

    private fun getSubscription(subscriptionAddress: Int, type1: Int, type2: Int, used: Int, rest: Int): OVChipSubscription {
        val data = readSubscription(subscriptionAddress)
        return OVChipSubscription.create(subscriptionAddress, data, type1, type2, used, rest)
    }

    private fun readSubscription(subscriptionAddress: Int): ByteArray {
        return readBlocksAtBytePointer(subscriptionAddress, 3)
    }
}
