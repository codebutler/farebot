/*
 * ManlyFastFerryRefill.java
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

import android.content.res.Resources;
import androidx.annotation.NonNull;

import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.manly_fast_ferry.record.ManlyFastFerryPurseRecord;
import com.google.auto.value.AutoValue;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * Describes top-up amounts "purse credits".
 */
@AutoValue
abstract class ManlyFastFerryRefill extends Refill {

    @NonNull
    static ManlyFastFerryRefill create(ManlyFastFerryPurseRecord purse, GregorianCalendar epoch) {
        return new AutoValue_ManlyFastFerryRefill(purse, epoch);
    }

    @Override
    public long getTimestamp() {
        GregorianCalendar ts = new GregorianCalendar();
        ts.setTimeInMillis(getEpoch().getTimeInMillis());
        ts.add(Calendar.DATE, getPurse().getDay());
        ts.add(Calendar.MINUTE, getPurse().getMinute());

        return ts.getTimeInMillis() / 1000;
    }

    @Override
    public String getAgencyName(@NonNull Resources resources) {
        // There is only one agency on the card, don't show anything.
        return null;
    }

    @Override
    public String getShortAgencyName(@NonNull Resources resources) {
        // There is only one agency on the card, don't show anything.
        return null;
    }

    @Override
    public long getAmount() {
        return getPurse().getTransactionValue();
    }

    @Override
    public String getAmountString(@NonNull Resources resources) {
        return NumberFormat.getCurrencyInstance(Locale.US).format((double) getAmount() / 100);
    }

    abstract ManlyFastFerryPurseRecord getPurse();

    abstract GregorianCalendar getEpoch();
}
