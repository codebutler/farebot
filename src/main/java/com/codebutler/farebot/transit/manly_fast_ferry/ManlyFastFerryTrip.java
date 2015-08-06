package com.codebutler.farebot.transit.manly_fast_ferry;

import android.os.Parcel;
import android.os.Parcelable;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.transit.manly_fast_ferry.record.ManlyFastFerryPurseRecord;

/**
 * Trips on the card are "purse debits", and it's not possible to tell it apart from non-ticket
 * usage (like cafe purchases).
 */
public class ManlyFastFerryTrip extends Trip {
    private GregorianCalendar mEpoch;
    private ManlyFastFerryPurseRecord mPurse;

    public ManlyFastFerryTrip(ManlyFastFerryPurseRecord purse, GregorianCalendar epoch) {
        mPurse = purse;
        mEpoch = epoch;
    }

    public ManlyFastFerryTrip(Parcel parcel) {
        mPurse = new ManlyFastFerryPurseRecord(parcel);
        mEpoch = new GregorianCalendar();
        mEpoch.setTimeInMillis(parcel.readLong());
    }

    // Implemented functionality.
    @Override
    public long getTimestamp() {
        GregorianCalendar ts = new GregorianCalendar();
        ts.setTimeInMillis(mEpoch.getTimeInMillis());
        ts.add(Calendar.DATE, mPurse.getDay());
        ts.add(Calendar.MINUTE, mPurse.getMinute());

        return ts.getTimeInMillis() / 1000;
    }

    @Override
    public long getExitTimestamp() {
        // This never gets used, except by Clipper, so stub.
        return 0;
    }

    @Override
    public String getRouteName() {
        return null;
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
    public String getFareString() {
        return NumberFormat.getCurrencyInstance(Locale.US).format(getFare());
    }

    @Override
    public String getBalanceString() {
        return null;
    }

    @Override
    public String getStartStationName() {
        return null;
    }

    @Override
    public Station getStartStation() {
        return null;
    }

    @Override
    public String getEndStationName() {
        return null;
    }

    @Override
    public Station getEndStation() {
        return null;
    }

    @Override
    public double getFare() {
        return (double)mPurse.getTransactionValue() / 100.;
    }

    @Override
    public Mode getMode() {
        // All transactions look the same... but this is a ferry, so we'll call it a ferry one.
        // Even when you buy things at the cafe.
        return Mode.FERRY;
    }

    @Override
    public boolean hasTime() {
        return true;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        mPurse.writeToParcel(parcel, i);
        parcel.writeLong(mEpoch.getTimeInMillis());
    }

    public static final Parcelable.Creator<ManlyFastFerryTrip> CREATOR = new Parcelable.Creator<ManlyFastFerryTrip>() {

        public ManlyFastFerryTrip createFromParcel(Parcel in) {
            return new ManlyFastFerryTrip(in);
        }

        public ManlyFastFerryTrip[] newArray(int size) {
            return new ManlyFastFerryTrip[size];
        }
    };



}
