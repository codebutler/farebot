/*
 * HSLTrip.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014, 2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.hsl;

import android.content.res.Resources;
import android.support.annotation.NonNull;

import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;
import com.google.auto.value.AutoValue;

import java.text.NumberFormat;
import java.util.Locale;

@AutoValue
abstract class HSLTrip extends Trip {

    @NonNull
    static Builder builder() {
        return new AutoValue_HSLTrip.Builder();
    }

    @Override
    public long getExitTimestamp() {
        return 0;
    }

    @Override
    public String getAgencyName(@NonNull Resources resource) {
        if (getArvo() == 1) {
            long mins = (getExpireTimestamp() - getTimestamp()) / 60;
            return resource.getString(R.string.hsl_balance_ticket, Long.toString(getPax()), Long.toString(mins));
        } else {
            return resource.getString(R.string.hsl_pass_ticket, Long.toString(getPax()));
        }
    }

    @Override
    public String getShortAgencyName(@NonNull Resources resources) {
        return getAgencyName(resources);
    }

    @Override
    public String getRouteName(@NonNull Resources resources) {
        if (getLine() == null) {
            return null;
        }
        String line = getLine().substring(1);
        return resources.getString(R.string.hsl_route_line_vehicle, line, Long.toString(getVehicleNumber()));
    }

    @Override
    public String getFareString(@NonNull Resources resources) {
        return NumberFormat.getCurrencyInstance(Locale.GERMANY).format(getFare() / 100.0);
    }

    @Override
    public boolean hasFare() {
        return true;
    }

    @Override
    public String getBalanceString() {
        return NumberFormat.getCurrencyInstance(Locale.GERMANY).format(getNewBalance() / 100);
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
    public Mode getMode() {
        if (getLine() != null) {
            if (getLine().equals("1300")) {
                return Mode.METRO;
            }
            if (getLine().equals("1019")) {
                return Mode.FERRY;
            }
            if (getLine().startsWith("100") || getLine().equals("1010")) {
                return Mode.TRAM;
            }
            if (getLine().startsWith("3")) {
                return Mode.TRAIN;
            }
            return Mode.BUS;
        } else {
            return Mode.BUS;
        }
    }

    @Override
    public boolean hasTime() {
        return false;
    }

    public long getCoachNumber() {
        if (getVehicleNumber() > -1) {
            return getVehicleNumber();
        }
        return getPax();
    }

    @Override
    public String getStartStationName(@NonNull Resources resources) {
        return null;
    }

    @Override
    public Station getStartStation() {
        return null;
    }

    abstract String getLine();

    abstract long getVehicleNumber();

    abstract long getFare();

    abstract long getArvo();

    abstract long getExpireTimestamp(); // -1

    abstract long getPax();

    abstract long getNewBalance();

    @NonNull
    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {

        abstract Builder timestamp(long timestamp);

        abstract Builder line(String line);

        abstract Builder vehicleNumber(long vehicleNumber);

        abstract Builder fare(long fare);

        abstract Builder arvo(long arvo);

        abstract Builder expireTimestamp(long expireTimestamp);

        abstract Builder pax(long pax);

        abstract Builder newBalance(long newBalance);

        abstract HSLTrip build();
    }

}
