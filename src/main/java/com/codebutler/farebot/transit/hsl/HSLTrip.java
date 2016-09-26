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

import android.support.annotation.NonNull;

import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.R;
import com.codebutler.farebot.card.desfire.DesfireRecord;
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

    @NonNull
    static Builder builder(@NonNull HSLTrip trip) {
        return new AutoValue_HSLTrip.Builder(trip);
    }

    @NonNull
    static HSLTrip create(@NonNull DesfireRecord record) {
        byte[] useData = record.getData().bytes();
        long[] usefulData = new long[useData.length];

        for (int i = 0; i < useData.length; i++) {
            usefulData[i] = ((long) useData[i]) & 0xFF;
        }

        long arvo = HSLTransitData.bitsToLong(0, 1, usefulData);
        long timestamp = HSLTransitData.cardDateToTimestamp(HSLTransitData.bitsToLong(1, 14, usefulData),
                HSLTransitData.bitsToLong(15, 11, usefulData));
        long expireTimestamp = HSLTransitData.cardDateToTimestamp(HSLTransitData.bitsToLong(26, 14, usefulData),
                HSLTransitData.bitsToLong(40, 11, usefulData));
        long fare = HSLTransitData.bitsToLong(51, 14, usefulData);
        long pax = HSLTransitData.bitsToLong(65, 5, usefulData);
        String line = null;
        long vehicleNumber = -1;
        long newBalance = HSLTransitData.bitsToLong(70, 20, usefulData);

        return new AutoValue_HSLTrip(timestamp, line, vehicleNumber, fare, arvo, expireTimestamp, pax, newBalance);
    }

    @Override
    public long getExitTimestamp() {
        return 0;
    }

    @Override
    public String getAgencyName() {
        FareBotApplication app = FareBotApplication.getInstance();
        String pax = app.getString(R.string.hsl_person_format, String.valueOf(getPax()));
        if (getArvo() == 1) {
            String mins = app.getString(R.string.hsl_mins_format,
                    String.valueOf((getExpireTimestamp() - getTimestamp()) / 60));
            String type = app.getString(R.string.hsl_balance_ticket);
            return String.format("%s, %s, %s", type, pax, mins);
        } else {
            String type = app.getString(R.string.hsl_pass_ticket);
            return String.format("%s, %s", type, pax);
        }
    }

    @Override
    public String getShortAgencyName() {
        return getAgencyName();
    }

    @Override
    public String getRouteName() {
        if (getLine() != null) {
            // FIXME: i18n
            return String.format("Line %s, Vehicle %s", getLine().substring(1), getVehicleNumber());
        }
        return null;
    }

    @Override
    public String getFareString() {
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
    public String getEndStationName() {
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
    public String getStartStationName() {
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
