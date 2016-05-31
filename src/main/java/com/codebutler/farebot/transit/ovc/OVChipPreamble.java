/*
 * OVChipPreamble.java
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

class OVChipPreamble implements Parcelable {

    private final String mId;
    private final int mCheckbit;
    private final String mManufacturer;
    private final String mPublisher;
    private final String mUnknownConstant1;
    private final int mExpdate;
    private final String mUnknownConstant2;
    private final int mType;

    private OVChipPreamble(
            String id,
            int checkbit,
            String manufacturer,
            String publisher,
            String unknownConstant1,
            int expdate,
            String unknownConstant2,
            int type
    ) {
        mId = id;
        mCheckbit = checkbit;
        mManufacturer = manufacturer;
        mPublisher = publisher;
        mUnknownConstant1 = unknownConstant1;
        mExpdate = expdate;
        mUnknownConstant2 = unknownConstant2;
        mType = type;
    }

    OVChipPreamble(byte[] data) {
        if (data == null) {
            data = new byte[48];
        }

        String hex = Utils.getHexString(data, null);

        mId = hex.substring(0, 8);
        mCheckbit = Utils.getBitsFromBuffer(data, 32, 8);
        mManufacturer = hex.substring(10, 20);
        mPublisher = hex.substring(20, 32);
        mUnknownConstant1 = hex.substring(32, 54);
        mExpdate = Utils.getBitsFromBuffer(data, 216, 20);
        mUnknownConstant2 = hex.substring(59, 68);
        mType = Utils.getBitsFromBuffer(data, 276, 4);
    }

    public String getId() {
        return mId;
    }

    public int getCheckbit() {
        return mCheckbit;
    }

    public String getManufacturer() {
        return mManufacturer;
    }

    public String getPublisher() {
        return mPublisher;
    }

    public String getUnknownConstant1() {
        return mUnknownConstant1;
    }

    public int getExpdate() {
        return mExpdate;
    }

    public String getUnknownConstant2() {
        return mUnknownConstant2;
    }

    public int getType() {
        return mType;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<OVChipPreamble> CREATOR = new Parcelable.Creator<OVChipPreamble>() {
        @Override
        public OVChipPreamble createFromParcel(Parcel source) {
            final String id = source.readString();
            final int checkbit = source.readInt();
            final String manufacturer = source.readString();
            final String publisher = source.readString();
            final String unknownConstant1 = source.readString();
            final int expdate = source.readInt();
            final String unknownConstant2 = source.readString();
            final int type = source.readInt();

            return new OVChipPreamble(id, checkbit,
                    manufacturer, publisher,
                    unknownConstant1, expdate,
                    unknownConstant2, type);
        }

        @Override
        public OVChipPreamble[] newArray(int size) {
            return new OVChipPreamble[size];
        }
    };

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mId);
        parcel.writeInt(mCheckbit);
        parcel.writeString(mManufacturer);
        parcel.writeString(mPublisher);
        parcel.writeString(mUnknownConstant1);
        parcel.writeInt(mExpdate);
        parcel.writeString(mUnknownConstant2);
        parcel.writeInt(mType);
    }
}
