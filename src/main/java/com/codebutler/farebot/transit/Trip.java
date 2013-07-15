/*
 * Trip.java
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
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class Trip implements Parcelable {
    public static final Creator<Trip> CREATOR = new Creator<Trip>() {
        @Override
        public Trip createFromParcel(Parcel parcel) {
            return null;
        }

        @Override
        public Trip[] newArray(int size) {
            return new Trip[size];
        }
    };

    public abstract long getTimestamp();
    public abstract long getExitTimestamp();
    public abstract String getRouteName();
    public abstract String getAgencyName();
    public abstract String getShortAgencyName();
    public abstract String getFareString();
    public abstract String getBalanceString();
    public abstract String getStartStationName();
    public abstract Station getStartStation();
    public abstract String getEndStationName();
    public abstract Station getEndStation();
    public abstract double getFare();
    public abstract Mode getMode();
    public abstract boolean hasTime();

    public static boolean hasLocation(Station station) {
        return ((station != null) && ((station.getLatitude() != null) || station.getLongitude() != null));
    }

    public static String formatStationNames(Trip trip) {
        List<String> stationText = new ArrayList<String>();
        if (trip.getStartStationName() != null)
            stationText.add(trip.getStartStationName());
        if (trip.getEndStationName() != null && (!trip.getEndStationName().equals(trip.getStartStationName())))
            stationText.add(trip.getEndStationName());

        if (stationText.size() > 0) {
            return StringUtils.join(stationText, " → ");
        } else {
            return null;
        }
    }

    public enum Mode {
        BUS,
        TRAIN,
        TRAM,
        METRO,
        FERRY,
        TICKET_MACHINE,
        VENDING_MACHINE,
        POS,
        OTHER,
        BANNED
    }

    public static class Comparator implements java.util.Comparator<Trip> {
        @Override
        public int compare(Trip trip, Trip trip1) {
            return Long.valueOf(trip1.getTimestamp()).compareTo(trip.getTimestamp());
        }
    }
}