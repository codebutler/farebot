package com.codebutler.farebot.transit.manly_fast_ferry.record;

import android.os.Parcel;
import android.os.Parcelable;

import com.codebutler.farebot.util.Utils;

/**
 * Represents a "preamble" type record.
 */
public class ManlyFastFerryPurseRecord extends ManlyFastFerryRegularRecord implements Parcelable {
    private int    mDay;
    private int    mMinute;
    private boolean mIsCredit;
    private int     mTransactionValue;

    public static ManlyFastFerryPurseRecord recordFromBytes(byte[] input) {
        if (input[0] != 0x02) throw new AssertionError("PurseRecord input[0] != 0x02");

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
        if (record.mDay < 0) throw new AssertionError("Day < 0");

        record.mMinute = Utils.getBitsFromBuffer(input, 52, 12);
        if (record.mMinute > 1440) throw new AssertionError("Minute > 1440");
        if (record.mMinute < 0) throw new AssertionError("Minute < 0");

        record.mTransactionValue = Utils.byteArrayToInt(input, 8, 4);
        if (record.mTransactionValue < 0) throw new AssertionError("Value < 0");

        return record;
    }

    protected ManlyFastFerryPurseRecord() {}

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

    public int getDay() { return mDay; }
    public int getMinute() { return mMinute; }
    public int getTransactionValue() { return mTransactionValue; }
    public boolean getIsCredit() { return mIsCredit; }

}
