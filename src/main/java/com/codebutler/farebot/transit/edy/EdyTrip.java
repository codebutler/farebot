/*
 * EdyTrip.java
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

package com.codebutler.farebot.transit.edy;

import android.app.Application;
import android.support.annotation.NonNull;

import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.R;
import com.codebutler.farebot.card.felica.FelicaBlock;
import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;
import com.google.auto.value.AutoValue;

import net.kazzz.felica.lib.Util;

import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

@AutoValue
abstract class EdyTrip extends Trip {

    @NonNull
    static EdyTrip create(FelicaBlock block) {
        byte[] data = block.getData().bytes();

        // Data Offsets with values
        // ------------------------
        // 0x00    type (0x20 = payment, 0x02 = charge, 0x04 = gift)
        // 0x01    sequence number (3 bytes, big-endian)
        // 0x04    date/time (upper 15 bits - added as day offset,
        //         lower 17 bits - added as second offset to Jan 1, 2000 00:00:00)
        // 0x08    transaction amount (big-endian)
        // 0x0c    balance (big-endian)

        int processType = data[0];
        int sequenceNumber = Util.toInt(data[1], data[2], data[3]);
        Date timestampData = EdyUtil.extractDate(data);
        int transactionAmount = Util.toInt(data[8], data[9], data[10], data[11]);
        int balance = Util.toInt(data[12], data[13], data[14], data[15]);

        return new AutoValue_EdyTrip(processType, sequenceNumber, timestampData, transactionAmount, balance);
    }

    @Override
    public Mode getMode() {
        switch (getProcessType()) {
            case EdyTransitData.FELICA_MODE_EDY_DEBIT:
                return Mode.POS;
            case EdyTransitData.FELICA_MODE_EDY_CHARGE:
                return Mode.TICKET_MACHINE;
            case EdyTransitData.FELICA_MODE_EDY_GIFT:
                return Mode.VENDING_MACHINE;
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
    public String getFareString() {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.JAPAN);
        format.setMaximumFractionDigits(0);
        if (getProcessType() != EdyTransitData.FELICA_MODE_EDY_DEBIT) {
            return "+" + format.format(getTransactionAmount());
        }
        return format.format(getTransactionAmount());
    }

    @Override
    public String getBalanceString() {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.JAPAN);
        format.setMaximumFractionDigits(0);
        return format.format(getBalance());
    }

    // use agency name for the transaction number
    @Override
    public String getShortAgencyName() {
        return getAgencyName();
    }

    @Override
    public String getAgencyName() {
        NumberFormat format = NumberFormat.getIntegerInstance();
        format.setMinimumIntegerDigits(8);
        format.setGroupingUsed(false);
        Application app = FareBotApplication.getInstance();
        String str;
        if (getProcessType() != EdyTransitData.FELICA_MODE_EDY_DEBIT) {
            str = app.getString(R.string.felica_process_charge);
        } else {
            str = app.getString(R.string.felica_process_merchandise_purchase);
        }
        str += " " + app.getString(R.string.transaction_sequence) + format.format(getSequenceNumber());
        return str;
    }

    @Override
    public boolean hasTime() {
        return getTimestampData() != null;
    }

    // unused
    @Override
    public String getRouteName() {
        return null;
    }

    @Override
    public String getStartStationName() {
        return null;
    }

    @Override
    public Station getStartStation() {
        return null;
    }

    @Override
    public String getEndStationName() {
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
