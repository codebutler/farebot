/*
 * ManlyFastFerryPurseRecord.java
 *
 * Copyright 2015 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.manly_fast_ferry.record;

import android.os.Parcel;
import android.os.Parcelable;

import com.codebutler.farebot.util.Utils;

/**
 * Represents a "purse" type record.
 */
public class ManlyFastFerryPurseRecord extends ManlyFastFerryRegularRecord implements Parcelable {

    public static final Creator<ManlyFastFerryPurseRecord> CREATOR = new Creator<ManlyFastFerryPurseRecord>() {
        @Override
        public ManlyFastFerryPurseRecord createFromParcel(Parcel source) {
            return new ManlyFastFerryPurseRecord(source);
        }

        @Override
        public ManlyFastFerryPurseRecord[] newArray(int size) {
            return new ManlyFastFerryPurseRecord[size];
        }
    };

    private int mDay;
    private int mMinute;
    private boolean mIsCredit;
    private int mTransactionValue;

    public static ManlyFastFerryPurseRecord recordFromBytes(byte[] input) {
        if (input[0] != 0x02) {
            throw new AssertionError("PurseRecord input[0] != 0x02");
        }

        ManlyFastFerryPurseRecord record = new ManlyFastFerryPurseRecord();
        if (input[3] == 0x09) {
            record.mIsCredit = false;
        } else if (input[3] == 0x08) {
            record.mIsCredit = true;
        } else {
            // bad record?
            return null;
        }

        record.mDay = Utils.getBitsFromBuffer(input, 32, 20);
        if (record.mDay < 0) {
            throw new AssertionError("Day < 0");
        }

        record.mMinute = Utils.getBitsFromBuffer(input, 52, 12);
        if (record.mMinute > 1440) {
            throw new AssertionError("Minute > 1440");
        }
        if (record.mMinute < 0) {
            throw new AssertionError("Minute < 0");
        }

        record.mTransactionValue = Utils.byteArrayToInt(input, 8, 4);
        if (record.mTransactionValue < 0) {
            throw new AssertionError("Value < 0");
        }

        return record;
    }

    private ManlyFastFerryPurseRecord() {
    }

    public ManlyFastFerryPurseRecord(Parcel parcel) {
        mDay = parcel.readInt();
        mMinute = parcel.readInt();
        mIsCredit = parcel.readInt() == 1;
        mTransactionValue = parcel.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mDay);
        parcel.writeInt(mMinute);
        parcel.writeInt(mIsCredit ? 1 : 0);
        parcel.writeInt(mTransactionValue);
    }

    public int getDay() {
        return mDay;
    }

    public int getMinute() {
        return mMinute;
    }

    public int getTransactionValue() {
        return mTransactionValue;
    }

    public boolean getIsCredit() {
        return mIsCredit;
    }
}
