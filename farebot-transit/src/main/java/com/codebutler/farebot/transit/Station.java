/*
 * Station.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011, 2015-2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Station {

    @NonNull
    public static Builder builder() {
        return new AutoValue_Station.Builder();
    }

    @NonNull
    public static Station create(String stationName, String shortStationName, String latitude, String longitude) {
        return new AutoValue_Station.Builder()
                .stationName(stationName)
                .shortStationName(shortStationName)
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }

    @NonNull
    public static Station create(String name, String code, String abbreviation, String latitude, String longitude) {
        return new AutoValue_Station.Builder()
                .stationName(name)
                .code(code)
                .abbreviation(abbreviation)
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }

    public String getDisplayStationName() {
        return (getShortStationName() != null) ? getShortStationName() : getStationName();
    }

    public boolean hasLocation() {
        return getLatitude() != null && !getLatitude().isEmpty()
                && getLongitude() != null && !getLongitude().isEmpty();
    }

    @Nullable
    public abstract String getStationName();

    @Nullable
    public abstract String getShortStationName();

    @Nullable
    public abstract String getCompanyName();

    @Nullable
    public abstract String getLineName();

    @Nullable
    public abstract String getLatitude();

    @Nullable
    public abstract String getLongitude();

    @Nullable
    public abstract String getCode();

    @Nullable
    public abstract String getAbbreviation();

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder stationName(String stationName);

        public abstract Builder shortStationName(String shortStationName);

        public abstract Builder companyName(String companyName);

        public abstract Builder lineName(String lineName);

        public abstract Builder latitude(String latitude);

        public abstract Builder longitude(String longitude);

        public abstract Builder code(String code);

        public abstract Builder abbreviation(String abbreviation);

        public abstract Station build();
    }
}
