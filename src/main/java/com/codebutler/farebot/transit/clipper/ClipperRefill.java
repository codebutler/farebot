package com.codebutler.farebot.transit.clipper;

import android.os.Parcel;

import com.codebutler.farebot.transit.Refill;

import java.text.NumberFormat;
import java.util.Locale;

public class ClipperRefill extends Refill {
    final long mTimestamp;
    final long mAmount;
    final long mMachineID;
    final long mAgency;

    public static final Creator<ClipperRefill> CREATOR = new Creator<ClipperRefill>() {
        public ClipperRefill createFromParcel(Parcel parcel) {
            return new ClipperRefill(parcel);
        }

        public ClipperRefill[] newArray(int size) {
            return new ClipperRefill[size];
        }
    };

    public ClipperRefill(long timestamp, long amount, long agency, long machineid) {
        mTimestamp  = timestamp;
        mAmount     = amount;
        mMachineID  = machineid;
        mAgency     = agency;
    }

    public ClipperRefill(Parcel parcel) {
        mTimestamp = parcel.readLong();
        mAmount    = parcel.readLong();
        mMachineID = parcel.readLong();
        mAgency    = parcel.readLong();
    }

    @Override public long getTimestamp() {
        return mTimestamp;
    }

    @Override public long getAmount() {
        return mAmount;
    }

    @Override public String getAmountString() {
        return NumberFormat.getCurrencyInstance(Locale.US).format((double)mAmount / 100.0);
    }

    public long getMachineID() {
        return mMachineID;
    }

    @Override public String getAgencyName() {
        return ClipperTransitData.getAgencyName((int)mAgency);
    }

    @Override public String getShortAgencyName() {
        return ClipperTransitData.getShortAgencyName((int) mAgency);
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(mTimestamp);
        parcel.writeLong(mAmount);
        parcel.writeLong(mMachineID);
        parcel.writeLong(mAgency);
    }
}
