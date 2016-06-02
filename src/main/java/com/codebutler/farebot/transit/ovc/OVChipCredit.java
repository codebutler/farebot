/*
 * OVChipCredit.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2012, 2014-2015 Eric Butler <eric@codebutler.com>
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

import android.os.Parcel;
import android.os.Parcelable;

import com.codebutler.farebot.util.Utils;

public class OVChipCredit implements Parcelable {

    public static final Parcelable.Creator<OVChipCredit> CREATOR = new Parcelable.Creator<OVChipCredit>() {
        @Override
        public OVChipCredit createFromParcel(Parcel source) {
            int id = source.readInt();
            int creditId = source.readInt();
            int credit = source.readInt();
            int banbits = source.readInt();
            return new OVChipCredit(id, creditId, credit, banbits);
        }

        @Override
        public OVChipCredit[] newArray(int size) {
            return new OVChipCredit[size];
        }
    };

    private final int mId;
    private final int mCreditId;
    private final int mCredit;
    private final int mBanbits;

    private OVChipCredit(int id, int creditId, int credit, int banbits) {
        mId = id;
        mCreditId = creditId;
        mCredit = credit;
        mBanbits = banbits;
    }

    OVChipCredit(byte[] data) {
        if (data == null) {
            data = new byte[16];
        }

        final int banbits = Utils.getBitsFromBuffer(data, 0, 9);
        final int id = Utils.getBitsFromBuffer(data, 9, 12);
        final int creditId = Utils.getBitsFromBuffer(data, 56, 12);
        int credit = Utils.getBitsFromBuffer(data, 78, 15);    // Skipping the first bit (77)...

        if ((data[9] & (byte) 0x04) != 4) {    // ...as the first bit is used to see if the credit is negative or not
            credit ^= (char) 0x7FFF;
            credit = credit * -1;
        }

        mId = id;
        mCreditId = creditId;
        mCredit = credit;
        mBanbits = banbits;
    }

    public int getId() {
        return mId;
    }

    public int getCreditId() {
        return mCreditId;
    }

    public int getCredit() {
        return mCredit;
    }

    public int getBanbits() {
        return mBanbits;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mId);
        parcel.writeInt(mCreditId);
        parcel.writeInt(mCredit);
        parcel.writeInt(mBanbits);
    }
}
