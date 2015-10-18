package com.codebutler.farebot.transit.hsl;

import android.os.Parcel;

import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.R;
import com.codebutler.farebot.transit.Refill;

import java.text.NumberFormat;
import java.util.Locale;

public class HSLRefill extends Refill {
    private final long mRefillTime;
    private final long mRefillAmount;

    public HSLRefill(byte[] data) {
        mRefillTime = HSLTransitData.cardDateToTimestamp(HSLTransitData.bitsToLong(20, 14, data), HSLTransitData.bitsToLong(34, 11, data));
        mRefillAmount = HSLTransitData.bitsToLong(45, 20, data);
    }

    public HSLRefill(Parcel parcel) {
        mRefillTime = parcel.readLong();
        mRefillAmount = parcel.readLong();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mRefillTime);
        dest.writeLong(mRefillAmount);
    }

    @Override public long getTimestamp() {
        return mRefillTime;
    }

    @Override public String getAgencyName() {
        return FareBotApplication.getInstance().getString(R.string.hsl_balance_refill);
    }

    @Override public String getShortAgencyName() {
        return FareBotApplication.getInstance().getString(R.string.hsl_balance_refill);
    }

    @Override public long getAmount() {
        return mRefillAmount;
    }

    @Override public String getAmountString() {
        return NumberFormat.getCurrencyInstance(Locale.GERMANY).format(mRefillAmount / 100.0);
    }
}
