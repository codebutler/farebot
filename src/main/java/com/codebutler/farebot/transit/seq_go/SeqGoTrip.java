/*
 * SeqGoTrip.java
 *
 * Copyright 2015 Michael Farrell <micolous+git@gmail.com>
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
package com.codebutler.farebot.transit.seq_go;

import android.os.Parcel;
import android.os.Parcelable;

import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;

import java.util.GregorianCalendar;

/**
 * Represents trip events on Go Card.
 */
public class SeqGoTrip extends Trip {
    int mJourneyId;
    Mode mMode;
    GregorianCalendar mStartTime;
    GregorianCalendar mEndTime;
    int mStartStation;
    int mEndStation;


    @Override
    public long getTimestamp() {
        if (mStartTime != null) {
            return mStartTime.getTimeInMillis() / 1000;
        } else {
            return 0;
        }
    }

    @Override
    public long getExitTimestamp() {
        if (mEndTime != null) {
            return mEndTime.getTimeInMillis() / 1000;
        } else {
            return 0;
        }
    }

    @Override
    public String getRouteName() {
        return null;
    }

    @Override
    public String getAgencyName() {
        switch (mMode) {
            case FERRY:
                return "Transdev Brisbane Ferries";
            case TRAIN:
                // Domestic Airport == 9
                if (mStartStation == 9 || mEndStation == 9) {
                    // TODO: Detect International Airport station.
                    return "Airtrain";
                } else {
                    return "Queensland Rail";
                }
            default:
                return "TransLink";
        }
    }

    @Override
    public String getShortAgencyName() {
        return getAgencyName();
    }

    @Override
    public String getFareString() {
        return null;
    }

    @Override
    public String getBalanceString() {
        return null;
    }

    @Override
    public String getStartStationName() {
        if (mStartStation == 0) {
            return null;
        } else {
            Station s = getStartStation();
            if (s == null) {
                return "Unknown (" + Integer.toString(mStartStation) + ")";
            } else {
                return s.getStationName();
            }
        }
    }

    @Override
    public Station getStartStation() {
        return SeqGoUtil.getStation(mStartStation);
    }

    @Override
    public String getEndStationName() {
        if (mEndStation == 0) {
            return null;
        } else {
            Station s = getEndStation();
            if (s == null) {
                return "Unknown (" + Integer.toString(mEndStation) + ")";
            } else {
                return s.getStationName();
            }
        }
    }

    @Override
    public Station getEndStation() {
        return SeqGoUtil.getStation(mEndStation);
    }

    @Override
    public boolean hasFare() {
        // We can't calculate fares yet.
        return false;
    }

    @Override
    public Mode getMode() {
        return mMode;
    }

    @Override
    public boolean hasTime() {
        return mStartTime != null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mJourneyId);
        parcel.writeLong(mStartTime == null ? 0 : mStartTime.getTimeInMillis());
        parcel.writeLong(mEndTime == null ? 0 : mEndTime.getTimeInMillis());
        parcel.writeString(mMode.toString());
        parcel.writeInt(mStartStation);
        parcel.writeInt(mEndStation);
    }

    public SeqGoTrip(Parcel parcel) {
        mJourneyId = parcel.readInt();
        long startTime = parcel.readLong();
        if (startTime != 0) {
            mStartTime = new GregorianCalendar();
            mStartTime.setTimeInMillis(startTime);
        }

        long endTime = parcel.readLong();
        if (endTime != 0) {
            mEndTime = new GregorianCalendar();
            mEndTime.setTimeInMillis(endTime);
        }

        mMode = Mode.valueOf(parcel.readString());
        mStartStation = parcel.readInt();
        mEndStation = parcel.readInt();
    }

    public SeqGoTrip() {}

    public static final Parcelable.Creator<SeqGoTrip> CREATOR = new Parcelable.Creator<SeqGoTrip>() {

        public SeqGoTrip createFromParcel(Parcel in) {
            return new SeqGoTrip(in);
        }

        public SeqGoTrip[] newArray(int size) {
            return new SeqGoTrip[size];
        }
    };
}
