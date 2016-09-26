/*
 * OrcaTransitData.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014-2016 Eric Butler <eric@codebutler.com>
 *
 * Thanks to:
 * Karl Koscher <supersat@cs.washington.edu>
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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.ui.ListItem;
import com.google.auto.value.AutoValue;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@AutoValue
public abstract class OrcaTransitData extends TransitData {

    static final int AGENCY_KCM = 0x04;
    static final int AGENCY_PT = 0x06;
    static final int AGENCY_ST = 0x07;
    static final int AGENCY_CT = 0x02;
    static final int AGENCY_WSF = 0x08;
    static final int AGENCY_ET = 0x03;

    @NonNull
    @Override
    public String getCardName() {
        return "ORCA";
    }

    @NonNull
    @Override
    public String getBalanceString() {
        return NumberFormat.getCurrencyInstance(Locale.US).format(getBalance() / 100);
    }

    @NonNull
    @Override
    public String getSerialNumber() {
        return Integer.toString(getSerialNumberData());
    }

    @Nullable
    @Override
    public List<Subscription> getSubscriptions() {
        return null;
    }

    @Nullable
    @Override
    public List<Refill> getRefills() {
        return null;
    }

    @Nullable
    @Override
    public List<ListItem> getInfo() {
        return null;
    }

    abstract int getSerialNumberData();

    abstract double getBalance();
}
