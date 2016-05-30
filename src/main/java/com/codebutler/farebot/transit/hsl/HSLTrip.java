/*
 * HSLTrip.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014-2015 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.hsl;

import android.os.Parcel;

import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.R;
import com.codebutler.farebot.card.desfire.DesfireRecord;
import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;

import java.text.NumberFormat;
import java.util.Locale;

public class HSLTrip extends Trip {
    String mLine;
    long mVehicleNumber;
    long mTimestamp;
    long mFare;
    final long mNewBalance;
    long mArvo;
    long mExpireTimestamp;
    long mPax;

    public HSLTrip(DesfireRecord record) {
        byte[] useData = record.getData();
        long[] usefulData = new long[useData.length];

        for (int i = 0; i < useData.length; i++) {
            usefulData[i] = ((long) useData[i]) & 0xFF;
        }

        mArvo = HSLTransitData.bitsToLong(0, 1, usefulData);

        mTimestamp = HSLTransitData.cardDateToTimestamp(HSLTransitData.bitsToLong(1, 14, usefulData), HSLTransitData.bitsToLong(15, 11, usefulData));
        mExpireTimestamp = HSLTransitData.cardDateToTimestamp(HSLTransitData.bitsToLong(26, 14, usefulData), HSLTransitData.bitsToLong(40, 11, usefulData));

        mFare = HSLTransitData.bitsToLong(51, 14, usefulData);

        mPax = HSLTransitData.bitsToLong(65, 5, usefulData);
        mLine = null;
        mVehicleNumber = -1;

        mNewBalance = HSLTransitData.bitsToLong(70, 20, usefulData);

    }

    public double getExpireTimestamp() {
        return this.mExpireTimestamp;
    }

    public static final Creator<HSLTrip> CREATOR = new Creator<HSLTrip>() {
        public HSLTrip createFromParcel(Parcel parcel) {
            return new HSLTrip(parcel);
        }

        public HSLTrip[] newArray(int size) {
            return new HSLTrip[size];
        }
    };

    HSLTrip(Parcel parcel) {
        // mArvo, mTimestamp, mExpireTimestamp, mFare, mPax, mNewBalance
        mArvo = parcel.readLong();
        mTimestamp = parcel.readLong();
        mExpireTimestamp = parcel.readLong();
        mFare = parcel.readLong();
        mPax = parcel.readLong();
        mNewBalance = parcel.readLong();
        mLine = null;
        mVehicleNumber = -1;
    }

    public HSLTrip() {
        mArvo = mTimestamp = mExpireTimestamp = mFare = mPax = mNewBalance = mVehicleNumber = -1;
        mLine = null;
    }

    @Override public long getTimestamp() {
        return mTimestamp;
    }

    @Override public long getExitTimestamp() {
        return 0;
    }

    @Override public String getAgencyName() {
        FareBotApplication app = FareBotApplication.getInstance();
        String pax = app.getString(R.string.hsl_person_format, mPax);
        if (mArvo == 1) {
            String mins = app.getString(R.string.hsl_mins_format, ((this.mExpireTimestamp - this.mTimestamp) / 60));
            String type = app.getString(R.string.hsl_balance_ticket);
            return String.format("%s, %s, %s", type, pax, mins);
        } else {
            String type = app.getString(R.string.hsl_pass_ticket);
            return String.format("%s, %s", type, pax);
        }
    }

    @Override public String getShortAgencyName() {
        return getAgencyName();
    }

    @Override public String getRouteName() {
        if (mLine != null) {
             // FIXME: i18n
            return String.format("Line %s, Vehicle %s", mLine.substring(1), mVehicleNumber);
        }
        return null;
    }

    @Override public String getFareString() {
        return NumberFormat.getCurrencyInstance(Locale.GERMANY).format(mFare / 100.0);
    }

    @Override public double getFare() {
        return mFare;
    }

    @Override public String getBalanceString() {
        return NumberFormat.getCurrencyInstance(Locale.GERMANY).format(mNewBalance / 100);
    }

    @Override public String getEndStationName() {
        return null;
    }

    @Override public Station getEndStation() {
        return null;
    }

    @Override public Mode getMode() {
        if (mLine != null) {
            if (mLine.equals("1300"))
                return Mode.METRO;
            if (mLine.equals("1019"))
                return Mode.FERRY;
            if (mLine.startsWith("100") || mLine.equals("1010"))
                return Mode.TRAM;
            if (mLine.startsWith("3"))
                return Mode.TRAIN;
            return Mode.BUS;
        } else {
            return Mode.BUS;
        }
    }

    @Override public boolean hasTime() {
        return false;
    }

    public long getCoachNumber() {
        if (mVehicleNumber > -1)
            return mVehicleNumber;
        return mPax;
    }

    @Override public String getStartStationName() {
        return null;
    }

    @Override public Station getStartStation() {
        return null;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        // mArvo, mTimestamp, mExpireTimestamp, mFare, mPax, mNewBalance
        parcel.writeLong(mArvo);
        parcel.writeLong(mTimestamp);
        parcel.writeLong(mExpireTimestamp);
        parcel.writeLong(mFare);
        parcel.writeLong(mPax);
        parcel.writeLong(mNewBalance);
    }

    public int describeContents() {
        return 0;
    }
}
