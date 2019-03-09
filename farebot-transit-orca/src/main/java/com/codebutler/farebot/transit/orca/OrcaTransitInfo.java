/*
 * OrcaTransitInfo.java
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

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitInfo;
import com.google.auto.value.AutoValue;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@AutoValue
public abstract class OrcaTransitInfo extends TransitInfo {

    static final int AGENCY_KCM = 0x04;
    static final int AGENCY_PT = 0x06;
    static final int AGENCY_ST = 0x07;
    static final int AGENCY_CT = 0x02;
    static final int AGENCY_WSF = 0x08;
    static final int AGENCY_ET = 0x03;
    static final int AGENCY_KT = 0x05;

    static final int FTP_TYPE_FERRY = 0x08;
    static final int FTP_TYPE_SOUNDER = 0x09;
    static final int FTP_TYPE_CUSTOMER_SERVICE = 0x0B;
    static final int FTP_TYPE_BUS = 0x80;
    static final int FTP_TYPE_LINK = 0xFB;
    static final int FTP_TYPE_WATER_TAXI = 0xFE;
    static final int FTP_TYPE_STREETCAR = 0xF9;
    static final int FTP_TYPE_BRT = 0xFA; //May also apply to future hardwired bus readers

    @NonNull
    @Override
    public String getCardName(@NonNull Resources resources) {
        return "ORCA";
    }

    @NonNull
    @Override
    public String getBalanceString(@NonNull Resources resources) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(getBalance() / 100);
    }

    @Nullable
    @Override
    public String getSerialNumber() {
        return Integer.toString(getSerialNumberData());
    }

    @Nullable
    @Override
    public List<Subscription> getSubscriptions() {
        return null;
    }

    @Override
    public boolean hasUnknownStations() { return true; }

    abstract int getSerialNumberData();

    abstract double getBalance();
}
