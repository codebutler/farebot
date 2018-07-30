/*
 * KMTTrip.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014-2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.kmt;

import android.content.res.Resources;
import android.support.annotation.NonNull;

import com.codebutler.farebot.card.felica.FelicaBlock;
import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;
import com.google.auto.value.AutoValue;

import net.kazzz.felica.lib.Util;

import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

@AutoValue
abstract class KMTTrip extends Trip {

    @NonNull
    static KMTTrip create(FelicaBlock block) {
        byte[] data = block.getData().bytes();
        int processType = data[0];
        int sequenceNumber = Util.toInt(data[1], data[2], data[3]);
        Date timestampData = KMTUtil.extractDate(data);
        int transactionAmount = Util.toInt(data[8], data[9], data[10], data[11]);
        int balance = Util.toInt(data[12], data[13], data[14], data[15]);
        return new AutoValue_KMTTrip(processType, sequenceNumber, timestampData, transactionAmount, balance);
    }

    @Override
    public Mode getMode() {
        switch (getProcessType()) {
            default:
                return Mode.OTHER;
        }
    }

    @Override
    public long getTimestamp() {
        if (getTimestampData() != null) {
            return getTimestampData().getTime() / 1000;
        } else {
            return 0;
        }
    }

    @Override
    public boolean hasFare() {
        return true;
    }

    @Override
    public String getFareString(@NonNull Resources resources) {
        Locale localeID = new Locale("in", "ID");
        NumberFormat format = NumberFormat.getCurrencyInstance(localeID);
        return format.format(getTransactionAmount());
    }

    @Override
    public String getBalanceString() {
        Locale localeID = new Locale("in", "ID");
        NumberFormat format = NumberFormat.getCurrencyInstance(localeID);
        format.setMaximumFractionDigits(0);
        return format.format(getBalance());
    }

    // use agency name for the transaction number
    @Override
    public String getShortAgencyName(@NonNull Resources resources) {
        return getAgencyName(resources);
    }

    @Override
    public String getAgencyName(@NonNull Resources resources) {
        return "-";
    }

    @Override
    public boolean hasTime() {
        return getTimestampData() != null;
    }

    @Override
    public String getRouteName(@NonNull Resources resources) {
        return null;
    }

    @Override
    public String getStartStationName(@NonNull Resources resources) {
        return null;
    }

    @Override
    public Station getStartStation() {
        return null;
    }

    @Override
    public String getEndStationName(@NonNull Resources resources) {
        return null;
    }

    @Override
    public Station getEndStation() {
        return null;
    }

    @Override
    public long getExitTimestamp() {
        return 0;
    }

    abstract int getProcessType();

    abstract int getSequenceNumber();

    abstract Date getTimestampData();

    abstract int getTransactionAmount();

    abstract int getBalance();

}
