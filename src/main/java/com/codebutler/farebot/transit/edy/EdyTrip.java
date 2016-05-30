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
import android.os.Parcel;

import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.R;
import com.codebutler.farebot.card.felica.FelicaBlock;
import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;

import net.kazzz.felica.lib.Util;

import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

public class EdyTrip extends Trip {
    private final int mProcessType;
    private final int mSequenceNumber;
    private final Date mTimestamp;
    private final int mTransactionAmount;
    private final int mBalance;

    public EdyTrip(FelicaBlock block) {
        byte[] data = block.getData();

        // Data Offsets with values
        // ------------------------
        // 0x00    type (0x20 = payment, 0x02 = charge, 0x04 = gift)
        // 0x01    sequence number (3 bytes, big-endian)
        // 0x04    date/time (upper 15 bits - added as day offset, lower 17 bits - added as second offset to Jan 1, 2000 00:00:00)
        // 0x08    transaction amount (big-endian)
        // 0x0c    balance (big-endian)

        mProcessType = data[0];
        mSequenceNumber = Util.toInt(data[1], data[2], data[3]);
        mTimestamp = EdyUtil.extractDate(data);
        mTransactionAmount = Util.toInt(data[8], data[9], data[10], data[11]);
        mBalance = Util.toInt(data[12], data[13], data[14], data[15]);
    }

    public static final Creator<EdyTrip> CREATOR = new Creator<EdyTrip>() {
        public EdyTrip createFromParcel(Parcel parcel) {
            return new EdyTrip(parcel);
        }

        public EdyTrip[] newArray(int size) {
            return new EdyTrip[size];
        }
    };

    public EdyTrip(Parcel parcel) {
        mProcessType = parcel.readInt();
        mSequenceNumber = parcel.readInt();
        mTimestamp  = new Date(parcel.readLong());
        mTransactionAmount = parcel.readInt();
        mBalance = parcel.readInt();
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mProcessType);
        parcel.writeInt(mSequenceNumber);
        parcel.writeLong(mTimestamp.getTime());
        parcel.writeInt(mTransactionAmount);
        parcel.writeInt(mBalance);
    }

    public Mode getMode() {
        if (mProcessType == EdyTransitData.FELICA_MODE_EDY_DEBIT) {
            return Mode.POS;
        } else if (mProcessType == EdyTransitData.FELICA_MODE_EDY_CHARGE) {
            return Mode.TICKET_MACHINE;
        } else if (mProcessType == EdyTransitData.FELICA_MODE_EDY_GIFT) {
            return Mode.VENDING_MACHINE;
        } else {
            return Mode.OTHER;
        }
    }

    public long getTimestamp() {
        if (mTimestamp != null)
            return mTimestamp.getTime() / 1000;
        else
            return 0;
    }

    public boolean hasFare() {
        return true;
    }

    public String getFareString() {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.JAPAN);
        format.setMaximumFractionDigits(0);
        if (mProcessType != EdyTransitData.FELICA_MODE_EDY_DEBIT)
            return "+" + format.format(mTransactionAmount);
        return format.format(mTransactionAmount);
    }

    public String getBalanceString() {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.JAPAN);
        format.setMaximumFractionDigits(0);
        return format.format(mBalance);
    }

    // use agency name for the tranaction number
    public String getShortAgencyName() {
        return getAgencyName();
    }
    public String getAgencyName() {
        NumberFormat format = NumberFormat.getIntegerInstance();
        format.setMinimumIntegerDigits(8);
        format.setGroupingUsed(false);
        Application app = FareBotApplication.getInstance();
        String str;
        if (mProcessType != EdyTransitData.FELICA_MODE_EDY_DEBIT)
            str = app.getString(R.string.felica_process_charge);
        else
            str = app.getString(R.string.felica_process_merchandise_purchase);
        str += " " + app.getString(R.string.transaction_sequence) + format.format(mSequenceNumber);
        return str;
    }

    public boolean hasTime() {
        return mTimestamp != null;
    }

    // unused
    public String getRouteName() {
        return null;
    }
    public String getStartStationName() {
        return null;
    }
    public Station getStartStation() {
        return null;
    }
    public String getEndStationName() {
        return null;
    }
    public Station getEndStation() {
        return null;
    }
    public int describeContents() {
        return 0;
    }
    public long getExitTimestamp() {
        return 0;
    }
}
