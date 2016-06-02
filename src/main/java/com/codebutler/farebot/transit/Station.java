/*
 * Station.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011, 2015 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.CallSuper;

public class Station implements Parcelable {

    public static final Creator<Station> CREATOR = new Creator<Station>() {
        @Override
        public Station createFromParcel(Parcel parcel) {
            return new Station(parcel);
        }

        @Override
        public Station[] newArray(int size) {
            return new Station[size];
        }
    };

    private final String mCompanyName;
    private final String mLineName;
    private final String mStationName;
    private final String mShortStationName;
    private final String mLatitude;
    private final String mLongitude;

    public Station(String stationName, String latitude, String longitude) {
        this(stationName, null, latitude, longitude);
    }

    public Station(String stationName, String shortStationName, String latitude, String longitude) {
        this(null, null, stationName, shortStationName, latitude, longitude);
    }

    public Station(
            String companyName,
            String lineName,
            String stationName,
            String shortStationName,
            String latitude,
            String longitude) {
        mCompanyName = companyName;
        mLineName = lineName;
        mStationName = stationName;
        mShortStationName = shortStationName;
        mLatitude = latitude;
        mLongitude = longitude;
    }

    protected Station(Parcel parcel) {
        mCompanyName = parcel.readString();
        mLineName = parcel.readString();
        mStationName = parcel.readString();
        mShortStationName = parcel.readString();
        mLatitude = parcel.readString();
        mLongitude = parcel.readString();
    }

    public String getStationName() {
        return mStationName;
    }

    public String getShortStationName() {
        return (mShortStationName != null) ? mShortStationName : mStationName;
    }

    public String getCompanyName() {
        return mCompanyName;
    }

    public String getLineName() {
        return mLineName;
    }

    public String getLatitude() {
        return mLatitude;
    }

    public String getLongitude() {
        return mLongitude;
    }

    public boolean hasLocation() {
        return getLatitude() != null && !getLatitude().isEmpty()
                && getLongitude() != null && !getLongitude().isEmpty();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    @CallSuper
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mCompanyName);
        parcel.writeString(mLineName);
        parcel.writeString(mStationName);
        parcel.writeString(mShortStationName);
        parcel.writeString(mLatitude);
        parcel.writeString(mLongitude);
    }
}
