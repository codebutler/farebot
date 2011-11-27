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

public class Station
{
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
}
