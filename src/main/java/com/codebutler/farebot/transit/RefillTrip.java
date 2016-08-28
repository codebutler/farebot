/*
 * RefillTrip.java
 *
 * Copyright 2016 Michael Farrell <micolous+git@gmail.com>
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

import com.google.auto.value.AutoValue;

/**
 * Wrapper around Refills to make them like Trips, so Trips become like history.  This is similar
 * to what the Japanese cards (Edy, Suica) already had implemented for themselves.
 */
@AutoValue
public abstract class RefillTrip extends Trip {

    @NonNull
    public static RefillTrip create(@NonNull Refill refill) {
        return new AutoValue_RefillTrip(refill);
    }

    @Override
    public long getTimestamp() {
        return getRefill().getTimestamp();
    }

    @Override
    public long getExitTimestamp() {
        return 0;
    }

    @Override
    public String getRouteName() {
        return null;
    }

    @Override
    public String getAgencyName() {
        return getRefill().getAgencyName();
    }

    @Override
    public String getShortAgencyName() {
        return getRefill().getShortAgencyName();
    }

    @Override
    public String getFareString() {
        return getRefill().getAmountString();
    }

    @Override
    public String getBalanceString() {
        return null;
    }

    @Override
    public String getStartStationName() {
        return null;
    }

    @Override
    public Station getStartStation() {
        return null;
    }

    @Override
    public String getEndStationName() {
        return null;
    }

    @Override
    public Station getEndStation() {
        return null;
    }

    @Override
    public boolean hasFare() {
        return true;
    }

    @Override
    public Trip.Mode getMode() {
        return Mode.TICKET_MACHINE;
    }

    @Override
    public boolean hasTime() {
        return true;
    }

    @NonNull
    abstract Refill getRefill();
}
