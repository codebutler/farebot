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
import com.codebutler.farebot.kmt.R;
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
        int processType = data[12];
        int sequenceNumber = Util.toInt(data[13], data[14], data[15]);
        Date timestampData = KMTUtil.extractDate(data);
        int transactionAmount = Util.toInt(data[4], data[5], data[6], data[7]);
        return new AutoValue_KMTTrip(processType, sequenceNumber, timestampData, transactionAmount);
    }

    @Override
    public Mode getMode() {
        switch (getProcessType()) {
            case 0:
                return Mode.POS;
            case 1:
                return Mode.TRAIN;
            case 2:
                return Mode.TICKET_MACHINE;
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
        return "-";
    }

    @Override
    public String getShortAgencyName(@NonNull Resources resources) {
        return getAgencyName(resources);
    }

    @Override
    public String getAgencyName(@NonNull Resources resources) {
        switch (getProcessType()) {
            case 1:
                return resources.getString(R.string.kmt_debit_desc);
            default:
                return resources.getString(R.string.kmt_credit_desc);
        }
    }

    @Override
    public boolean hasTime() {
        return getTimestampData() != null;
    }

    @Override
    public String getRouteName(@NonNull Resources resources) {
        return resources.getString(R.string.kmt_defroute);
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

}
