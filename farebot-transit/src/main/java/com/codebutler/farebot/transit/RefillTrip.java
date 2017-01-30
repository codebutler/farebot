/*
 * RefillTrip.java
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

package com.codebutler.farebot.transit;

import android.content.res.Resources;
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
    public String getRouteName(@NonNull Resources resources) {
        return null;
    }

    @Override
    public String getAgencyName(@NonNull Resources resources) {
        return getRefill().getAgencyName(resources);
    }

    @Override
    public String getShortAgencyName(@NonNull Resources resources) {
        return getRefill().getShortAgencyName(resources);
    }

    @Override
    public String getFareString(@NonNull Resources resources) {
        return getRefill().getAmountString(resources);
    }

    @Override
    public String getBalanceString() {
        return null;
    }

    @Override
    public String getStartStationName(@NonNull Resources resources) {
        return null;
    }

    @Override
    public Station getStartStation() {
        return null;
    }

    @Override
    public String getEndStationName(@NonNull Resources resources) {
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
