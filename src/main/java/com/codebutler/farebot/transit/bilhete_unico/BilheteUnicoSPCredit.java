/*
 * BilheteUnicoSPCredit.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2013 Marcelo Liberato <mliberato@gmail.com>
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

package com.codebutler.farebot.transit.bilhete_unico;

import android.os.Parcel;
import android.os.Parcelable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class BilheteUnicoSPCredit implements Parcelable {

    public static final Parcelable.Creator<BilheteUnicoSPCredit> CREATOR
            = new Parcelable.Creator<BilheteUnicoSPCredit>() {
        @Override
        public BilheteUnicoSPCredit createFromParcel(Parcel source) {
            int credit = source.readInt();
            return new BilheteUnicoSPCredit(credit);
        }

        @Override
        public BilheteUnicoSPCredit[] newArray(int size) {
            return new BilheteUnicoSPCredit[size];
        }
    };

    private final int mCredit;

    private BilheteUnicoSPCredit(int credit) {
        mCredit = credit;
    }

    BilheteUnicoSPCredit(byte[] data) {
        if (data == null) {
            data = new byte[16];
        }

        mCredit = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getInt(0);
    }

    public int getCredit() {
        return mCredit;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mCredit);
    }
}
