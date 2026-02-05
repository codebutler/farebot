/*
 * OVChipTransitFactory.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012-2013 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2012, 2014-2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.ovc

import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.base.util.hex
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.Trip

class OVChipTransitFactory(
    private val stringResource: StringResource
) : TransitFactory<ClassicCard, OVChipTransitInfo> {

    override fun check(card: ClassicCard): Boolean {
        if (card.sectors.size != 40) {
            return false
        }
        if (card.getSector(0) is DataClassicSector) {
            val blockData = (card.getSector(0) as DataClassicSector).readBlocks(1, 1)
            return blockData.size >= 11 && blockData.copyOfRange(0, 11).contentEquals(OVC_HEADER)
        }
        return false
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity {
        val hex = (card.getSector(0) as DataClassicSector).getBlock(0).data.hex()
        val id = hex.substring(0, 8)
        return TransitIdentity.create("OV-chipkaart", id)
    }

    override fun parseInfo(card: ClassicCard): OVChipTransitInfo {
        val index = OVChipIndex.create((card.getSector(39) as DataClassicSector).readBlocks(11, 4))
        val parser = OVChipParser(card, index)
        val credit = parser.getCredit()
        val preamble = parser.getPreamble()
        val info = parser.getInfo()

        val transactions = parser.getTransactions().toMutableList()
        transactions.sortWith(OVChipTransaction.ID_ORDER)

        val trips = mutableListOf<OVChipTrip>()

        var i = 0
        while (i < transactions.size) {
            val transaction = transactions[i]

            if (transaction.valid != 1) {
                i++
                continue
            }

            if (i < transactions.size - 1) {
                val nextTransaction = transactions[i + 1]
                if (transaction.id == nextTransaction.id) {
                    i++
                    continue
                } else if (transaction.isSameTrip(nextTransaction)) {
                    trips.add(OVChipTrip.create(transaction, nextTransaction, stringResource))
                    i++
                    if (i < transactions.size - 2) {
                        val followingTransaction = transactions[i + 1]
                        if (nextTransaction.id == followingTransaction.id) {
                            i++
                        }
                    }
                    i++
                    continue
                }
            }

            trips.add(OVChipTrip.create(transaction, stringResource))
            i++
        }

        trips.sortWith(OVChipTrip.ID_ORDER)

        val subs = parser.getSubscriptions().toMutableList()
        subs.sortWith(Comparator { s1, s2 ->
            s1.id.compareTo(s2.id)
        })

        return OVChipTransitInfo(
            trips = trips.toList<Trip>(),
            subscriptions = subs.toList<Subscription>(),
            index = index,
            preamble = preamble,
            ovcInfo = info,
            credit = credit,
            stringResource = stringResource,
        )
    }

    companion object {
        private val OVC_HEADER = byteArrayOf(
            -124, 0, 0, 0, 6, 3, -96, 0, 19, -82, -28
        )
    }
}
