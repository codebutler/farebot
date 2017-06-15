/*
 * OVChipParser.java
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

package com.codebutler.farebot.transit.ovc;

import com.codebutler.farebot.card.classic.ClassicCard;
import com.codebutler.farebot.card.classic.ClassicUtils;
import com.codebutler.farebot.base.util.ByteUtils;
import com.codebutler.farebot.card.classic.DataClassicSector;

class OVChipParser {
    private final ClassicCard mCard;
    private final OVChipIndex mIndex;

    OVChipParser(ClassicCard card, OVChipIndex index) {
        mCard = card;
        mIndex = index;
    }

    OVChipPreamble getPreamble() {
        byte[] data = ((DataClassicSector) mCard.getSector(0)).readBlocks(0, 3);
        return OVChipPreamble.create(data);
    }

    public OVChipInfo getInfo() {
        int blockIndex = mIndex.getRecentInfoSlot();

        int sector = (char) blockIndex == (char) 0x580 ? 22 : 23;

        int startBlock = ClassicUtils.convertBytePointerToBlock(blockIndex);

        // FIXME: Clean this up
        int blockSector = ClassicUtils.blockToSector(startBlock);
        int firstBlock = ClassicUtils.sectorToBlock(blockSector);
        startBlock = startBlock - firstBlock;

        byte[] data = ((DataClassicSector) mCard.getSector(sector)).readBlocks(startBlock, 3);
        return OVChipInfo.create(data);
    }

    public OVChipCredit getCredit() {
        int blockIndex = ClassicUtils.convertBytePointerToBlock(mIndex.getRecentCreditSlot());

        // FIXME: Clean this up
        int sector = ClassicUtils.blockToSector(blockIndex);
        int firstBlock = ClassicUtils.sectorToBlock(sector);
        blockIndex = blockIndex - firstBlock;

        return OVChipCredit.create(((DataClassicSector) mCard.getSector(sector)).readBlocks(blockIndex, 1));
    }

    public OVChipTransaction[] getTransactions() {
        OVChipTransaction[] ovchipTransactions = new OVChipTransaction[28];
        for (int transactionId = 0; transactionId < ovchipTransactions.length; transactionId++) {
            ovchipTransactions[transactionId] = OVChipTransaction.create(transactionId, readTransaction(transactionId));
        }
        return ovchipTransactions;
    }

    private byte[] readTransaction(int transactionId) {
        int blockIndex = (transactionId % 7) * 2;
        if (transactionId <= 6) {
            return ((DataClassicSector) mCard.getSector(35)).readBlocks(blockIndex, 2);
        } else if (transactionId >= 7 && transactionId <= 13) {
            return ((DataClassicSector) mCard.getSector(36)).readBlocks(blockIndex, 2);
        } else if (transactionId >= 14 && transactionId <= 20) {
            return ((DataClassicSector) mCard.getSector(37)).readBlocks(blockIndex, 2);
        } else if (transactionId >= 21 && transactionId <= 27) {
            return ((DataClassicSector) mCard.getSector(38)).readBlocks(blockIndex, 2);
        } else {
            throw new IllegalArgumentException("Invalid transactionId: " + transactionId);
        }
    }

    public OVChipSubscription[] getSubscriptions() {
        byte[] data;

        data = readSubscriptionIndexSlot(mIndex.getRecentSubscriptionSlot());

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
        int count = ByteUtils.getBitsFromBuffer(data, 0, 4);
        OVChipSubscription[] subscriptions = new OVChipSubscription[count];    // Might be *dangerous* to rely on this
        int offset = 4;

        for (int i = 0; i < count; i++) {
            int bits = ByteUtils.getBitsFromBuffer(data, offset + (i * 21), 21);

            /* Based on info from ovc-tools by ocsr ( https://github.com/ocsrunl/ ) */
            int type1 = ByteUtils.getBitsFromInteger(bits, 13, 8);
            int type2 = ByteUtils.getBitsFromInteger(bits, 7, 6);
            int used = ByteUtils.getBitsFromInteger(bits, 6, 1);
            int rest = ByteUtils.getBitsFromInteger(bits, 4, 2);
            int subscriptionIndexId = ByteUtils.getBitsFromInteger(bits, 0, 4);
            int subscriptionAddress = mIndex.getSubscriptionIndex()[(subscriptionIndexId - 1)];

            subscriptions[i] = getSubscription(subscriptionAddress, type1, type2, used, rest);
        }

        return subscriptions;
    }

    private byte[] readSubscriptionIndexSlot(int subscriptionSlot) {
        int blockIndex = ClassicUtils.convertBytePointerToBlock(subscriptionSlot);

        // FIXME: Clean this up
        int sector = ClassicUtils.blockToSector(blockIndex);
        int firstBlock = ClassicUtils.sectorToBlock(sector);
        blockIndex = blockIndex - firstBlock;

        return ((DataClassicSector) mCard.getSector(sector)).readBlocks(blockIndex, 2);
    }

    private OVChipSubscription getSubscription(int subscriptionAddress, int type1, int type2, int used, int rest) {
        byte[] data = readSubscription(subscriptionAddress);
        return OVChipSubscription.create(subscriptionAddress, data, type1, type2, used, rest);
    }

    private byte[] readSubscription(int subscriptionAddress) {
        int blockIndex = ClassicUtils.convertBytePointerToBlock(subscriptionAddress);

        // FIXME: Clean this up
        int sector = ClassicUtils.blockToSector(blockIndex);
        int firstBlock = ClassicUtils.sectorToBlock(sector);
        blockIndex = blockIndex - firstBlock;

        return ((DataClassicSector) mCard.getSector(sector)).readBlocks(blockIndex, 3);
    }
}
