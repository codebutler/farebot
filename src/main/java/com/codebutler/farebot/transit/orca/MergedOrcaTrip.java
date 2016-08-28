/*
 * MergedOrcaTrip.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014-2015 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.orca;

import android.support.annotation.NonNull;

import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.R;
import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class MergedOrcaTrip extends Trip {

    @NonNull
    static MergedOrcaTrip create(@NonNull OrcaTrip startTrip, @NonNull OrcaTrip endTrip) {
        return new AutoValue_MergedOrcaTrip(startTrip, endTrip);
    }

    @Override
    public long getTimestamp() {
        return getStartTrip().getTimestamp();
    }

    @Override
    public long getExitTimestamp() {
        return getEndTrip().getTimestamp();
    }

    @Override
    public String getRouteName() {
        return getStartTrip().getRouteName();
    }

    @Override
    public String getAgencyName() {
        return getStartTrip().getAgencyName();
    }

    @Override
    public String getShortAgencyName() {
        return getStartTrip().getShortAgencyName();
    }

    @Override
    public String getFareString() {
        if (getEndTrip().getTransType() == OrcaTransitData.TRANS_TYPE_CANCEL_TRIP) {
            return FareBotApplication.getInstance()
                    .getString(R.string.fare_cancelled_format, getStartTrip().getFareString());
        }
        return getStartTrip().getFareString();
    }

    @Override
    public String getBalanceString() {
        return getEndTrip().getBalanceString();
    }

    @Override
    public String getStartStationName() {
        return getStartTrip().getStartStationName();
    }

    @Override
    public Station getStartStation() {
        return getStartTrip().getStartStation();
    }

    @Override
    public String getEndStationName() {
        return getEndTrip().getStartStationName();
    }

    @Override
    public Station getEndStation() {
        return getEndTrip().getStartStation();
    }

    @Override
    public boolean hasFare() {
        return getStartTrip().hasFare();
    }

    @Override
    public Mode getMode() {
        return getStartTrip().getMode();
    }

    @Override
    public boolean hasTime() {
        return getStartTrip().hasTime();
    }

    @NonNull
    abstract OrcaTrip getStartTrip();

    @NonNull
    abstract OrcaTrip getEndTrip();

}
