/*
 * ManlyFastFerryTrip.java
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

package com.codebutler.farebot.transit.manly_fast_ferry;

import android.support.annotation.NonNull;

import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.transit.manly_fast_ferry.record.ManlyFastFerryPurseRecord;
import com.google.auto.value.AutoValue;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * Trips on the card are "purse debits", and it is not possible to tell it apart from non-ticket
 * usage (like cafe purchases).
 */
@AutoValue
abstract class ManlyFastFerryTrip extends Trip {

    @NonNull
    static ManlyFastFerryTrip create(ManlyFastFerryPurseRecord purse, GregorianCalendar epoch) {
        return new AutoValue_ManlyFastFerryTrip(purse, epoch);
    }

    // Implemented functionality.
    @Override
    public long getTimestamp() {
        GregorianCalendar ts = new GregorianCalendar();
        ts.setTimeInMillis(getEpoch().getTimeInMillis());
        ts.add(Calendar.DATE, getPurse().getDay());
        ts.add(Calendar.MINUTE, getPurse().getMinute());

        return ts.getTimeInMillis() / 1000;
    }

    @Override
    public long getExitTimestamp() {
        // This never gets used, except by Clipper, so stub.
        return 0;
    }

    @Override
    public String getRouteName() {
        return null;
    }

    @Override
    public String getAgencyName() {
        // There is only one agency on the card, don't show anything.
        return null;
    }

    @Override
    public String getShortAgencyName() {
        // There is only one agency on the card, don't show anything.
        return null;
    }

    @Override
    public String getFareString() {
        return NumberFormat.getCurrencyInstance(Locale.US).format((double) getPurse().getTransactionValue() / 100.);
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
    public Mode getMode() {
        // All transactions look the same... but this is a ferry, so we'll call it a ferry one.
        // Even when you buy things at the cafe.
        return Mode.FERRY;
    }

    @Override
    public boolean hasTime() {
        return true;
    }

    abstract ManlyFastFerryPurseRecord getPurse();

    abstract GregorianCalendar getEpoch();
}
