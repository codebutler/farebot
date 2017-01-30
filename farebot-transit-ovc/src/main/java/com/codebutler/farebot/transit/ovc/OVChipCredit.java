/*
 * OVChipCredit.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2012, 2014-2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.ovc;

import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.codebutler.farebot.core.ByteUtils;
import com.google.auto.value.AutoValue;

@AutoValue
abstract class OVChipCredit implements Parcelable {

    @NonNull
    static OVChipCredit create(byte[] data) {
        if (data == null) {
            data = new byte[16];
        }

        final int banbits = ByteUtils.getBitsFromBuffer(data, 0, 9);
        final int id = ByteUtils.getBitsFromBuffer(data, 9, 12);
        final int creditId = ByteUtils.getBitsFromBuffer(data, 56, 12);
        int credit = ByteUtils.getBitsFromBuffer(data, 78, 15);    // Skipping the first bit (77)...

        if ((data[9] & (byte) 0x04) != 4) {    // ...as the first bit is used to see if the credit is negative or not
            credit ^= (char) 0x7FFF;
            credit = credit * -1;
        }

        return new AutoValue_OVChipCredit(id, creditId, credit, banbits);
    }

    public abstract int getId();

    public abstract int getCreditId();

    public abstract int getCredit();

    public abstract int getBanbits();

}
