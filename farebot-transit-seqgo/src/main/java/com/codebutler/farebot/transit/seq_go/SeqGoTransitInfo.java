/*
 * SeqGoTransitInfo.java
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

package com.codebutler.farebot.transit.seq_go;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitInfo;
import com.codebutler.farebot.transit.Trip;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Transit data type for Go card (Brisbane / South-East Queensland, AU), used by Translink.
 * <p>
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/Go-%28SEQ%29
 *
 * @author Michael Farrell
 */
@AutoValue
public abstract class SeqGoTransitInfo extends TransitInfo {

    public static final String NAME = "Go card";

    @NonNull
    static SeqGoTransitInfo create(
            @NonNull String serialNumber,
            @NonNull ImmutableList<Trip> trips,
            @NonNull ImmutableList<Refill> refills,
            boolean hasUnknownStations,
            int balance) {
        return new AutoValue_SeqGoTransitInfo(serialNumber, trips, refills, hasUnknownStations, balance);
    }

    @NonNull
    @Override
    public String getBalanceString(@NonNull Resources resources) {
        return NumberFormat.getCurrencyInstance(Locale.US).format((double) getBalance() / 100.);
    }

    @NonNull
    @Override
    public String getCardName(@NonNull Resources resources) {
        return NAME;
    }

    @Nullable
    @Override
    public List<Subscription> getSubscriptions() {
        return null;
    }

    @Override
    public abstract boolean hasUnknownStations();

    abstract int getBalance();
}
