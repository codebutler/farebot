/*
 * OVChipPreamble.java
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

import com.codebutler.farebot.util.Utils;
import com.google.auto.value.AutoValue;

@AutoValue
abstract class OVChipPreamble implements Parcelable {

    @NonNull
    static OVChipPreamble create(byte[] data) {
        if (data == null) {
            data = new byte[48];
        }

        String hex = Utils.getHexString(data, null);

        String id = hex.substring(0, 8);
        int checkbit = Utils.getBitsFromBuffer(data, 32, 8);
        String manufacturer = hex.substring(10, 20);
        String publisher = hex.substring(20, 32);
        String unknownConstant1 = hex.substring(32, 54);
        int expdate = Utils.getBitsFromBuffer(data, 216, 20);
        String unknownConstant2 = hex.substring(59, 68);
        int type = Utils.getBitsFromBuffer(data, 276, 4);

        return new AutoValue_OVChipPreamble(
                id,
                checkbit,
                manufacturer,
                publisher,
                unknownConstant1,
                expdate,
                unknownConstant2,
                type);
    }

    public abstract String getId();

    public abstract int getCheckbit();

    public abstract String getManufacturer();

    public abstract String getPublisher();

    public abstract String getUnknownConstant1();

    public abstract int getExpdate();

    public abstract String getUnknownConstant2();

    public abstract int getType();
}
