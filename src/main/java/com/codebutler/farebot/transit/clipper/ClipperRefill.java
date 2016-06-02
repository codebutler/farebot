/*
 * ClipperRefill.java
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

package com.codebutler.farebot.transit.clipper;

import android.os.Parcel;

import com.codebutler.farebot.transit.Refill;

import java.text.NumberFormat;
import java.util.Locale;

class ClipperRefill extends Refill {

    public static final Creator<ClipperRefill> CREATOR = new Creator<ClipperRefill>() {
        @Override
        public ClipperRefill createFromParcel(Parcel parcel) {
            return new ClipperRefill(parcel);
        }

        @Override
        public ClipperRefill[] newArray(int size) {
            return new ClipperRefill[size];
        }
    };

    private final long mTimestamp;
    private final long mAmount;
    private final long mMachineID;
    private final long mAgency;

    ClipperRefill(long timestamp, long amount, long agency, long machineid) {
        mTimestamp = timestamp;
        mAmount = amount;
        mMachineID = machineid;
        mAgency = agency;
    }

    private ClipperRefill(Parcel parcel) {
        mTimestamp = parcel.readLong();
        mAmount = parcel.readLong();
        mMachineID = parcel.readLong();
        mAgency = parcel.readLong();
    }

    @Override
    public long getTimestamp() {
        return mTimestamp;
    }

    @Override
    public long getAmount() {
        return mAmount;
    }

    @Override
    public String getAmountString() {
        return NumberFormat.getCurrencyInstance(Locale.US).format((double) mAmount / 100.0);
    }

    public long getMachineID() {
        return mMachineID;
    }

    @Override
    public String getAgencyName() {
        return ClipperTransitData.getAgencyName((int) mAgency);
    }

    @Override
    public String getShortAgencyName() {
        return ClipperTransitData.getShortAgencyName((int) mAgency);
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(mTimestamp);
        parcel.writeLong(mAmount);
        parcel.writeLong(mMachineID);
        parcel.writeLong(mAgency);
    }
}
