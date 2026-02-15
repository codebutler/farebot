/*
 * OVChipTransitFactory.kt
 *
 * Copyright 2012-2013 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright 2012, 2014-2016 Eric Butler <eric@codebutler.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.ovc

import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.base.util.getBitsFromBufferSigned
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransactionTripLastPrice
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import com.codebutler.farebot.transit.en1545.En1545Bitmap
import com.codebutler.farebot.transit.en1545.En1545Container
import com.codebutler.farebot.transit.en1545.En1545FixedHex
import com.codebutler.farebot.transit.en1545.En1545FixedInteger
import com.codebutler.farebot.transit.en1545.En1545Parser
import com.codebutler.farebot.transit.en1545.En1545TransitData
import farebot.transit.ovc.generated.resources.*

class OVChipTransitFactory(
) : TransitFactory<ClassicCard, OVChipTransitInfo> {
    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override fun check(card: ClassicCard): Boolean {
        if (card.sectors.size != 40) return false
        val sector0 = card.getSector(0) as? DataClassicSector ?: return false
        val blockData = sector0.readBlocks(1, 1)
        return blockData.size >= 11 && blockData.copyOfRange(0, 11).contentEquals(OVC_HEADER)
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity = TransitIdentity.create(NAME, null)

    override fun parseInfo(card: ClassicCard): OVChipTransitInfo {
        val index =
            OVChipIndex.parse(
                (card.getSector(39) as DataClassicSector).readBlocks(11, 4),
            )
        val credit =
            (card.getSector(39) as DataClassicSector)
                .readBlocks(if (index.recentCreditSlot) 10 else 9, 1)
        val mTicketEnvParsed =
            En1545Parser.parse(
                (card.getSector(if (index.recentInfoSlot) 23 else 22) as DataClassicSector).readBlocks(0, 3),
                En1545Container(
                    En1545FixedHex("EnvUnknown1", 48),
                    En1545FixedInteger(En1545TransitData.ENV_APPLICATION_ISSUER_ID, 5), // Could be 4 bits though
                    En1545FixedInteger.date(En1545TransitData.ENV_APPLICATION_VALIDITY_END),
                    En1545FixedHex("EnvUnknown2", 43),
                    En1545Bitmap(
                        En1545FixedHex("NeverSeen1", 8),
                        En1545Container(
                            En1545FixedInteger.dateBCD(En1545TransitData.HOLDER_BIRTH_DATE),
                            En1545FixedHex("EnvUnknown3", 32),
                            En1545FixedInteger(AUTOCHARGE_ACTIVE, 3),
                            En1545FixedInteger(AUTOCHARGE_LIMIT, 16),
                            En1545FixedInteger(AUTOCHARGE_CHARGE, 16),
                            En1545FixedInteger(AUTOCHARGE_UNKNOWN, 16),
                        ),
                    ),
                ),
            )

        // byte 0-11:unknown const
        val sector0block1 = (card.getSector(0) as DataClassicSector).getBlock(1).data
        val mExpdate = sector0block1.getBitsFromBuffer(88, 20)
        // last bytes: unknown const
        val mBanbits = credit.getBitsFromBuffer(0, 9)
        val mCreditSlotId = credit.getBitsFromBuffer(9, 12)
        val mCreditId = credit.getBitsFromBuffer(56, 12)
        val mCredit = credit.getBitsFromBufferSigned(77, 16) xor 0x7fff.inv()
        // byte 0-2.5: unknown const
        val sector0block2 = (card.getSector(0) as DataClassicSector).getBlock(2).data
        val mType = sector0block2.getBitsFromBuffer(20, 4)

        val trips = getTrips(card)
        val subscriptions = getSubscriptions(card, index)

        return OVChipTransitInfo(
            parsed = mTicketEnvParsed,
            index = index,
            expdate = mExpdate,
            type = mType,
            creditSlotId = mCreditSlotId,
            creditId = mCreditId,
            credit = mCredit,
            banbits = mBanbits,
            trips = trips,
            subscriptions = subscriptions,
        )
    }

    private fun getTrips(card: ClassicCard): List<com.codebutler.farebot.transit.TransactionTripAbstract> {
        val transactions =
            (0..27).mapNotNull { transactionId ->
                OVChipTransaction.parseClassic(
                    (card.getSector(35 + transactionId / 7) as DataClassicSector)
                        .readBlocks(transactionId % 7 * 2, 2),
                )
            }
        val taggedTransactions =
            transactions
                .filter {
                    // don't include Reload transactions when grouping,
                    // which might have conflicting IDs
                    !it.isTransparent
                }.groupingBy { it.id }
                .reduce { _, transaction, nextTransaction ->
                    if (transaction.isTapOff) {
                        // check for two consecutive (duplicate) logouts, skip the second one
                        transaction
                    } else {
                        // handle two consecutive (duplicate) logins, skip the first one
                        nextTransaction
                    }
                }.values
        val fullTransactions =
            taggedTransactions +
                transactions.filter {
                    it.isTransparent
                }

        return TransactionTripLastPrice.merge(fullTransactions.toMutableList())
    }

    private fun getSubscriptions(
        card: ClassicCard,
        index: OVChipIndex,
    ): List<OVChipSubscription> {
        val data =
            (card.getSector(39) as DataClassicSector)
                .readBlocks(if (index.recentSubscriptionSlot) 3 else 1, 2)

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
        val count = data.getBitsFromBuffer(0, 4)
        return (0 until count)
            .map {
                val bits = data.getBitsFromBuffer(4 + it * 21, 21)

                // Based on info from ovc-tools by ocsr ( https://github.com/ocsrunl/ )
                val type1 = NumberUtils.getBitsFromInteger(bits, 13, 8)
                // val type2 = NumberUtils.getBitsFromInteger(bits, 7, 6)
                val used = NumberUtils.getBitsFromInteger(bits, 6, 1)
                // val rest = NumberUtils.getBitsFromInteger(bits, 4, 2)
                val subscriptionIndexId = NumberUtils.getBitsFromInteger(bits, 0, 4)
                val subscriptionAddress = index.subscriptionIndex[subscriptionIndexId - 1]
                val subData =
                    (card.getSector(32 + subscriptionAddress / 5) as DataClassicSector)
                        .readBlocks(subscriptionAddress % 5 * 3, 3)

                OVChipSubscription.parse(subData, type1, used)
            }.sortedWith { s1, s2 -> (s1.id ?: 0).compareTo(s2.id ?: 0) }
    }

    companion object {
        private val NAME = FormattedString("OV-chipkaart")

        private val CARD_INFO =
            CardInfo(
                nameRes = Res.string.ovc_card_name,
                cardType = CardType.MifareClassic,
                region = TransitRegion.NETHERLANDS,
                locationRes = Res.string.ovc_location,
                imageRes = Res.drawable.ovchip_card,
                latitude = 52.3676f,
                longitude = 4.9041f,
                brandColor = 0x84ABC7,
                credits = listOf("Wilbert Duijvenvoorde"),
                keysRequired = true,
            )

        private val OVC_HEADER =
            byteArrayOf(
                0x84.toByte(),
                0x00,
                0x00,
                0x00,
                0x06,
                0x03,
                0xA0.toByte(),
                0x00,
                0x13,
                0xAE.toByte(),
                0xE4.toByte(),
            )

        internal const val AUTOCHARGE_ACTIVE = "AutochargeActive"
        internal const val AUTOCHARGE_LIMIT = "AutochargeLimit"
        internal const val AUTOCHARGE_CHARGE = "AutochargeCharge"
        internal const val AUTOCHARGE_UNKNOWN = "AutochargeUnknown"
    }
}
