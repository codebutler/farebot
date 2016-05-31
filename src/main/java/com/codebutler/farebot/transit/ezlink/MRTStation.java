/*
 * MRTStation.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014-2015 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2011 Sean Cross <sean@chumby.com>
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

package com.codebutler.farebot.transit.ezlink;

import android.os.Parcel;

import com.codebutler.farebot.transit.Station;

class MRTStation extends Station {

    public static final Creator<MRTStation> CREATOR = new Creator<MRTStation>() {
        @Override
        public MRTStation createFromParcel(Parcel source) {
            return new MRTStation(source);
        }

        @Override
        public MRTStation[] newArray(int size) {
            return new MRTStation[size];
        }
    };

    private final String mCode;
    private final String mAbbreviation;

    MRTStation(String name, String code, String abbreviation, String latitude, String longitude) {
        super(name, latitude, longitude);

        mCode = code;
        mAbbreviation = abbreviation;
    }

    private MRTStation(Parcel source) {
        super(source);
        mCode = source.readString();
        mAbbreviation = source.readString();
    }

    public String getCode() {
        return mCode;
    }

    public String getAbbreviation() {
        return mAbbreviation;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        super.writeToParcel(parcel, flags);
        parcel.writeString(mCode);
        parcel.writeString(mAbbreviation);
    }
}
