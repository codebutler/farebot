/*
 * Trip.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2015 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
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

    public abstract String getBalanceString();

    protected abstract String getStartStationName();

    public abstract Station getStartStation();

    protected abstract String getEndStationName();

    public abstract Station getEndStation();

    /**
     * If true, it means that this activity has a known fare associated with it.  This should be
     * true for most transaction types.
     * <p>
     * Reasons for this being false, including not actually having the trip cost available, and for
     * events like card activation and card banning which have no cost associated with the action.
     * <p>
     * If a trip is free of charge, this should still be set to true.  However, if the trip is
     * associated with a monthly travel pass, then this should be set to false.
     *
     * @return true if there is a financial transaction associated with the Trip.
     */
    public abstract boolean hasFare();

    /**
     * Formats the cost of the trip in the appropriate local currency.  Be aware that your
     * implementation should use language-specific formatting and not rely on the system language
     * for that information.
     * <p>
     * For example, if a phone is set to English and travels to Japan, it does not make sense to
     * format their travel costs in dollars.  Instead, it should be shown in Yen, which the Japanese
     * currency formatter does.
     *
     * @return The cost of the fare formatted in the local currency of the card.
     */
    public abstract String getFareString();

    public abstract Mode getMode();

    public abstract boolean hasTime();

    public static String formatStationNames(Trip trip) {
        List<String> stationText = new ArrayList<>();
        if (trip.getStartStationName() != null) {
            stationText.add(trip.getStartStationName());
        }
        if (trip.getEndStationName() != null && (!trip.getEndStationName().equals(trip.getStartStationName()))) {
            stationText.add(trip.getEndStationName());
        }

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
