/*
 * HSLRefill.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014-2015 Eric Butler <eric@codebutler.com>
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

import android.os.Parcel;

import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.R;
import com.codebutler.farebot.transit.Refill;

import java.text.NumberFormat;
import java.util.Locale;

public class HSLRefill extends Refill {
    private final long mRefillTime;
    private final long mRefillAmount;

    public HSLRefill(byte[] data) {
        mRefillTime = HSLTransitData.cardDateToTimestamp(HSLTransitData.bitsToLong(20, 14, data), HSLTransitData.bitsToLong(34, 11, data));
        mRefillAmount = HSLTransitData.bitsToLong(45, 20, data);
    }

    public HSLRefill(Parcel parcel) {
        mRefillTime = parcel.readLong();
        mRefillAmount = parcel.readLong();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mRefillTime);
        dest.writeLong(mRefillAmount);
    }

    @Override public long getTimestamp() {
        return mRefillTime;
    }

    @Override public String getAgencyName() {
        return FareBotApplication.getInstance().getString(R.string.hsl_balance_refill);
    }

    @Override public String getShortAgencyName() {
        return FareBotApplication.getInstance().getString(R.string.hsl_balance_refill);
    }

    @Override public long getAmount() {
        return mRefillAmount;
    }

    @Override public String getAmountString() {
        return NumberFormat.getCurrencyInstance(Locale.GERMANY).format(mRefillAmount / 100.0);
    }
}
