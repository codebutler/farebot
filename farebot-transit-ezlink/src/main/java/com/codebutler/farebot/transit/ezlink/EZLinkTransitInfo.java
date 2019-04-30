/*
 * EZLinkTransitInfo.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014-2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2011 Sean Cross <sean@chumby.com>
 * Copyright (C) 2012 tbonang <bonang@gmail.com>
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

package com.codebutler.farebot.transit.ezlink;

import android.content.res.Resources;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitInfo;
import com.codebutler.farebot.transit.Trip;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.List;

@AutoValue
public abstract class EZLinkTransitInfo extends TransitInfo {

    @NonNull
    static EZLinkTransitInfo create(@NonNull String serialNumber, @NonNull ImmutableList<Trip> trips, int balance) {
        return new AutoValue_EZLinkTransitInfo(serialNumber, trips, balance);
    }

    @NonNull
    @Override
    public String getCardName(@NonNull Resources resources) {
        return EZLinkData.getCardIssuer(getSerialNumber());
    }

    @NonNull
    @Override
    public String getBalanceString(@NonNull Resources resources) {
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance();
        numberFormat.setCurrency(Currency.getInstance("SGD"));
        return numberFormat.format(getBalance() / 100);
    }

    @Nullable
    @Override
    public List<Refill> getRefills() {
        return null;
    }

    @Nullable
    @Override
    public List<Subscription> getSubscriptions() {
        return null;
    }

    abstract double getBalance();
}
