package com.codebutler.farebot.transit.manly_fast_ferry;

import android.os.Parcel;
import android.os.Parcelable;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.manly_fast_ferry.record.ManlyFastFerryPurseRecord;

/**
 * Describes top-up amounts "purse credits".
 */
public class ManlyFastFerryRefill extends Refill {
    private GregorianCalendar mEpoch;
    private ManlyFastFerryPurseRecord mPurse;

    public ManlyFastFerryRefill(ManlyFastFerryPurseRecord purse, GregorianCalendar epoch) {
        mPurse = purse;
        mEpoch = epoch;
    }

    public ManlyFastFerryRefill(Parcel parcel) {
        mPurse = new ManlyFastFerryPurseRecord(parcel);
        mEpoch = new GregorianCalendar();
        mEpoch.setTimeInMillis(parcel.readLong());
    }

    @Override
    public long getTimestamp() {
        GregorianCalendar ts = new GregorianCalendar();
        ts.setTimeInMillis(mEpoch.getTimeInMillis());
        ts.add(Calendar.DATE, mPurse.getDay());
        ts.add(Calendar.MINUTE, mPurse.getMinute());

        return ts.getTimeInMillis() / 1000;
    }

    @Override
    public String getAgencyName() {
        // There is only one agency on the card, don't show anything.
        return null;
    }

    @Override
    public String getShortAgencyName() {
        // There is only one agency on the card, don't show anything.
        return null;
    }

    @Override
    public long getAmount() {
        return mPurse.getTransactionValue();
    }

    @Override
    public String getAmountString() {
        return NumberFormat.getCurrencyInstance(Locale.US).format((double)getAmount() / 100);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        mPurse.writeToParcel(parcel, i);
        parcel.writeLong(mEpoch.getTimeInMillis());
    }

    public static final Parcelable.Creator<ManlyFastFerryRefill> CREATOR = new Parcelable.Creator<ManlyFastFerryRefill>() {

        public ManlyFastFerryRefill createFromParcel(Parcel in) {
            return new ManlyFastFerryRefill(in);
        }

        public ManlyFastFerryRefill[] newArray(int size) {
            return new ManlyFastFerryRefill[size];
        }
    };
}
