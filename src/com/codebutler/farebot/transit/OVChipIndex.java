/*
 * OVChipIndex.java
 *
 * Copyright (C) 2012 Eric Butler
 *
 * Authors:
 * Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit;

import android.os.Parcel;
import android.os.Parcelable;
import com.codebutler.farebot.Utils;

import java.util.Arrays;

public class OVChipIndex implements Parcelable {
    private int mRecentTransactionSlot;        // Most recent transaction slot (0xFB0 or 0xFD0)
    private int mRecentInfoSlot;            // Most recent card information index slot (0x5C0 or 0x580)
    private int mRecentSubscriptionSlot;    // Most recent subscription index slot (0xF10 or 0xF30)
    private int mRecentTravelhistorySlot;    // Most recent travel history index slot (0xF50 or 0xF70)
    private int mRecentCreditSlot;            // Most recent credit index slot (0xF90 or 0xFA0)
    private int[] mSubscriptionIndex;

    public OVChipIndex (
            int recentTransactionSlot,
            int recentInfoSlot,
            int recentSubscriptionSlot,
            int recentTravelhistorySlot,
            int recentCreditSlot,
            int[] subscriptionIndex
    ) {
        mRecentTransactionSlot = recentTransactionSlot;
        mRecentInfoSlot = recentInfoSlot;
        mRecentSubscriptionSlot = recentSubscriptionSlot;
        mRecentTravelhistorySlot = recentTravelhistorySlot;
        mRecentCreditSlot = recentCreditSlot;
        mSubscriptionIndex = subscriptionIndex;
    }

    public OVChipIndex (byte[] data) {
        int recentTransactionSlot = 0;
        int recentInfoSlot = 0;
        int recentSubscriptionSlot = 0;
        int recentTravelhistorySlot = 0;
        int recentCreditSlot = 0;
        int[] subscriptionIndex = null;

        byte[] first_slot = Arrays.copyOfRange(data, 0, data.length / 2);
        byte[] second_slot = Arrays.copyOfRange(data, data.length / 2, data.length);

        int iIDa3 = ((first_slot[1] & (char)0x3F) << 10) | ((first_slot[2] & (char)0xFF) << 2) | ((first_slot[3] >> 6) & (char)0x03);
        int iIDb3 = ((second_slot[1] & (char)0x3F) << 10) | ((second_slot[2] & (char)0xFF) << 2) | ((second_slot[3] >> 6) & (char)0x03);

        recentTransactionSlot = (iIDb3 > iIDa3 ? (char)(0xFB0) : (char)(0xFD0));
        byte[] buffer = (iIDb3 > iIDa3 ? second_slot : first_slot);

        int cardindex = ((buffer[3] >> 5) & (char)0x01);
        recentInfoSlot = (cardindex == 1 ? (char)0x5C0 : (char)0x580);

        int indexes = ((buffer[31] >> 5) & (byte)0x07);
        recentSubscriptionSlot = (((byte)indexes & (byte)0x04) == (byte)0x00 ? (char)0xF10 : (char)0xF30);
        recentTravelhistorySlot = (((byte)indexes & (byte)0x02) == (byte)0x00 ? (char)0xF50 : (char)0xF70);
        recentCreditSlot = (((byte)indexes & (byte)0x01) == (byte)0x00 ? (char)0xF90 : (char)0xFA0);

        subscriptionIndex = new int[12];
        int offset = 108;

        for (int i = 0; i < 12; i++) {
            int bits = Utils.getBitsFromBuffer(buffer, offset + (i * 4), 4);
            subscriptionIndex[i] = (bits < 5 ? ((char)0x800 + bits * (byte)0x30) : bits > 9 ? ((char)0xA00 + (bits - 10) * (byte)0x30) : ((char)0x900 + (bits - 5) * (byte)0x30));
        }

        mRecentTransactionSlot = recentTransactionSlot;
        mRecentInfoSlot = recentInfoSlot;
        mRecentSubscriptionSlot = recentSubscriptionSlot;
        mRecentTravelhistorySlot = recentTravelhistorySlot;
        mRecentCreditSlot = recentCreditSlot;
        mSubscriptionIndex = subscriptionIndex;
    }

    public int getRecentTransactionSlot() {
        return mRecentTransactionSlot;
    }

    public int getRecentInfoSlot() {
        return mRecentInfoSlot;
    }

    public int getRecentSubscriptionSlot() {
        return mRecentSubscriptionSlot;
    }

    public int getRecentTravelhistorySlot() {
        return mRecentTravelhistorySlot;
    }

    public int getRecentCreditSlot() {
        return mRecentCreditSlot;
    }

    public int[] getSubscriptionIndex() {
        return mSubscriptionIndex;
    }

    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<OVChipIndex> CREATOR = new Parcelable.Creator<OVChipIndex>() {
        public OVChipIndex createFromParcel(Parcel source) {
            int recentTransactionSlot = 0;
            int recentInfoSlot = 0;
            int recentSubscriptionSlot = 0;
            int recentTravelhistorySlot = 0;
            int recentCreditSlot = 0;
            int[] subscriptionIndex = null;

            recentTransactionSlot = source.readInt();
            recentInfoSlot = source.readInt();
            recentSubscriptionSlot = source.readInt();
            recentTravelhistorySlot = source.readInt();
            recentCreditSlot = source.readInt();

            subscriptionIndex = new int[source.readInt()];
            source.readIntArray(subscriptionIndex);

            return new OVChipIndex(recentTransactionSlot,
                    recentInfoSlot, recentSubscriptionSlot,
                    recentTravelhistorySlot, recentCreditSlot,
                    subscriptionIndex);
        }

        public OVChipIndex[] newArray (int size) {
            return new OVChipIndex[size];
        }
    };

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mRecentTransactionSlot);
        parcel.writeInt(mRecentInfoSlot);
        parcel.writeInt(mRecentSubscriptionSlot);
        parcel.writeInt(mRecentTravelhistorySlot);
        parcel.writeInt(mRecentCreditSlot);
        parcel.writeInt(mSubscriptionIndex.length);
        parcel.writeIntArray(mSubscriptionIndex);
    }
}