/*
 * SeqGoTrip.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.seq_go;

import android.content.res.Resources;
import android.support.annotation.NonNull;

import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;
import com.google.auto.value.AutoValue;

import java.util.GregorianCalendar;

/**
 * Represents trip events on Go Card.
 */
@AutoValue
abstract class SeqGoTrip extends Trip {

    @NonNull
    static Builder builder() {
        return new AutoValue_SeqGoTrip.Builder();
    }

    @Override
    public long getTimestamp() {
        if (getStartTime() != null) {
            return getStartTime().getTimeInMillis() / 1000;
        } else {
            return 0;
        }
    }

    @Override
    public long getExitTimestamp() {
        if (getEndTime() != null) {
            return getEndTime().getTimeInMillis() / 1000;
        } else {
            return 0;
        }
    }

    @Override
    public String getRouteName(@NonNull Resources resources) {
        return null;
    }

    @Override
    public String getAgencyName(@NonNull Resources resources) {
        switch (getMode()) {
            case FERRY:
                return "Transdev Brisbane Ferries";
            case TRAIN:
                // Domestic Airport == 9
                if (getStartStationId() == 9 || getEndStationId() == 9) {
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
    public String getShortAgencyName(@NonNull Resources resources) {
        return getAgencyName(resources);
    }

    @Override
    public String getFareString(@NonNull Resources resources) {
        return null;
    }

    @Override
    public String getBalanceString() {
        return null;
    }

    @Override
    public String getStartStationName(@NonNull Resources resources) {
        if (getStartStationId() == 0) {
            return null;
        } else {
            Station s = getStartStation();
            if (s == null) {
                return "Unknown (" + Integer.toString(getStartStationId()) + ")";
            } else {
                return s.getStationName();
            }
        }
    }

    @Override
    public String getEndStationName(@NonNull Resources resources) {
        if (getEndStationId() == 0) {
            return null;
        } else {
            Station s = getEndStation();
            if (s == null) {
                return "Unknown (" + Integer.toString(getEndStationId()) + ")";
            } else {
                return s.getStationName();
            }
        }
    }

    @Override
    public boolean hasFare() {
        // We can't calculate fares yet.
        return false;
    }

    @Override
    public boolean hasTime() {
        return getStartTime() != null;
    }

    abstract int getJourneyId();

    public abstract Mode getMode();

    abstract GregorianCalendar getStartTime();

    abstract GregorianCalendar getEndTime();

    abstract int getStartStationId();

    abstract int getEndStationId();

    @AutoValue.Builder
    abstract static class Builder {

        abstract Builder journeyId(int journeyId);

        abstract Builder mode(Mode mode);

        abstract Builder startTime(GregorianCalendar startTime);

        abstract Builder endTime(GregorianCalendar endTime);

        abstract Builder startStationId(int startStationId);

        abstract Builder startStation(Station station);

        abstract Builder endStationId(int endStationId);

        abstract Builder endStation(Station station);

        abstract SeqGoTrip build();
    }
}
