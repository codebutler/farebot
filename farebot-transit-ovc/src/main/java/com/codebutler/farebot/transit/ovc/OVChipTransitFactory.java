/*
 * OVChipTransitFactory.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012-2013 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
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

import android.content.Context;
import android.support.annotation.NonNull;

import com.codebutler.farebot.card.classic.ClassicCard;
import com.codebutler.farebot.core.ByteUtils;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitFactory;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.Trip;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class OVChipTransitFactory implements TransitFactory<ClassicCard, OVChipTransitInfo> {

    private static final byte[] OVC_HEADER = new byte[11];

    static {
        OVC_HEADER[0] = -124;
        OVC_HEADER[4] = 6;
        OVC_HEADER[5] = 3;
        OVC_HEADER[6] = -96;
        OVC_HEADER[8] = 19;
        OVC_HEADER[9] = -82;
        OVC_HEADER[10] = -28;
    }

    @NonNull private final OVChipDBUtil mOVChipDBUtil;

    public OVChipTransitFactory(@NonNull Context context) {
        mOVChipDBUtil = new OVChipDBUtil(context);
    }

    @Override
    public boolean check(@NonNull ClassicCard classicCard) {
        if (classicCard.getSectors().size() != 40) {
            return false;
        }
        // Starting at 0×010, 8400 0000 0603 a000 13ae e401 xxxx 0e80 80e8 seems to exist on all OVC's
        // (with xxxx different).
        // http://www.ov-chipkaart.de/back-up/3-8-11/www.ov-chipkaart.me/blog/index7e09.html?page_id=132
        byte[] blockData = classicCard.getSector(0).readBlocks(1, 1);
        return Arrays.equals(Arrays.copyOfRange(blockData, 0, 11), OVC_HEADER);

    }

    @NonNull
    @Override
    public TransitIdentity parseIdentity(@NonNull ClassicCard card) {
        String hex = ByteUtils.getHexString(card.getSector(0).getBlock(0).getData().bytes(), null);
        String id = hex.substring(0, 8);
        return TransitIdentity.create("OV-chipkaart", id);
    }

    @NonNull
    @Override
    public OVChipTransitInfo parseInfo(@NonNull ClassicCard card) {
        OVChipIndex index = OVChipIndex.create(card.getSector(39).readBlocks(11, 4));
        OVChipParser parser = new OVChipParser(card, index);
        OVChipCredit credit = parser.getCredit();
        OVChipPreamble preamble = parser.getPreamble();
        OVChipInfo info = parser.getInfo();

        List<OVChipTransaction> transactions = new ArrayList<>(Arrays.asList(parser.getTransactions()));
        Collections.sort(transactions, OVChipTransaction.ID_ORDER);

        List<OVChipTrip> trips = new ArrayList<>();

        for (int i = 0; i < transactions.size(); i++) {
            OVChipTransaction transaction = transactions.get(i);

            if (transaction.getValid() != 1) {
                continue;
            }

            if (i < (transactions.size() - 1)) {
                OVChipTransaction nextTransaction = transactions.get(i + 1);
                if (transaction.getId() == nextTransaction.getId()) {
                    // handle two consecutive (duplicate) logins, skip the first one
                    continue;
                } else if (transaction.isSameTrip(nextTransaction)) {
                    trips.add(OVChipTrip.create(mOVChipDBUtil, transaction, nextTransaction));
                    i++;
                    if (i < (transactions.size() - 2)) {
                        // check for two consecutive (duplicate) logouts, skip the second one
                        OVChipTransaction followingTransaction = transactions.get(i + 1);
                        if (nextTransaction.getId() == followingTransaction.getId()) {
                            i++;
                        }
                    }
                    continue;
                }
            }

            trips.add(OVChipTrip.create(mOVChipDBUtil, transaction));
        }

        Collections.sort(trips, OVChipTrip.ID_ORDER);

        List<OVChipSubscription> subs = Arrays.asList(parser.getSubscriptions());
        Collections.sort(subs, new Comparator<OVChipSubscription>() {
            @Override
            public int compare(OVChipSubscription s1, OVChipSubscription s2) {
                return Integer.valueOf(s1.getId()).compareTo(s2.getId());
            }
        });

        return OVChipTransitInfo.create(
                ImmutableList.<Trip>copyOf(trips),
                ImmutableList.<Subscription>copyOf(subs),
                index,
                preamble,
                info,
                credit);
    }
}
