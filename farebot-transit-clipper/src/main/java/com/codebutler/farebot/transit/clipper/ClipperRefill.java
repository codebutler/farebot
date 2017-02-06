/*
 * ClipperRefill.java
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

package com.codebutler.farebot.transit.clipper;

import android.content.res.Resources;
import android.support.annotation.NonNull;

import com.codebutler.farebot.transit.Refill;
import com.google.auto.value.AutoValue;

import java.text.NumberFormat;
import java.util.Locale;

@AutoValue
abstract class ClipperRefill extends Refill {

    @NonNull
    static ClipperRefill create(long timestamp, long amount, long agency, long machineid) {
        return new AutoValue_ClipperRefill(timestamp, amount, agency, machineid);
    }

    @Override
    public String getAmountString(@NonNull Resources resources) {
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance(Locale.US);
        return numberFormat.format((double) getAmount() / 100.0);
    }

    @Override
    public String getAgencyName(@NonNull Resources resources) {
        return ClipperTransitInfo.getAgencyName((int) getAgency());
    }

    @Override
    public String getShortAgencyName(@NonNull Resources resources) {
        return ClipperTransitInfo.getShortAgencyName((int) getAgency());
    }

    abstract long getAgency();

    abstract long getMachineID();
}
