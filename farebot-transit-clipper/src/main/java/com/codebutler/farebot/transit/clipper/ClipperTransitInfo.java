/*
 * ClipperTransitInfo.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014-2016 Eric Butler <eric@codebutler.com>
 *
 * Thanks to:
 * An anonymous contributor for reverse engineering Clipper data and providing
 * most of the code here.
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
import android.support.annotation.Nullable;

import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitInfo;
import com.codebutler.farebot.transit.Trip;
import com.google.auto.value.AutoValue;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@AutoValue
public abstract class ClipperTransitInfo extends TransitInfo {

    @NonNull
    static ClipperTransitInfo create(
            @NonNull String serialNumber,
            @NonNull List<Trip> trips,
            @NonNull List<Refill> refills,
            short balance) {
        return new AutoValue_ClipperTransitInfo(serialNumber, trips, refills, balance);
    }

    @NonNull
    @Override
    public String getCardName(@NonNull Resources resources) {
        return resources.getString(R.string.transit_clipper_card_name);
    }

    @NonNull
    @Override
    public String getBalanceString(@NonNull Resources resources) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(getBalance() / 100.0);
    }

    @Nullable
    @Override
    public List<Subscription> getSubscriptions() {
        return null;
    }

    abstract short getBalance();
}
