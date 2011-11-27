/*
 * Station.java
 *
 * Copyright (C) 2011 Eric Butler
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
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

public class Station implements Parcelable {
    private final String mCompanyName, mLineName, mStationName, mShortStationName, mLatitude, mLongitude;

    public Station(String stationName, String latitude, String longitude) {
        this(stationName, null, latitude, longitude);
    }

    public Station(String stationName, String shortStationName, String latitude, String longitude) {
        this(null, null, stationName, shortStationName, latitude, longitude);
    }

    public Station(String companyName, String lineName, String stationName, String shortStationName, String latitude, String longitude) {
        mCompanyName      = companyName;
        mLineName         = lineName;
        mStationName      = stationName;
        mShortStationName = shortStationName;
        mLatitude         = latitude;
        mLongitude        = longitude;
    }
    
    public static Creator<Station> CREATOR = new Creator<Station>() {
        public Station createFromParcel(Parcel parcel) {
            return new Station(parcel);
        }
        public Station[] newArray(int size) {
            return new Station[size];
        }
    };

    private Station(Parcel parcel) {
        mCompanyName      = parcel.readString();
        mLineName         = parcel.readString();
        mStationName      = parcel.readString();
        mShortStationName = parcel.readString();
        mLatitude         = parcel.readString();
        mLongitude        = parcel.readString();
    }

    public String getStationName () {
        return mStationName;
    }

    public String getShortStationName () {
        return (mShortStationName != null) ? mShortStationName : mStationName;
    }

    public String getCompanyName() {
        return mCompanyName;
    }

    public String getLineName() {
        return mLineName;
    }

    public String getLatitude () {
        return mLatitude;
    }

    public String getLongitude () {
        return mLongitude;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mCompanyName);
        parcel.writeString(mLineName);
        parcel.writeString(mStationName);
        parcel.writeString(mShortStationName);
        parcel.writeString(mLatitude);
        parcel.writeString(mLongitude);
    }
}
