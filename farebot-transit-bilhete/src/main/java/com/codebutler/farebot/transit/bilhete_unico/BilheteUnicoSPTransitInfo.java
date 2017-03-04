/*
 * BilheteUnicoSPTransitInfo.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2013-2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2013 Marcelo Liberato <mliberato@gmail.com>
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

package com.codebutler.farebot.transit.bilhete_unico;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitInfo;
import com.codebutler.farebot.transit.Trip;
import com.google.auto.value.AutoValue;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.List;

@AutoValue
public abstract class BilheteUnicoSPTransitInfo extends TransitInfo {

    static final String NAME = "Bilhete Único";

    @NonNull
    static BilheteUnicoSPTransitInfo create(@NonNull BilheteUnicoSPCredit credit) {
        return new AutoValue_BilheteUnicoSPTransitInfo(credit);
    }

    @NonNull
    @Override
    public String getCardName(@NonNull Resources resources) {
        return NAME;
    }

    @NonNull
    @Override
    public String getBalanceString(@NonNull Resources resources) {
        return BilheteUnicoSPTransitInfo.convertAmount(getCredit().getCredit());
    }

    @Nullable
    @Override
    public String getSerialNumber() {
        return null;
    }

    @Nullable
    @Override
    public List<Trip> getTrips() {
        return null;
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

    @NonNull
    abstract BilheteUnicoSPCredit getCredit();

    private static String convertAmount(int amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance();
        formatter.setCurrency(Currency.getInstance("BRL"));

        return formatter.format((double) amount / 100.0);
    }
}
