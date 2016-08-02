/*
 * CEPASTransaction.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014-2015 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2011 Sean Cross <sean@chumby.com>
 * Copyright (C) 2012 tbonang <bonang@gmail.com>
 * Copyright (C) 2012 Victor Heng <bakavic@gmail.com>
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

package com.codebutler.farebot.card.cepas;

import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class CEPASTransaction implements Parcelable {

    public enum TransactionType {
        MRT,
        // Old MRT transit info is unhyphenated - renamed from OLD_MRT to TOP_UP,
        // as it seems like the code has been repurposed.
        TOP_UP,
        BUS,
        BUS_REFUND,
        CREATION,
        RETAIL,
        SERVICE,
        UNKNOWN;
    }

    @NonNull
    public static CEPASTransaction create(byte rawType, int amount, int timestamp, String userData) {
        return new AutoValue_CEPASTransaction(rawType, amount, timestamp, userData);
    }

    @NonNull
    public static CEPASTransaction create(@NonNull byte[] rawData) {
        int tmp;

        int type = rawData[0];

        tmp = (0x00ff0000 & ((rawData[1])) << 16) | (0x0000ff00 & (rawData[2] << 8)) | (0x000000ff & (rawData[3]));
        /* Sign-extend the value */
        if (0 != (rawData[1] & 0x80)) {
            tmp |= 0xff000000;
        }
        int amount = tmp;

        /* Date is expressed "in seconds", but the epoch is January 1 1995, SGT */
        int date = ((0xff000000 & (rawData[4] << 24))
                | (0x00ff0000 & (rawData[5] << 16))
                | (0x0000ff00 & (rawData[6] << 8))
                | (0x000000ff & (rawData[7] << 0)))
                + 788947200 - (16 * 3600);

        byte[] userData = new byte[9];
        System.arraycopy(rawData, 8, userData, 0, 8);
        userData[8] = '\0';
        String userDataString = new String(userData);

        return new AutoValue_CEPASTransaction(type, amount, date, userDataString);
    }

    @NonNull
    public TransactionType getType() {
        switch (getRawType()) {
            case 48:
                return TransactionType.MRT;
            case 117:
            case 3:
                return TransactionType.TOP_UP;
            case 49:
                return TransactionType.BUS;
            case 118:
                return TransactionType.BUS_REFUND;
            case -16:
            case 5:
                return TransactionType.CREATION;
            case 4:
                return TransactionType.SERVICE;
            case 1:
                return TransactionType.RETAIL;
        }
        return TransactionType.UNKNOWN;
    }

    public abstract int getRawType();

    public abstract int getAmount();

    public abstract int getTimestamp();

    @NonNull
    public abstract String getUserData();
}
