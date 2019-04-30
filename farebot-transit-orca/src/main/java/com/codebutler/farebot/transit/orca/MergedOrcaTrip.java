/*
 * MergedOrcaTrip.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014, 2016 Eric Butler <eric@codebutler.com>
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

import android.content.res.Resources;
import androidx.annotation.NonNull;

import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;
import com.google.auto.value.AutoValue;

import java.text.NumberFormat;
import java.util.Locale;

import static com.codebutler.farebot.transit.orca.OrcaData.TRANS_TYPE_CANCEL_TRIP;

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
    public String getRouteName(@NonNull Resources resources) {
        return getStartTrip().getRouteName(resources);
    }

    @Override
    public String getAgencyName(@NonNull Resources resources) {
        return getStartTrip().getAgencyName(resources);
    }

    @Override
    public String getShortAgencyName(@NonNull Resources resources) {
        return getStartTrip().getShortAgencyName(resources);
    }

    @Override
    public String getFareString(@NonNull Resources resources) {
        if (getEndTrip().getTransType() == TRANS_TYPE_CANCEL_TRIP) {
            return resources.getString(
                    R.string.transit_orca_fare_cancelled_format,
                    getStartTrip().getFareString(resources));
        }
        return NumberFormat.getCurrencyInstance(Locale.US).format(
                (getStartTrip().getFare() + getEndTrip().getFare()) / 100.0);
    }

    @Override
    public String getBalanceString() {
        return getEndTrip().getBalanceString();
    }

    @Override
    public String getStartStationName(@NonNull Resources resources) {
        return getStartTrip().getStartStationName(resources);
    }

    @Override
    public Station getStartStation() {
        return getStartTrip().getStartStation();
    }

    @Override
    public String getEndStationName(@NonNull Resources resources) {
        return getEndTrip().getStartStationName(resources);
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
